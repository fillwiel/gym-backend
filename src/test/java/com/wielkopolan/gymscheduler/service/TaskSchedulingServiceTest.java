package com.wielkopolan.gymscheduler.service;

import com.wielkopolan.gymscheduler.dto.ScheduleRequestDTO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TaskSchedulingServiceTest {

    @Test
    void scheduleRequest_shouldThrowException_whenDtoIsNull() {
        // Given
        final var scheduledTaskServiceMock = Mockito.mock(ScheduledTaskService.class);
        final var service = new TaskSchedulingService(scheduledTaskServiceMock, "defaultId");

        // When & Then
        final var exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.scheduleRequest(null)
        );
        assertEquals("Invalid schedule request", exception.getMessage());
    }

    @Test
    void scheduleRequest_shouldThrowException_whenIdIsNull() {
        // Given
        final var scheduledTaskServiceMock = Mockito.mock(ScheduledTaskService.class);
        final var service = new TaskSchedulingService(scheduledTaskServiceMock, "defaultId");
        final var testTime = OffsetDateTime.of(2026, 4, 15, 10, 30, 0, 0, ZoneOffset.ofHours(2));
        final var dto = new ScheduleRequestDTO("member123", null, testTime);

        // When & Then
        final var exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.scheduleRequest(dto)
        );
        assertEquals("Invalid schedule request", exception.getMessage());
    }

    @Test
    void scheduleRequest_shouldThrowException_whenScheduledTimeIsNull() {
        // Given
        final var scheduledTaskServiceMock = Mockito.mock(ScheduledTaskService.class);
        final var service = new TaskSchedulingService(scheduledTaskServiceMock, "defaultId");
        final var dto = new ScheduleRequestDTO("member123", "taskId", null);

        // When & Then
        final var exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.scheduleRequest(dto)
        );
        assertEquals("Invalid schedule request", exception.getMessage());
    }

    @Test
    void scheduleRequest_shouldThrowException_whenTaskAlreadyExists() {
        // Given
        final var scheduledTaskServiceMock = Mockito.mock(ScheduledTaskService.class);
        final var service = new TaskSchedulingService(scheduledTaskServiceMock, "defaultId");
        final var testTime = OffsetDateTime.of(2026, 4, 15, 10, 30, 0, 0, ZoneOffset.ofHours(2));
        final var dto = new ScheduleRequestDTO("member123", "taskId", testTime);

        Mockito.when(scheduledTaskServiceMock.existsById("taskId")).thenReturn(true);

        // When & Then
        final var exception = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.scheduleRequest(dto)
        );
        assertEquals("Task already exists: taskId", exception.getMessage());
    }

    @Test
    void scheduleRequest_shouldSaveTask_whenValidRequest() {
        // Given
        final var scheduledTaskServiceMock = Mockito.mock(ScheduledTaskService.class);
        final var service = new TaskSchedulingService(scheduledTaskServiceMock, "defaultId");
        final var testTime = OffsetDateTime.now();
        final var dto = new ScheduleRequestDTO("member123", "taskId", testTime);

        Mockito.when(scheduledTaskServiceMock.existsById("taskId")).thenReturn(false);

        // When
        service.scheduleRequest(dto);

        // Then
        org.mockito.ArgumentCaptor<com.wielkopolan.gymscheduler.entity.ScheduledTask> taskCaptor = org.mockito.ArgumentCaptor.forClass(com.wielkopolan.gymscheduler.entity.ScheduledTask.class);
        Mockito.verify(scheduledTaskServiceMock).save(taskCaptor.capture());

        final var savedTask = taskCaptor.getValue();
        assertEquals("taskId", savedTask.id());
        assertEquals("member123", savedTask.memberId());
        assertEquals(testTime.toInstant(), savedTask.scheduledTime());
    }

    @Test
    void scheduleRequest_shouldUseDefaultMemberId_whenMemberIdIsBlank() {
        // Given
        final var scheduledTaskServiceMock = Mockito.mock(ScheduledTaskService.class);
        final var service = new TaskSchedulingService(scheduledTaskServiceMock, "defaultId");
        final var testTime = OffsetDateTime.now();
        final var dto = new ScheduleRequestDTO("  ", "taskId", testTime);

        Mockito.when(scheduledTaskServiceMock.existsById("taskId")).thenReturn(false);

        // When
        service.scheduleRequest(dto);

        // Then
        org.mockito.ArgumentCaptor<com.wielkopolan.gymscheduler.entity.ScheduledTask> taskCaptor = org.mockito.ArgumentCaptor.forClass(com.wielkopolan.gymscheduler.entity.ScheduledTask.class);
        Mockito.verify(scheduledTaskServiceMock).save(taskCaptor.capture());

        final var savedTask = taskCaptor.getValue();
        assertEquals("taskId", savedTask.id());
        assertEquals("defaultId", savedTask.memberId());
    }

    @Test
    void shouldCreateTaskWithCorrectInstantAndVerifyConvertTimeReplacement() {
        // Given
        final var scheduledTaskServiceMock = Mockito.mock(ScheduledTaskService.class);
        final TaskSchedulingService service = new TaskSchedulingService(scheduledTaskServiceMock, "defaultId");

        final var testTime = OffsetDateTime.of(2026, 4, 15, 10, 30, 0, 0, ZoneOffset.ofHours(2));
        final var dto = new ScheduleRequestDTO("member123", "taskId", testTime);

        // When
        final var result = service.createTaskFromDto(dto);

        // Then
        // Before the refactor, the convertTime method was:
        // dateTime.toInstant().atZone(ZoneOffset.UTC).toInstant()
        // Here we assert our new encapsulated approach (dto.scheduledTime().toInstant()) gives the exact same result:
        final var expectedInstant = testTime.toInstant().atZone(ZoneOffset.UTC).toInstant();

        assertNotNull(result);
        assertEquals("taskId", result.id());
        assertEquals("member123", result.memberId());
        assertEquals(expectedInstant, result.scheduledTime());
    }

}