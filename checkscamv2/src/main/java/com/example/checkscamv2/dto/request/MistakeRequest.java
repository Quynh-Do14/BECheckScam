package com.example.checkscamv2.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MistakeRequest {
    @NotBlank(message = "Complainant email is required")
    @Email(message = "Invalid email format")
    private String emailAuthorMistake;

    @NotBlank(message = "Complaint reason/general description is required")
    @Size(max = 2000, message = "Complaint reason cannot exceed 2000 characters")
    private String complaintReason;

    @NotNull(message = "Mistake details cannot be null")
    @Size(min = 1, message = "At least one mistake detail must be provided")
    @Valid
    private List<MistakeDetailRequest> mistakeDetails;

    @NotBlank(message = "Captcha token is required")
    private String captchaToken;
}