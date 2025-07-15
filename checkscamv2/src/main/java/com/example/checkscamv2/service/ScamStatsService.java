package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.response.SubjectDetailResponseDTO;
import com.example.checkscamv2.dto.response.TopScamItemResponseDTO;
import java.util.List;

public interface ScamStatsService {
    List<TopScamItemResponseDTO> getTopPhoneScams();
    List<TopScamItemResponseDTO> getTopBankScams();
    List<TopScamItemResponseDTO> getTopUrlScams();
    
    void incrementPhoneViewCount(String phoneNumber);
    void incrementBankViewCount(String bankAccount);
    void incrementUrlViewCount(String url);
    
    SubjectDetailResponseDTO getSubjectDetail(String info, Integer type);
}
