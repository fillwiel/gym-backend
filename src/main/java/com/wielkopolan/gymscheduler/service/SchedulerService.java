package com.wielkopolan.gymscheduler.service;

import com.wielkopolan.gymscheduler.dto.ScheduleRequestDTO;
import com.wielkopolan.gymscheduler.entity.ScheduledTask;
import com.wielkopolan.gymscheduler.repository.ScheduledTaskRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SchedulerService {

    public static final String ALREADY_SIGNED_UP_RESPONSE = "Klubowicz jest już zapisany na te zajęcia";
    private final ScheduledTaskRepository repository;
    private final RequestSenderService senderService;
    private final ZoneId zoneId;
    private final String defaultMemberId;
    private final int numberOfDaysEndRange;

    public SchedulerService(final ScheduledTaskRepository repository, final RequestSenderService senderService, @Value("${app.default.memberId}") final String defaultMemberId, @Value("${app.days.range}") final int numberOfDaysEndRange) {
        this.repository = repository;
        this.senderService = senderService;
        this.defaultMemberId = defaultMemberId;
        this.numberOfDaysEndRange = numberOfDaysEndRange;
        this.zoneId = ZoneId.of("Europe/Warsaw");
    }

    public void scheduleRequest(final ScheduleRequestDTO dto) {
        if (dto == null || dto.id() == null || dto.scheduledTime() == null) {
            throw new IllegalArgumentException("Invalid schedule request");
        }
        if (repository.existsById(dto.id())) {
            throw new IllegalArgumentException("Task already exists: " + dto.id());
        }
        final var task = new ScheduledTask();
        task.setMemberId(dto.memberId() == null || dto.memberId().isBlank() ? defaultMemberId : dto.memberId());
        task.setId(dto.id());
        task.setScheduledTime(convertTime(dto.scheduledTime()));
        repository.save(task);
    }

    @PostConstruct
    public void processDueTasksOnStartup() {
        log.info("Processing due tasks on server startup...");
        processDueTasks();
    }

    @Scheduled(cron = "0 0 6 * * *", zone = "Europe/Warsaw")
    public void processDueTasksDaily() {
        processDueTasks();
    }

    public void processDueTasks() {
        final var endOfRange = ZonedDateTime.now(zoneId).plusDays(numberOfDaysEndRange).toLocalDate().atTime(23, 59, 59).toInstant(ZoneOffset.UTC);

        final var dueTasks = repository.findByProcessedFalseAndScheduledTimeBefore(endOfRange);
        log.info("Found {} due tasks to process", dueTasks.size());
        for (var task : dueTasks) {
            processTask(task);
        }
    }

    public void processTask(final String id) {
        repository.findById(id).ifPresent(this::processTask);
    }

    private void processTask(final ScheduledTask task) {
        final var responseBody = senderService.sendPostRequest(task).getBody();
        if (responseBody == null) {
            log.warn("Could not send post request to scheduled task {}", task.getId());
            return;
        }
        if (responseBody.success()) {
            log.info("Successfully signed up for class with ID {}", task.getId());
            task.setProcessed(true);
            repository.save(task);
        } else {
            if (ALREADY_SIGNED_UP_RESPONSE.equals(responseBody.errorMessage())) {
                task.setProcessed(true);
                repository.save(task);
            }
            log.warn("Failed to sign up for class with ID {}. Reason: {}", task.getId(), responseBody.errorMessage());
        }
    }

    public Optional<ScheduledTask> getTask(final String id) {
        return repository.findById(id);
    }

    public List<ScheduledTask> getPendingTasksForMember(final String memberId) {
        return repository.findByMemberIdAndProcessedFalse(memberId);
    }

    public List<ScheduledTask> getTasksForMember(final String memberId) {
        return repository.findByMemberId(memberId);
    }

    private static Instant convertTime(final OffsetDateTime dateTime) {
        //TODO remove redundant .atZone(ZoneOffset.UTC).toInstant() , but test first.
        return dateTime.toInstant().atZone(ZoneOffset.UTC).toInstant();
    }
}