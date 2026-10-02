package will.dev.smart_invite_v3.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.Payment;
import will.dev.smart_invite_v3.entity.Referrer;
import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.exception.ReferralCodeException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.GuestRepository;
import will.dev.smart_invite_v3.repository.PaymentRepository;
import will.dev.smart_invite_v3.repository.UserNewsRepository;
import will.dev.smart_invite_v3.repository.UserRepository;
import will.dev.smart_invite_v3.service.EmailService;
import will.dev.smart_invite_v3.service.ReferrerService;
import will.dev.smart_invite_v3.service.WhatsAppService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private UserNewsRepository userNewsRepository;

    @Mock
    private WhatsAppService whatsAppService;

    @Mock
    private EmailService emailService;

    @Mock
    private ReferrerService referrerService;

    @InjectMocks
    private AdminServiceImpl adminService;

    private User user;
    private Referrer referrer;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(42L);
        user.setName("Test User");
        user.setEmail("user@example.com");

        referrer = Referrer.builder()
                .id(1L)
                .code("REF-ABC123")
                .name("Parrain VIP")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("assignReferralCode assigne le code et rétro-affecte aux événements et paiements sans code")
    void assignReferralCode_shouldAssignAndBackfill() {
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(referrerService.getActiveByCode("REF-ABC123")).thenReturn(referrer);

        Event event1 = new Event();
        event1.setId(101L);
        event1.setReferralCode(null);

        Event event2 = new Event();
        event2.setId(102L);
        event2.setReferralCode("EXISTING-CODE");

        when(eventRepository.findAllByOrganizerIdOrderByCreatedAtDesc(42L)).thenReturn(new ArrayList<>(List.of(event1, event2)));

        Payment payment1 = new Payment();
        payment1.setId(201L);
        payment1.setReferralCode(null);

        when(paymentRepository.findAllByOrganizerIdOrderByCreatedAtDesc(42L)).thenReturn(new ArrayList<>(List.of(payment1)));

        adminService.assignReferralCode(42L, " ref-abc123 ");

        assertThat(user.getReferralCode()).isEqualTo("REF-ABC123");
        assertThat(event1.getReferralCode()).isEqualTo("REF-ABC123");
        assertThat(event2.getReferralCode()).isEqualTo("EXISTING-CODE");
        assertThat(payment1.getReferralCode()).isEqualTo("REF-ABC123");

        verify(userRepository).save(user);
        verify(eventRepository).saveAll(any());
        verify(paymentRepository).saveAll(any());
    }

    @Test
    @DisplayName("assignReferralCode supprime le code si vide ou null")
    void assignReferralCode_shouldClearCode_whenBlank() {
        user.setReferralCode("OLD-CODE");
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));

        adminService.assignReferralCode(42L, "   ");

        assertThat(user.getReferralCode()).isNull();
        verify(userRepository).save(user);
        verifyNoInteractions(referrerService);
    }

    @Test
    @DisplayName("assignReferralCode propage l'erreur si le code est invalide ou inactif")
    void assignReferralCode_shouldThrow_whenCodeInvalid() {
        when(userRepository.findById(42L)).thenReturn(Optional.of(user));
        when(referrerService.getActiveByCode("REF-INVALID"))
                .thenThrow(new ReferralCodeException("Ce code de recommandation est invalide"));

        assertThatThrownBy(() -> adminService.assignReferralCode(42L, "REF-INVALID"))
                .isInstanceOf(ReferralCodeException.class)
                .hasMessageContaining("invalide");

        verify(userRepository, never()).save(any());
    }
}
