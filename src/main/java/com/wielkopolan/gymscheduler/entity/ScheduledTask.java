package com.wielkopolan.gymscheduler.entity;

import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("scheduled_tasks")
public record ScheduledTask(
        String id,
        String memberId,
        Instant scheduledTime,
        boolean processed
) {
}