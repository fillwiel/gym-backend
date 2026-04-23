package com.wielkopolan.gymscheduler.service;

import com.wielkopolan.gymscheduler.dto.ScheduleRequestDTO;
import com.wielkopolan.gymscheduler.entity.ScheduledTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TaskSchedulingService {

    private final ScheduledTaskService scheduledTaskService;
    private final String defaultMemberId;

    public TaskSchedulingService(final ScheduledTaskService scheduledTaskService, @Value("${app.default.memberId}") final String defaultMemberId) {
        this.scheduledTaskService = scheduledTaskService;
        this.defaultMemberId = defaultMemberId;
    }

    public ScheduledTask scheduleRequest(final ScheduleRequestDTO dto) {
        if (dto == null || dto.id() == null || dto.scheduledTime() == null) {
            throw new IllegalArgumentException("Invalid schedule request");
        }
        if (scheduledTaskService.existsById(dto.id())) {
            throw new IllegalArgumentException("Task already exists: " + dto.id());
        }

        final var task = createTaskFromDto(dto);
        return scheduledTaskService.save(task);
    }

    protected ScheduledTask createTaskFromDto(final ScheduleRequestDTO dto) {
        final var memberId = dto.memberId() == null || dto.memberId().isBlank() ? defaultMemberId : dto.memberId();
        return new ScheduledTask(dto.id(), memberId, dto.scheduledTime().toInstant(), false);
    }
}