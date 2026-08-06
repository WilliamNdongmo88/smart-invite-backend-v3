package will.dev.smart_invite_v3.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import will.dev.smart_invite_v3.entity.Event;
import will.dev.smart_invite_v3.entity.EventSchedule;
import will.dev.smart_invite_v3.exception.EventNotFoundException;
import will.dev.smart_invite_v3.repository.EventRepository;
import will.dev.smart_invite_v3.repository.EventScheduleRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventScheduleService {

    private final EventRepository         eventRepository;
    private final EventScheduleRepository eventScheduleRepository;
    private final ThankYouJobService      thankYouJobService;

    /**
     * Crée ou met à jour l'entrée event_schedules pour un événement.
     * scheduled_for = eventDate + 1 jour.
     */
    @Transactional
    public void schedule(Long eventId, LocalDateTime eventDate) {
        try {
        if (eventDate == null) {
            log.warn("[EventSchedule] eventDate null pour l'événement {} — schedule ignoré", eventId);
            return;
        }

        // TEMPORAIRE — pour test
        LocalDateTime scheduledFor = LocalDateTime.now().plusMinutes(15);
        // LocalDateTime scheduledFor = event.getEventDate().plusDays(1);


            Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        EventSchedule schedule = eventScheduleRepository.findByEventId(eventId)
                .orElseGet(() -> EventSchedule.builder().event(event).build());

        schedule.setScheduledFor(scheduledFor);
        schedule.setExecuted(false);

        eventScheduleRepository.save(schedule);
        log.info("[EventSchedule] Job planifié pour l'événement {} à {}", eventId, scheduledFor);
        } catch (Exception e) {
            log.error("[EventSchedule] ERREUR schedule() pour l'événement {} : {}", eventId, e.getMessage(), e);
        }
    }

    /**
     * Poll toutes les minutes — exécute les jobs dont scheduled_for est dépassé.
     */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void processPendingJobs() {
        List<EventSchedule> due = eventScheduleRepository
                .findAllByExecutedFalseAndScheduledForBefore(LocalDateTime.now());

        for (EventSchedule schedule : due) {
            Long eventId = schedule.getEvent().getId();
            try {
                log.info("[EventSchedule] Exécution job remerciement pour l'événement {}", eventId);
                thankYouJobService.execute(eventId);
                schedule.setExecuted(true);
                eventScheduleRepository.save(schedule);
            } catch (Exception e) {
                log.error("[EventSchedule] Échec job pour l'événement {} : {}", eventId, e.getMessage());
            }
        }
    }
}
