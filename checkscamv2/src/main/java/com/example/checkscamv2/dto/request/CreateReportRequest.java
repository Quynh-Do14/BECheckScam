package com.example.checkscamv2.dto.request;

import com.example.checkscamv2.dto.ReportDetailDTO;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReportRequest {
    @NotBlank(message = "Email người báo cáo là bắt buộc")
    @Email(message = "Email không hợp lệ")
    private String emailAuthorReport;

    @NotBlank(message = "Mô tả báo cáo là bắt buộc")
    private String description;

    private String moneyScam;

    @NotNull(message = "Danh sách chi tiết báo cáo không được để trống")
    @Size(min = 1, message = "Phải có ít nhất một chi tiết báo cáo")
    private List<ReportDetailDTO> reportDetails;

    private List<MultipartFile> attachments;

    @NotNull(message = "Danh mục là bắt buộc")
    private Long categoryId;

    private String captchaToken;
}
