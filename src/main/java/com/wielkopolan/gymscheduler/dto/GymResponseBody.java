package com.wielkopolan.gymscheduler.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GymResponseBody(
        @JsonProperty("IsLogged") Boolean isLogged,
        @JsonProperty("Success") Boolean success,
        @JsonProperty("ShowCaptcha") Boolean showCaptcha,
        @JsonProperty("RedirectUrl") String redirectUrl,
        @JsonProperty("CodeMd5") String codeMd5,
        @JsonProperty("PictureString") String pictureString,
        @JsonProperty("PaymentRequired") Boolean paymentRequired,
        @JsonProperty("PaymentInfo") String paymentInfo,
        @JsonProperty("ShowSubmitButtonWithErrors") Boolean showSubmitButtonWithErrors,
        @JsonProperty("ClassID") Integer classId,
        @JsonProperty("ErrorMessage") String errorMessage,
        @JsonProperty("SuccessMessage") String successMessage
) {}