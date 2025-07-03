package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.constant.ErrorCodeEnum;
import com.example.checkscamv2.dto.ScamTypeDTO;
import com.example.checkscamv2.exception.CheckScamException;
import com.example.checkscamv2.repository.ScamTypeRepository;
import com.example.checkscamv2.service.ScamTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ScamTypeServiceImpl implements ScamTypeService {
    private final ScamTypeRepository repository;


    @Override
    public ScamTypeDTO createScamType(ScamTypeDTO scamTypesDto) {
        return null;
    }

    @Override
    public List<ScamTypeDTO> getAllScamTypes() {
        return List.of();
    }

    @Override
    public ScamTypeDTO updateScamType(Long id, ScamTypeDTO scamTypesDto) throws CheckScamException {
        return null;
    }

    @Override
    public boolean deleteScamType(Long id) throws CheckScamException {
        return repository.findById(id)
                .map(entity -> {
                    repository.delete(entity);
                    return true;
                })
                .orElseThrow(() -> new CheckScamException(ErrorCodeEnum.NOT_FOUND));
    }
}
