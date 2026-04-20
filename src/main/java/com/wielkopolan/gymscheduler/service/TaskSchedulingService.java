package com.wielkopolan.gymscheduler.service;

import com.wielkopolan.gymscheduler.dto.ScheduleRequestDTO;
import com.wielkopolan.gymscheduler.entity.ScheduledTask;
import com.wielkopolan.gymscheduler.repository.ScheduledTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
public class TaskSchedulingService {

    private final ScheduledTaskRepository repository;
    private final String defaultMemberId;

    public TaskSchedulingService(final ScheduledTaskRepository repository, @Value("${app.default.memberId}") final String defaultMemberId) {
        this.repository = repository;
        this.defaultMemberId = defaultMemberId;
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

    private static Instant convertTime(final OffsetDateTime dateTime) {
        //TODO remove redundant .atZone(ZoneOffset.UTC).toInstant() , but test first.
        return dateTime.toInstant().atZone(ZoneOffset.UTC).toInstant();
    }
}