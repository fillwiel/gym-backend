package com.wielkopolan.gymscheduler.service;

import com.wielkopolan.gymscheduler.dto.GymResponseBody;
import com.wielkopolan.gymscheduler.entity.ScheduledTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
public class RequestSenderService {

    private final WebClient client;
    private final String authCookieToken;

    public RequestSenderService(@Value("${auth.cookie.token}") final String authCookieToken) {
        this.client = WebClient.create("https://atmosfera-lodz.cms.efitness.com.pl");
        this.authCookieToken = authCookieToken;
    }

    public boolean sendPostRequest(final ScheduledTask task) {
        try {
            final var response = client.post()
                    .uri("/Schedule/RegisterForClass")
                    .header("Accept", "*/*")
                    .header("Accept-Language", "pl,en-US;q=0.9,en;q=0.8,pt-PT;q=0.7,pt;q=0.6")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("Cookie", "language=pl-PL; .ASPXAUTH_Cms=" + authCookieToken)
                    .header("Origin", "https://atmosfera-lodz.cms.efitness.com.pl")
                    .bodyValue("id=" + task.getId() + "&memberID=" + task.getMemberId() + "&promoCodeID=&promoCode=&g-recaptcha-response=")
                    .retrieve()
                    .bodyToMono(GymResponseBody.class)
                    .block();

            if (response != null && response.success()) {
                log.info("Request successful: {}", response.successMessage());
                return true;
            } else {
                log.warn("Request failed: {}", response != null ? response.errorMessage() : "No response");
                return false;
            }
        } catch (final Exception e) {
            log.error("Error during request: {}", e.getMessage());
            return false;
        }
    }
}