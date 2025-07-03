package com.example.checkscamv2.dto.request;

import com.example.checkscamv2.constant.MistakeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MistakeStatusUpdateRequest {
    @NotNull(message = "Mistake status cannot be null")
    private MistakeStatus status;
}