package com.wielkopolan.gymscheduler.controller;

import com.wielkopolan.gymscheduler.dto.ScheduleRequestDTO;
import com.wielkopolan.gymscheduler.entity.ScheduledTask;
import com.wielkopolan.gymscheduler.service.SchedulerService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * Retrieve a list of tasks by ID.
     *
     * @param id the task ID
     * @return the list of tasks
     */
    @GetMapping("/list/{id}")
    public List<ScheduledTask> getTask(@PathVariable final String id) {
        return schedulerService.getTask(id);
    }

    /**
     * Retrieve a list of tasks for member.
     *
     * @param memberId ID of member
     * @return the list of tasks
     */
    @GetMapping("/members/{memberId}/tasks/active")
    public List<ScheduledTask> getActiveTasksForMember(@PathVariable final String memberId) {
        return schedulerService.getActiveTasksForMember(memberId);
    }
}