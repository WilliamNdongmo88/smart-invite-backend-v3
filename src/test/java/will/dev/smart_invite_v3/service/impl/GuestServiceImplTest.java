package will.dev.smart_invite_v3.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import will.dev.smart_invite_v3.dto.guest.request.BulkDeleteRequest;
import will.dev.smart_invite_v3.dto.guest.request.UpdateGuestRequest;
import will.dev.smart_invite_v3.dto.guest.response.GuestResponse;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.Guest;
import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.enums.NotificationMode;
import will.dev.smart_invite_v3.enums.RsvpStatus;
import will.dev.smart_invite_v3.exception.EventAccessDeniedException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.GuestRepository;
import will.dev.smart_invite_v3.repository.InvitationRepository;
import will.dev.smart_invite_v3.repository.PaymentRepository;
import will.dev.smart_invite_v3.service.EmailService;
import will.dev.smart_invite_v3.service.WhatsAppService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuestServiceImplTest {

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private InvitationRepository invitationRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private WhatsAppService whatsAppService;

    @InjectMocks
    private GuestServiceImpl guestService;

    private User organizer;
    private Event event;
    private Guest guest;

    @BeforeEach
    void setUp() {
        organizer = new User();
        organizer.setId(1L);

        event = new Event();
        event.setId(10L);
        event.setOrganizer(organizer);

        guest = new Guest();
        guest.setId(100L);
        guest.setFullName("Jean Dupont");
        guest.setEmail("jean@example.com");
        guest.setEvent(event);
        guest.setRsvpStatus(RsvpStatus.PENDING);
        guest.setNotificationMode(NotificationMode.EMAIL);
    }

    @Test
    @DisplayName("update modifie correctement les données d'un invité")
    void update_shouldModifyGuestFields() {
        when(guestRepository.findById(100L)).thenReturn(Optional.of(guest));
        when(guestRepository.save(any(Guest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateGuestRequest request = new UpdateGuestRequest(
                "Jean-Pierre Dupont",
                "jp.dupont@example.com",
                "+33612345678",
                4,                          // Integer, pas String
                NotificationMode.WHATSAPP
        );

        GuestResponse response = guestService.update(100L, request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.fullName()).isEqualTo("Jean-Pierre Dupont");
        assertThat(response.email()).isEqualTo("jp.dupont@example.com");
        assertThat(response.tableNumber()).isEqualTo(4);
        assertThat(response.notificationMode()).isEqualTo(NotificationMode.WHATSAPP);
    }

    @Test
    @DisplayName("update rejette la modification si l'organisateur n'est pas le propriétaire de l'événement")
    void update_shouldThrowAccessDenied_whenUserIsNotOrganizer() {
        when(guestRepository.findById(100L)).thenReturn(Optional.of(guest));

        UpdateGuestRequest request = new UpdateGuestRequest(
                "Nouveau Nom", null, null, null, null
        );

        assertThatThrownBy(() -> guestService.update(100L, request, 999L))
                .isInstanceOf(EventAccessDeniedException.class);
    }

    @Test
    @DisplayName("update lance RuntimeException si l'invité n'existe pas")
    void update_shouldThrowNotFound_whenGuestDoesNotExist() {
        when(guestRepository.findById(999L)).thenReturn(Optional.empty());

        UpdateGuestRequest request = new UpdateGuestRequest(
                "Nouveau Nom", null, null, null, null
        );

        assertThatThrownBy(() -> guestService.update(999L, request, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invité introuvable");
    }

    @Test
    @DisplayName("delete supprime l'invité après nettoyage de son invitation")
    void delete_shouldRemoveGuestAndCleanupInvitations() {
        when(guestRepository.findById(100L)).thenReturn(Optional.of(guest));
        // deleteGuestWithCleanup appelle findByGuestId pour trouver l'invitation avant de la supprimer
        when(invitationRepository.findByGuestId(100L)).thenReturn(Optional.empty());

        guestService.delete(100L, 1L);

        verify(invitationRepository).findByGuestId(100L);
        verify(guestRepository).delete(guest);
    }

    @Test
    @DisplayName("bulkDelete supprime tous les invités appartenant à l'organisateur")
    void bulkDelete_shouldDeleteAllOwnedGuests() {
        Guest guest2 = new Guest();
        guest2.setId(101L);
        guest2.setEvent(event);

        when(guestRepository.findById(100L)).thenReturn(Optional.of(guest));
        when(guestRepository.findById(101L)).thenReturn(Optional.of(guest2));

        BulkDeleteRequest request = new BulkDeleteRequest(List.of(100L, 101L));

        guestService.bulkDelete(request, 1L);

        verify(guestRepository).delete(guest);
        verify(guestRepository).delete(guest2);
    }
}
