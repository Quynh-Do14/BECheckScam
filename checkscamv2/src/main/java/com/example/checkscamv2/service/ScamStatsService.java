package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.response.SubjectDetailResponseDTO;
import com.example.checkscamv2.dto.response.TopScamItemResponseDTO;
import java.util.List;

public interface ScamStatsService {
    Object getPhoneScamStatsInfo(String info);
    Object getBankScamStatsInfo(String info);
    Object getUrlScamStatsInfo(String info);
    
    // Methods for ranking
    List<TopScamItemResponseDTO> getTopPhoneScams();
    List<TopScamItemResponseDTO> getTopBankScams();
    List<TopScamItemResponseDTO> getTopUrlScams();
    
    // Methods for updating view counts
    void incrementPhoneViewCount(String phoneNumber);
    void incrementBankViewCount(String bankAccount);
    void incrementUrlViewCount(String url);
    
    // Method for getting subject detail
    SubjectDetailResponseDTO getSubjectDetail(String info, Integer type);
}
