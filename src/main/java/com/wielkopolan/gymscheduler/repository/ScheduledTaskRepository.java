package com.wielkopolan.gymscheduler.repository;

import com.wielkopolan.gymscheduler.entity.ScheduledTask;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface ScheduledTaskRepository extends MongoRepository<ScheduledTask, String> {
    List<ScheduledTask> findByProcessedFalseAndScheduledTimeBefore(final Instant scheduledTime);
    List<ScheduledTask> findByMemberIdAndProcessedFalse(final String memberId);
}