package com.wielkopolan.gymscheduler.service;

import com.wielkopolan.gymscheduler.dto.GymResponseBody;
import com.wielkopolan.gymscheduler.entity.ScheduledTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskProcessingServiceTest {

    @Mock
    private ScheduledTaskService scheduledTaskService;

    @Mock
    private RetryableSenderService retryableSenderService;

    private TaskProcessingService service;

    @BeforeEach
    void setUp() {
        service = new TaskProcessingService(scheduledTaskService, retryableSenderService, 2);
    }

    @Test
    void processDueTasks_shouldProcessAndSaveTasks_whenSignupIsSuccessful() {
        // Given
        var task = new ScheduledTask("task-1", "member-1", Instant.now(), false);
        var responseBody = new GymResponseBody(true, true, false, "", "", "", false, "", false, 1, null, "Success");

        when(scheduledTaskService.getDueTasks(any())).thenReturn(List.of(task));
        when(retryableSenderService.sendRequestWithRetry(task)).thenReturn(Optional.of(responseBody));

        // When
        service.processDueTasks();

        // Then
        var taskCaptor = ArgumentCaptor.forClass(ScheduledTask.class);
        verify(scheduledTaskService).save(taskCaptor.capture());

        var savedTask = taskCaptor.getValue();
        assertEquals("task-1", savedTask.id());
        assertTrue(savedTask.processed());
    }

    @Test
    void processDueTasks_shouldMarkAsProcessed_whenUserAlreadySignedUp() {
        // Given
        var task = new ScheduledTask("task-2", "member-2", Instant.now(), false);
        var responseBody = new GymResponseBody(true, false, false, "", "", "", false, "", false, 2, TaskProcessingService.ALREADY_SIGNED_UP_RESPONSE, null);

        when(scheduledTaskService.getDueTasks(any())).thenReturn(List.of(task));
        when(retryableSenderService.sendRequestWithRetry(task)).thenReturn(Optional.of(responseBody));

        // When
        service.processDueTasks();

        // Then
        var taskCaptor = ArgumentCaptor.forClass(ScheduledTask.class);
        verify(scheduledTaskService).save(taskCaptor.capture());

        var savedTask = taskCaptor.getValue();
        assertEquals("task-2", savedTask.id());
        assertTrue(savedTask.processed());
    }

    @Test
    void processDueTasks_shouldNotSave_whenSignupFailsAndUserNotAlreadySignedUp() {
        // Given
        var task = new ScheduledTask("task-3", "member-3", Instant.now(), false);
        var responseBody = new GymResponseBody(true, false, false, "", "", "", false, "", false, 3, "Some other error", null);

        when(scheduledTaskService.getDueTasks(any())).thenReturn(List.of(task));
        when(retryableSenderService.sendRequestWithRetry(task)).thenReturn(Optional.of(responseBody));

        // When
        service.processDueTasks();

        // Then
        verify(scheduledTaskService, never()).save(any());
    }

    @Test
    void processDueTasks_shouldHandleHttpServerErrorExceptionGracefully() {
        // Given
        var task = new ScheduledTask("task-4", "member-4", Instant.now(), false);

        when(scheduledTaskService.getDueTasks(any())).thenReturn(List.of(task));
        when(retryableSenderService.sendRequestWithRetry(task)).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        // When
        assertDoesNotThrow(() -> service.processDueTasks());

        // Then
        verify(scheduledTaskService, never()).save(any());
    }

    @Test
    void processTask_byId_shouldProcessSuccessfullyIfExists() {
        // Given
        var taskId = "task-5";
        var task = new ScheduledTask(taskId, "member-5", Instant.now(), false);
        var responseBody = new GymResponseBody(true, true, false, "", "", "", false, "", false, 5, null, "Success");

        when(scheduledTaskService.getTask(taskId)).thenReturn(Optional.of(task));
        when(retryableSenderService.sendRequestWithRetry(task)).thenReturn(Optional.of(responseBody));

        // When
        service.processTask(taskId);

        // Then
        verify(scheduledTaskService).save(any(ScheduledTask.class));
    }

    @Test
    void processTask_byId_shouldDoNothingIfTaskDoesNotExist() {
        // Given
        var taskId = "non-existent-task";
        when(scheduledTaskService.getTask(taskId)).thenReturn(Optional.empty());

        // When
        service.processTask(taskId);

        // Then
        verify(retryableSenderService, never()).sendRequestWithRetry(any());
        verify(scheduledTaskService, never()).save(any());
    }
}

