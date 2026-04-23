package com.wielkopolan.gymscheduler.controller;

import com.wielkopolan.gymscheduler.dto.ScheduleRequestDTO;
import com.wielkopolan.gymscheduler.entity.ScheduledTask;
import com.wielkopolan.gymscheduler.service.ScheduledTaskService;
import com.wielkopolan.gymscheduler.service.TaskProcessingService;
import com.wielkopolan.gymscheduler.service.TaskSchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final TaskProcessingService taskProcessingService;
    private final TaskSchedulingService taskSchedulingService;
    private final ScheduledTaskService scheduledTaskService;

    /**
     * Schedule a new task.
     *
     * @param dto the schedule request data
     * @return created {@link ScheduledTask}
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<ScheduledTask> schedule(@RequestBody final ScheduleRequestDTO dto) {
        return ResponseEntity.ok(taskSchedulingService.scheduleRequest(dto));
    }

    /**
     * Process all due tasks.
     */
    @PostMapping("/process-scheduled")
    @ResponseStatus(HttpStatus.OK)
    public void processScheduledTasks() {
        taskProcessingService.processDueTasks();
    }

    /**
     * Process a specific task by ID.
     *
     * @param id the task ID
     */
    @PostMapping("/process/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void processTask(@PathVariable final String id) {
        taskProcessingService.processTask(id);
    }

    /**
     * Retrieve a task by ID.
     *
     * @param id the task ID
     * @return found task or 404
     */
    @GetMapping("/task/{id}")
    public ResponseEntity<ScheduledTask> getTask(@PathVariable final String id) {
        return scheduledTaskService.getTask(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Retrieve a list of pending tasks for member.
     *
     * @param memberId ID of member
     * @return the list of tasks
     */
    @GetMapping("/members/{memberId}/tasks/pending")
    public List<ScheduledTask> getPendingTasksForMember(@PathVariable final String memberId) {
        return scheduledTaskService.getPendingTasksForMember(memberId);
    }
    /**
     * Retrieve a list of all tasks for member.
     *
     * @param memberId ID of member
     * @return the list of tasks
     */
    @GetMapping("/members/{memberId}/tasks")
    public List<ScheduledTask> getTasksForMember(@PathVariable final String memberId) {
        return scheduledTaskService.getTasksForMember(memberId);
    }
}