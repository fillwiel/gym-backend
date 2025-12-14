package com.wielkopolan.gymscheduler.controller;

import com.wielkopolan.gymscheduler.dto.ScheduleRequestDTO;
import com.wielkopolan.gymscheduler.entity.ScheduledTask;
import com.wielkopolan.gymscheduler.service.SchedulerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final SchedulerService schedulerService;

    public ScheduleController(final SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }

    /**
     * Schedule a new task.
     *
     * @param dto the schedule request data
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void schedule(@RequestBody final ScheduleRequestDTO dto) {
        schedulerService.scheduleRequest(dto);
    }

    /**
     * Process all due tasks.
     */
    @PostMapping("/process-scheduled")
    @ResponseStatus(HttpStatus.OK)
    public void processScheduledTasks() {
        schedulerService.processDueTasks();
    }

    /**
     * Process a specific task by ID.
     *
     * @param id the task ID
     */
    @PostMapping("/process/{id}")
    @ResponseStatus(HttpStatus.OK)
    public void processTask(@PathVariable final String id) {
        schedulerService.processTask(id);
    }

    /**
     * Retrieve a task by ID.
     *
     * @param id the task ID
     * @return found task or 404
     */
    @GetMapping("/task/{id}")
    public ResponseEntity<ScheduledTask> getTask(@PathVariable final String id) {
        return schedulerService.getTask(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Retrieve a list of pending tasks for member.
     *
     * @param memberId ID of member
     * @return the list of tasks
     */
    @GetMapping("/members/{memberId}/tasks/pending")
    public List<ScheduledTask> getPendingTasksForMember(@PathVariable final String memberId) {
        return schedulerService.getPendingTasksForMember(memberId);
    }
    /**
     * Retrieve a list of all tasks for member.
     *
     * @param memberId ID of member
     * @return the list of tasks
     */
    @GetMapping("/members/{memberId}/tasks")
    public List<ScheduledTask> getTasksForMember(@PathVariable final String memberId) {
        return schedulerService.getTasksForMember(memberId);
    }
}