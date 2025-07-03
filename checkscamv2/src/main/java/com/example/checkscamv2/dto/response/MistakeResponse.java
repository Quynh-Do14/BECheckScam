package com.example.checkscamv2.dto.response;

import com.example.checkscamv2.constant.MistakeStatus;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MistakeResponse {
    private Long id;
    private String complaintReason;
    private String emailAuthorMistake;
    private MistakeStatus status;
    private LocalDateTime dateMistake;
    private List<String> attachmentUrls;
    private List<MistakeDetailResponse> mistakeDetails;
}