package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.service.UrlScamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;


@Service
@RequiredArgsConstructor
@Slf4j
public class UrlScamServiceImpl implements UrlScamService {

    @Override
    public void importUrlData(MultipartFile file) throws IOException {

    }
}
