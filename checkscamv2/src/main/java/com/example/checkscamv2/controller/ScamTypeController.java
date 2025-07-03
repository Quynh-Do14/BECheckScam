package com.example.checkscamv2.controller;

import com.example.checkscamv2.service.ScamTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/scam-types")
@RequiredArgsConstructor
public class ScamTypeController {

    private final ScamTypeService scamTypeService;
}
