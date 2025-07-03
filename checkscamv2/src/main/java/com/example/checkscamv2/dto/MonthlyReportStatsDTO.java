package com.example.checkscamv2.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MonthlyReportStatsDTO {
    private int month;
    private long count;

    public MonthlyReportStatsDTO(int month, long count) {
        this.month = month;
        this.count = count;
    }
}
