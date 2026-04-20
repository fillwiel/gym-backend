package com.wielkopolan.gymscheduler.service;

import com.wielkopolan.gymscheduler.dto.GymResponseBody;
import com.wielkopolan.gymscheduler.entity.ScheduledTask;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;

import java.util.Optional;


@Service
@RequiredArgsConstructor
public class RetryableSenderService {

    private final RequestSenderService senderService;

    /**
     * Sends a request for the given scheduled task with retry logic.
     * Retries on HTTP server errors with a 3-second delay.
     *
     * @param task the scheduled task containing request details
     * @return an Optional containing the gym response body if successful
     */
    @Retryable(
            retryFor = HttpServerErrorException.class,
            backoff = @Backoff(delay = 3000)
    )
    public Optional<GymResponseBody> sendRequestWithRetry(final ScheduledTask task) {
        final var response = senderService.sendPostRequest(task);
        return Optional.ofNullable(response).map(ResponseEntity::getBody);
    }
}