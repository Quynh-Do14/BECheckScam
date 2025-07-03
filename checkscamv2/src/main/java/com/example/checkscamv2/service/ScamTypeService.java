package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.ScamTypeDTO;
import com.example.checkscamv2.exception.CheckScamException;

import java.util.List;

public interface ScamTypeService {
    ScamTypeDTO createScamType(ScamTypeDTO scamTypesDto);
    List<ScamTypeDTO> getAllScamTypes();
    ScamTypeDTO updateScamType(Long id, ScamTypeDTO scamTypesDto) throws CheckScamException;
    boolean deleteScamType(Long id) throws CheckScamException;
}
