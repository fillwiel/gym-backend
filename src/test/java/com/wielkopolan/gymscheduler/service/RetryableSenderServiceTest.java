package com.wielkopolan.gymscheduler.service;

import com.wielkopolan.gymscheduler.dto.GymResponseBody;
import com.wielkopolan.gymscheduler.entity.ScheduledTask;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests retry logic for 500 server errors on schedule POST requests.
 */
@SpringBootTest
class RetryableSenderServiceTest {

    @Autowired
    private RetryableSenderService retryableSenderService;

    @MockitoBean
    private RequestSenderService senderService;

    @Test
    void whenExactInternalServerErrorSubclassOccurs_thenRetryIsTriggered() {
        // Given
        ScheduledTask task = new ScheduledTask();
        task.setId("class-500");

        // Replicate the exact production exception with an HTML body
        final var htmlErrorBody = "<!DOCTYPE html><html id=\"error-page\"><body>Oops. Coś poszło nie tak</body></html>";
        final var real500Error = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                HttpHeaders.EMPTY,
                htmlErrorBody.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        // When: Mock the exact exception
        when(senderService.sendPostRequest(any(ScheduledTask.class)))
                .thenThrow(real500Error);

        // Then
        try {
            retryableSenderService.sendRequestWithRetry(task);
        } catch (final HttpServerErrorException _) {
            // Expected after all retries fail
        }

        // Verify it retried 2 times after the initial failure (3 total attempts)
        verify(senderService, times(3)).sendPostRequest(task);
    }

    @Test
    void whenSendRequestFails_thenRetryIsTriggered() {
        // Given
        ScheduledTask task = new ScheduledTask();
        task.setId("class-123");

        // When
        // Mock the underlying senderService to always throw an error
        when(senderService.sendPostRequest(any(ScheduledTask.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        //Then
        try {
            retryableSenderService.sendRequestWithRetry(task);
        } catch (final HttpServerErrorException _) {
            // The exception is expected after all retries fail
        }

        // Verify that the method was called 4 times (1 initial + 2 retries)
        verify(senderService, times(3)).sendPostRequest(task);
    }

    @Test
    void whenSendRequestSucceedsOnRetry_thenMethodCompletes() {
        // Given
        var task = new ScheduledTask();
        task.setId("class-123");
        final var successBody = new GymResponseBody(true, true, false, null, null, null, false, null, false, 123, null, "Success");
        final var successEntity = ResponseEntity.ok(successBody);

        // Mock the senderService to fail twice, then succeed
        when(senderService.sendPostRequest(any(ScheduledTask.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
                .thenReturn(successEntity);

        // When
        retryableSenderService.sendRequestWithRetry(task);

        // Then
        // Verify the method was called 2 times (1 failure + 1 success)
        verify(senderService, times(2)).sendPostRequest(task);
    }

    @Test
    void whenHttpClientErrorExceptionOccurs_thenNoRetryIsTriggered() {
        // Given
        var task = new ScheduledTask();
        task.setId("class-123");

        // Mock the underlying senderService to throw a 400 Bad Request error
        when(senderService.sendPostRequest(any(ScheduledTask.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        // Then
        try {
            retryableSenderService.sendRequestWithRetry(task);
        } catch (final HttpClientErrorException _) {
            // Expected to fail immediately
        }

        // Verify that the method was called exactly 1 time (no retries)
        verify(senderService, times(1)).sendPostRequest(task);
    }
}