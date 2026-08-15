package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import will.dev.smart_invite_v3.dto.payment.request.InitPaymentRequest;
import will.dev.smart_invite_v3.dto.payment.request.ReviewPaymentRequest;
import will.dev.smart_invite_v3.dto.payment.response.PaymentPlanResponse;
import will.dev.smart_invite_v3.dto.payment.response.PaymentResponse;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.Payment;
import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.enums.PaymentStatus;
import will.dev.smart_invite_v3.exception.EventNotFoundException;
import will.dev.smart_invite_v3.exception.PaymentException;
import will.dev.smart_invite_v3.exception.UserNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.PaymentRepository;
import will.dev.smart_invite_v3.repository.UserRepository;
import will.dev.smart_invite_v3.service.EmailService;
import will.dev.smart_invite_v3.service.FirebaseStorageService;
import will.dev.smart_invite_v3.service.PaymentService;
import will.dev.smart_invite_v3.service.WhatsAppService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    @Value("${spring.profiles.active}")
    private String path;

    private static final BigDecimal UNIT_PRICE = BigDecimal.valueOf(52);
    private static final Set<String> ALLOWED_TYPES =
            Set.of("application/pdf", "image/png", "image/jpeg", "image/jpg");

    private final PaymentRepository    paymentRepository;
    private final EventRepository      eventRepository;
    private final UserRepository       userRepository;
    private final FirebaseStorageService firebaseStorage;
    private final EmailService         emailService;
    private final WhatsAppService      whatsAppService;
    private final NotificationDispatcher notificationDispatcher;

    @Override
    public PaymentPlanResponse calculatePlan(int quota) {
        return PaymentPlanResponse.calculate(quota);
    }

    @Override
    @Transactional
    public PaymentResponse initPayment(InitPaymentRequest request, Long organizerId) {
        Event event = eventRepository.findById(request.eventId())
                .orElseThrow(() -> new EventNotFoundException(request.eventId()));

        if (!event.getOrganizer().getId().equals(organizerId)) {
            throw new PaymentException("Cet événement ne vous appartient pas");
        }

        // Vérifier si un paiement PENDING ou UNDER_REVIEW existe déjà
        paymentRepository.findTopByEventIdAndOrganizerIdOrderByCreatedAtDesc(
                request.eventId(), organizerId
        ).ifPresent(existing -> {
            if (existing.getStatus() == PaymentStatus.PENDING) {
                throw new PaymentException(
                        "Un paiement en attente existe déjà pour cet événement (id: " + existing.getId() + ")");
            }
            if (existing.getStatus() == PaymentStatus.UNDER_REVIEW) {
                throw new PaymentException(
                        "Une preuve de paiement est déjà en cours de vérification (id: " + existing.getId() + ")");
            }
        });

        User organizer = userRepository.findById(organizerId)
                .orElseThrow(() -> new UserNotFoundException("Utilisateur introuvable"));

        BigDecimal amount = UNIT_PRICE.multiply(BigDecimal.valueOf(request.quota()));

        Payment payment = Payment.builder()
                .event(event)
                .organizer(organizer)
                .quota(request.quota())
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .build();

        return PaymentResponse.from(paymentRepository.save(payment));
    }

    @Override
    @Transactional
    public PaymentResponse submitProof(Long paymentId, MultipartFile file, Long organizerId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Paiement introuvable : " + paymentId));

        if (!payment.getOrganizer().getId().equals(organizerId)) {
            throw new PaymentException("Ce paiement ne vous appartient pas");
        }

        if (payment.getStatus() == PaymentStatus.APPROVED) {
            throw new PaymentException("Ce paiement est déjà approuvé");
        }

        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new PaymentException("Format non supporté. Utilisez PDF, PNG ou JPG");
        }

        // Upload sur Firebase Storage dans le dossier "dev ou prod" selon l'environnement
        String proofUrl = firebaseStorage.upload(file, path + "/payment");

        payment.setProofUrl(proofUrl);
        payment.setStatus(PaymentStatus.UNDER_REVIEW);

        Payment saved = paymentRepository.save(payment);

        // Notifier l'admin par email (hors transaction pour ne pas bloquer)
        try {
            System.out.println("----Initialisation d'envoie de la preuve---");
            emailService.sendPaymentProofNotification(
                    payment.getOrganizer().getName(),
                    payment.getEvent().getTitle(),
                    payment.getQuota(),
                    payment.getAmount(),
                    proofUrl
            );
        } catch (Exception e) {
            log.warn("Notification admin échouée pour paiement {} : {}", paymentId, e.getMessage());
        }

        return PaymentResponse.from(saved);
    }

    @Override
    public List<PaymentResponse> getHistory(Long organizerId) {
        return paymentRepository
                .findAllByOrganizerIdOrderByCreatedAtDesc(organizerId)
                .stream()
                .map(PaymentResponse::from)
                .toList();
    }

    @Override
    public List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(PaymentResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public PaymentResponse reviewPayment(Long paymentId, ReviewPaymentRequest request) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Paiement introuvable : " + paymentId));

        if (payment.getStatus() != PaymentStatus.UNDER_REVIEW) {
            throw new PaymentException("Seuls les paiements UNDER_REVIEW peuvent être examinés");
        }

        if (request.approved()) {
            payment.setStatus(PaymentStatus.APPROVED);
            payment.setPaidQuota(payment.getQuota());
            // Activer le quota sur l'événement
            Event event = payment.getEvent();
            int current = event.getMaxGuests() != null ? event.getMaxGuests() : 0;
            event.setMaxGuests(current + payment.getQuota());
            eventRepository.save(event);
        } else {
            if (request.rejectionReason() == null || request.rejectionReason().isBlank()) {
                throw new PaymentException("Un motif de rejet est obligatoire");
            }
            payment.setStatus(PaymentStatus.REJECTED);
            payment.setRejectionReason(request.rejectionReason());
        }

        Payment saved = paymentRepository.save(payment);

        try {
            notificationDispatcher.sendOrganizerNotification(
                    payment.getOrganizer(),
                    () -> emailService.sendPaymentReviewNotification(
                            payment.getOrganizer().getEmail(),
                            payment.getOrganizer().getName(),
                            payment.getEvent().getTitle(),
                            request.approved(),
                            request.rejectionReason()),
                    () -> whatsAppService.sendOrganizerTextMessage(
                            payment.getOrganizer().getPhone(),
                            buildPaymentReviewMessage(payment.getEvent().getTitle(),
                                    request.approved(), request.rejectionReason()))
            );
        } catch (Exception e) {
            log.warn("Notification organisateur échouée pour paiement {} : {}", paymentId, e.getMessage());
        }

        return PaymentResponse.from(saved);
    }

    private String buildPaymentReviewMessage(String eventTitle, boolean approved, String rejectionReason) {
        String status = approved ? "approuvé ✅" : "rejeté ❌";
        String msg = "💳 *Résultat de votre paiement*\n\n" +
                "Votre paiement pour *" + eventTitle + "* a été *" + status + "*. ";
        if (!approved && rejectionReason != null && !rejectionReason.isBlank()) {
            msg += "\nMotif : _" + rejectionReason + "_";
        }
        return msg;
    }
}