package com.wielkopolan.gymscheduler.service;

import com.wielkopolan.gymscheduler.dto.GymResponseBody;
import com.wielkopolan.gymscheduler.dto.ScheduleRequestDTO;
import com.wielkopolan.gymscheduler.entity.ScheduledTask;
import com.wielkopolan.gymscheduler.repository.ScheduledTaskRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.http.ResponseEntity;

import java.time.*;
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
        try {
            sendRequestForTask(task).ifPresentOrElse(responseBody -> {
                if (Boolean.TRUE.equals(responseBody.success())) {
                    markTaskProcessed(task, "Successfully signed up for class with ID {}", task.getId());
                } else {
                    handleFailedResponse(task, responseBody);
                }
            }, () -> log.warn("Could not send post request to scheduled task {}", task.getId()));
        } catch (final HttpServerErrorException e) {
            log.error("Could not send post request to scheduled task {}", task.getId(), e);
        }
    }

    private Optional<GymResponseBody> sendRequestForTask(final ScheduledTask task) {
        final ResponseEntity<GymResponseBody> entity = senderService.sendPostRequest(task);
        return Optional.ofNullable(entity).map(ResponseEntity::getBody);
    }

    private void markTaskProcessed(final ScheduledTask task, final String message, final Object... params) {
        log.info(message, params);
        task.setProcessed(true);
        repository.save(task);
    }

    private void handleFailedResponse(final ScheduledTask task, final GymResponseBody responseBody) {
        if (ALREADY_SIGNED_UP_RESPONSE.equals(responseBody.errorMessage())) {
            markTaskProcessed(task, "Marking task {} as processed because member is already signed up", task.getId());
        }
        log.warn("Failed to sign up for class with ID {}. Reason: {}", task.getId(), responseBody.errorMessage());
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