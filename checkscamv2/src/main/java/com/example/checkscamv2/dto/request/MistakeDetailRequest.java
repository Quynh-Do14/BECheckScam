package com.example.checkscamv2.dto.request;

import com.example.checkscamv2.constant.MistakeDetailType; // ĐÃ THAY ĐỔI IMPORT
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MistakeDetailRequest {
    @NotNull(message = "Mistake detail type cannot be null")
    private MistakeDetailType type; // ĐÃ THAY ĐỔI KIỂU DỮ LIỆU

    @NotBlank(message = "Mistake detail info cannot be blank")
    @Size(max = 1000, message = "Mistake detail info cannot exceed 1000 characters")
    private String info;
}