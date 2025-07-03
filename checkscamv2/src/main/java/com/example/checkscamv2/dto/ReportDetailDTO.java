package com.example.checkscamv2.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDetailDTO {
    @NotNull(message = "Loại thông tin là bắt buộc")
    @Min(value = 1, message = "Loại phải là 1 (SDT), 2 (STK), hoặc 3 (URL)")
    @Max(value = 3, message = "Loại phải là 1 (SDT), 2 (STK), hoặc 3 (URL)")
    private Integer type;

    @NotBlank(message = "Thông tin là bắt buộc")
    private String info;

    private String description;
}