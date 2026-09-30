package will.dev.smart_invite_v3.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import will.dev.smart_invite_v3.dto.event.response.EventStatsResponse;
import will.dev.smart_invite_v3.dto.event.response.ThankYouTemplateResponse;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.ThankYouTemplate;
import will.dev.smart_invite_v3.entity.User;
import will.dev.smart_invite_v3.enums.EventType;
import will.dev.smart_invite_v3.enums.RsvpStatus;
import will.dev.smart_invite_v3.exception.EventAccessDeniedException;
import will.dev.smart_invite_v3.exception.EventNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.GuestRepository;
import will.dev.smart_invite_v3.repository.UserRepository;
import will.dev.smart_invite_v3.service.FirebaseStorageService;
import will.dev.smart_invite_v3.service.RedisService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GuestRepository guestRepository;

    @Mock
    private RedisService redisService;

    @Mock
    private EventScheduleService eventScheduleService;

    @Mock
    private FirebaseStorageService firebaseStorage;

    @InjectMocks
    private EventServiceImpl eventService;

    private User organizer;
    private Event event;

    @BeforeEach
    void setUp() {
        organizer = new User();
        organizer.setId(1L);
        organizer.setEmail("organizer@example.com");

        event = new Event();
        event.setId(10L);
        event.setTitle("Mariage Test");
        event.setType(EventType.MARIAGE);
        event.setOrganizer(organizer);
        event.setMaxGuests(100);
    }

    @Test
    @DisplayName("getStats devrait retourner le nombre réel d'invités et le taux d'occupation")
    void getStats_shouldReturnRealCountsAndOccupancy() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));
        when(guestRepository.countByEventId(10L)).thenReturn(50);
        when(guestRepository.countByEventIdAndRsvpStatus(10L, RsvpStatus.CONFIRMED)).thenReturn(30L);
        when(guestRepository.countByEventIdAndRsvpStatus(10L, RsvpStatus.PENDING)).thenReturn(15L);
        when(guestRepository.countByEventIdAndRsvpStatus(10L, RsvpStatus.DECLINED)).thenReturn(5L);

        EventStatsResponse stats = eventService.getStats(10L, 1L);

        assertThat(stats).isNotNull();
        assertThat(stats.totalGuests()).isEqualTo(50L);
        assertThat(stats.confirmedGuests()).isEqualTo(30L);
        assertThat(stats.pendingGuests()).isEqualTo(15L);
        assertThat(stats.declinedGuests()).isEqualTo(5L);
        assertThat(stats.occupancyRate()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("getStats devrait lancer EventAccessDeniedException si l'utilisateur n'est pas le créateur")
    void getStats_shouldThrowAccessDenied_whenNotOrganizer() {
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> eventService.getStats(10L, 999L))
                .isInstanceOf(EventAccessDeniedException.class);
    }

    @Test
    @DisplayName("getStats devrait lancer EventNotFoundException si l'événement n'existe pas")
    void getStats_shouldThrowNotFound_whenEventMissing() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.getStats(999L, 1L))
                .isInstanceOf(EventNotFoundException.class);
    }

    @Test
    @DisplayName("getThankYouTemplate retourne les valeurs par défaut quand aucun template personnalisé n'est configuré")
    void getThankYouTemplate_shouldReturnDefault_whenTemplateNull() {
        event.setThankYouTemplate(null);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        ThankYouTemplateResponse res = eventService.getThankYouTemplate(10L, 1L);

        assertThat(res).isNotNull();
        assertThat(res.isCustom()).isFalse();
        assertThat(res.accroche()).isEqualTo(EventServiceImpl.DEFAULT_ACCROCHE);
    }

    @Test
    @DisplayName("getThankYouTemplate retourne le template custom quand il est configuré")
    void getThankYouTemplate_shouldReturnCustom_whenTemplateExists() {
        ThankYouTemplate custom = ThankYouTemplate.builder()
                .accroche("Merci du fond du coeur")
                .corpsLigne1("Votre présence a illuminé cette journée")
                .corpsLigne2("Avec toute notre amitié")
                .conclusion("À bientôt !")
                .build();
        event.setThankYouTemplate(custom);
        when(eventRepository.findById(10L)).thenReturn(Optional.of(event));

        ThankYouTemplateResponse res = eventService.getThankYouTemplate(10L, 1L);

        assertThat(res).isNotNull();
        assertThat(res.isCustom()).isTrue();
        assertThat(res.accroche()).isEqualTo("Merci du fond du coeur");
    }
}
