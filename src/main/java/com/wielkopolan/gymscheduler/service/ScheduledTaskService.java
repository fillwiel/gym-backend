package com.wielkopolan.gymscheduler.service;

import com.wielkopolan.gymscheduler.entity.ScheduledTask;
import com.wielkopolan.gymscheduler.repository.ScheduledTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class ScheduledTaskService {

    private final ScheduledTaskRepository repository;

    /**
     * Retrieves a list of due tasks that have not been processed yet and are scheduled before the given time.
     *
     * @param endOfRange end range of time of {@link ScheduledTask} by which they are filtered
     * @return a list of due {@link ScheduledTask}s
     */
    public List<ScheduledTask> getDueTasks(final Instant endOfRange) {
        return repository.findByProcessedFalseAndScheduledTimeBefore(endOfRange);
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

    public boolean existsById(final String id) {
        return repository.existsById(id);
    }

    public ScheduledTask save(final ScheduledTask task) {
        return repository.save(task);
    }
}

