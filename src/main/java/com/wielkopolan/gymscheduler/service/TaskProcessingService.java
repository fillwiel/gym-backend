package com.wielkopolan.gymscheduler.service;

import com.wielkopolan.gymscheduler.dto.GymResponseBody;
import com.wielkopolan.gymscheduler.entity.ScheduledTask;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

@Slf4j
@Service
public class TaskProcessingService {

    public static final String ALREADY_SIGNED_UP_RESPONSE = "Klubowicz jest już zapisany na te zajęcia";
    private final ScheduledTaskService scheduledTaskService;
    private final RetryableSenderService retryableSenderService;
    private final ZoneId zoneId;
    private final int numberOfDaysEndRange;

    public TaskProcessingService(final ScheduledTaskService scheduledTaskService, final RetryableSenderService retryableSenderService, @Value("${app.days.range}") final int numberOfDaysEndRange) {
        this.scheduledTaskService = scheduledTaskService;
        this.retryableSenderService = retryableSenderService;
        this.numberOfDaysEndRange = numberOfDaysEndRange;
        this.zoneId = ZoneId.of("Europe/Warsaw");
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
        final var dueTasks = scheduledTaskService.getDueTasks(endOfRange);
        log.debug("Found {} due tasks to process", dueTasks.size());
        dueTasks.forEach(this::processTask);
    }

    public void processTask(final String id) {
        scheduledTaskService.getTask(id).ifPresent(this::processTask);
    }

    private void processTask(final ScheduledTask task) {
        try {
            retryableSenderService.sendRequestWithRetry(task).ifPresentOrElse(responseBody -> {
                if (Boolean.TRUE.equals(responseBody.success())) {
                    markTaskProcessed(task, "Successfully signed up for class with ID {}", task.id());
                } else {
                    handleFailedResponse(task, responseBody);
                }
            }, () -> log.warn("Could not sign up for class with id {}", task.id()));
        } catch (final HttpServerErrorException e) {
            log.error("Could not send post request to scheduled task {}", task.id(), e);
        }
    }

    private void markTaskProcessed(final ScheduledTask task, final String logMessage, final Object... params) {
        log.info(logMessage, params);
        final var updatedTask = new ScheduledTask(task.id(), task.memberId(), task.scheduledTime(), true);
        scheduledTaskService.save(updatedTask);
    }

    private void handleFailedResponse(final ScheduledTask task, final GymResponseBody responseBody) {
        if (ALREADY_SIGNED_UP_RESPONSE.equals(responseBody.errorMessage())) {
            markTaskProcessed(task, "Marking task {} as processed, because member is already signed up.", task.id());
        }
        log.warn("Failed to sign up for class with ID {}. Reason: {}", task.id(), responseBody.errorMessage());
    }
}