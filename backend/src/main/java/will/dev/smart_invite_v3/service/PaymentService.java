package will.dev.smart_invite_v3.service;

import org.springframework.web.multipart.MultipartFile;
import will.dev.smart_invite_v3.dto.payment.request.InitPaymentRequest;
import will.dev.smart_invite_v3.dto.payment.request.ReviewPaymentRequest;
import will.dev.smart_invite_v3.dto.payment.response.PaymentPlanResponse;
import will.dev.smart_invite_v3.dto.payment.response.PaymentResponse;

import java.util.List;

public interface PaymentService {

    /** US-029 — Calcul du montant selon quota */
    PaymentPlanResponse calculatePlan(int quota);

    /** US-030 — Initialisation d'un paiement */
    PaymentResponse initPayment(InitPaymentRequest request, Long organizerId);

    /** US-031 — Soumission d'une preuve de paiement */
    PaymentResponse submitProof(Long paymentId, MultipartFile file, Long organizerId);

    /** Historique des paiements de l'utilisateur */
    List<PaymentResponse> getHistory(Long organizerId);

    /** Admin — valider ou rejeter une preuve de paiement */
    PaymentResponse reviewPayment(Long paymentId, ReviewPaymentRequest request);
}
