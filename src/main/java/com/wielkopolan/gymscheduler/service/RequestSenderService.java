package com.wielkopolan.gymscheduler.service;

import com.wielkopolan.gymscheduler.dto.GymResponseBody;
import com.wielkopolan.gymscheduler.entity.ScheduledTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
public class RequestSenderService {

    private final RestClient client;
    private final String authCookieToken;
    private final String cmsURL;

    public RequestSenderService(@Value("${auth.cookie.token}") final String authCookieToken, @Value("${app.cms.url}") final String cmsURL, final RestClient.Builder builder) {
        this.authCookieToken = authCookieToken;
        this.cmsURL = cmsURL;
        this.client = builder.baseUrl(cmsURL).build();
    }

    public ResponseEntity<GymResponseBody> sendPostRequest(final ScheduledTask task) throws HttpServerErrorException {
        final var requestBody = "id=" + task.id() + "&memberID=" + task.memberId();
        log.debug("Sending POST request with body: {}", requestBody);

        try {
            final var response = client.post()
                    .uri("/Schedule/RegisterForClass")
                    .header("Accept", "*/*")
                    .header("Accept-Language", "pl,en-US;q=0.9,en;q=0.8,pt-PT;q=0.7,pt;q=0.6")
                    .header("Connection", "keep-alive")
                    .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                    .header("Cookie", "language=pl-PL; .ASPXAUTH_Cms=" + authCookieToken)
                    .header("Origin", cmsURL)
                    .body(requestBody)
                    .retrieve()
                    .toEntity(GymResponseBody.class);

            log.debug("Received response with status {}: {}", response.getStatusCode(), response.getBody());
            return response;
        } catch (final RestClientResponseException e) {
            log.error("HTTP error while sending request. Status: {}, Response body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw e;
        }
    }
}