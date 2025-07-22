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

@Slf4j
@Service
public class SchedulerService {

    private final ScheduledTaskRepository repository;
    private final RequestSenderService senderService;
    private final ZoneId zoneId;
    private final String defaultMemberId;
    private final int numberOfDaysEndRange;

    public SchedulerService(ScheduledTaskRepository repository, RequestSenderService senderService, @Value("${app.default.memberId}") final String defaultMemberId, @Value("${app.days.range}") final int numberOfDaysEndRange) {
        this.repository = repository;
        this.senderService = senderService;
        this.defaultMemberId = defaultMemberId;
        this.numberOfDaysEndRange = numberOfDaysEndRange;
        this.zoneId = ZoneId.of("Europe/Warsaw");
    }

    public void scheduleRequest(ScheduleRequestDTO dto) {
        ScheduledTask task = new ScheduledTask();
        task.setMemberId(defaultMemberId);
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

        var dueTasks = repository.findByProcessedFalseAndScheduledTimeBefore(endOfRange);
        log.info("Found {} due tasks to process", dueTasks.size());
        for (var task : dueTasks) {
            processTask(task);
        }
    }

    public void processTask(final String id) {
        repository.findById(id).ifPresent(this::processTask);
    }

    private void processTask(final ScheduledTask task) {
        boolean success = senderService.sendPostRequest(task);
        if (success) {
            log.info("Successfully signed up for class with ID {}", task.getId());
            task.setProcessed(true);
            repository.save(task);
        } else {
            log.warn("Failed to sign up for class with ID {}", task.getId());
        }
    }

    public List<ScheduledTask> getTask(final String id) {
        return repository.findAllById(List.of(id));
    }

    public List<ScheduledTask> getActiveTasksForMember(final String memberId) {
        return repository.findByMemberIdAndProcessedFalse(memberId);
    }

    private static Instant convertTime(final OffsetDateTime dateTime) {
        return dateTime.toInstant().atZone(ZoneOffset.UTC).toInstant();
    }
}