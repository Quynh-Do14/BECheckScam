package com.example.checkscamv2.dto.response;

import com.example.checkscamv2.constant.MistakeDetailType; // ĐÃ THAY ĐỔI IMPORT
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MistakeDetailResponse {
    private Long id;
    private MistakeDetailType type; // ĐÃ THAY ĐỔI KIỂU DỮ LIỆU
    private String info;
}