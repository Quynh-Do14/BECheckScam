package com.example.checkscamv2.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class YearlyReportStatsDTO {
    private int year;
    private long count;

    public YearlyReportStatsDTO(int year, long count) {
        this.year = year;
        this.count = count;
    }

}