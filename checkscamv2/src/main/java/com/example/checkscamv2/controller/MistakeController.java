package com.example.checkscamv2.controller;

import com.example.checkscamv2.component.LocalizationUtils;
import com.example.checkscamv2.constant.MessageKeys;
import com.example.checkscamv2.dto.request.MistakeRequest;
import com.example.checkscamv2.dto.request.MistakeStatusUpdateRequest;
import com.example.checkscamv2.dto.response.MistakeResponse;
import com.example.checkscamv2.dto.response.ResponseObject;
import com.example.checkscamv2.entity.Attachment;
import com.example.checkscamv2.exception.CheckScamException;
import com.example.checkscamv2.exception.DataNotFoundException;
import com.example.checkscamv2.exception.FileUploadValidationException;
import com.example.checkscamv2.exception.InvalidParamException; // Thêm import này nếu cần
import com.example.checkscamv2.service.MistakeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/mistakes")
@RequiredArgsConstructor
public class MistakeController {

    private final MistakeService mistakeService;
    private final LocalizationUtils localizationUtils;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<MistakeResponse> submitMistake(
            @Valid @RequestBody MistakeRequest request) throws CheckScamException {
        MistakeResponse response = mistakeService.submitMistake(request, null); // Giả định upload file riêng
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<Page<MistakeResponse>> getAllMistakes(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<MistakeResponse> mistakes = mistakeService.getAllMistakes(status, pageable);
        return ResponseEntity.ok(mistakes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MistakeResponse> getMistakeDetails(@PathVariable Long id) {
        MistakeResponse mistake = mistakeService.getMistakeDetails(id);
        return ResponseEntity.ok(mistake);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<MistakeResponse> updateMistakeStatus(
            @PathVariable Long id,
            @Valid @RequestBody MistakeStatusUpdateRequest request) {
        MistakeResponse updatedMistake = mistakeService.updateMistakeStatus(id, request);
        return ResponseEntity.ok(updatedMistake);
    }

    @PatchMapping("/approve/{id}")
    public ResponseEntity<?> approveMistake(@PathVariable Long id) {
        try {
            MistakeResponse updatedMistake = mistakeService.approveMistake(id);
            return ResponseEntity.ok(updatedMistake);
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi xác nhận khiếu nại: " + e.getMessage());
        }
    }

    @PatchMapping("/reject/{id}")
    public ResponseEntity<?> rejectMistake(@PathVariable Long id) {
        try {
            MistakeResponse updatedMistake = mistakeService.rejectMistake(id);
            return ResponseEntity.ok(updatedMistake);
        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Lỗi: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi từ chối khiếu nại: " + e.getMessage());
        }
    }

    @PostMapping(value = "/uploads/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResponseObject> uploadAttachments(
            @PathVariable("id") Long mistakeId, // Đổi tên biến cho rõ ràng là mistakeId
            @RequestParam("files")List<MultipartFile> files) {
        try {
            List<Attachment> attachments = mistakeService.uploadFile(mistakeId, files); // Gọi service với mistakeId
            if (attachments.isEmpty() && (files == null || files.stream().allMatch(f -> f == null || f.isEmpty() || f.getSize() == 0))) {
                return ResponseEntity.ok().body(ResponseObject.builder()
                        .message(localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_ATTACHMENTS_NO_VALID_FILES))
                        .status(HttpStatus.OK)
                        .data(attachments)
                        .build());
            }

            return ResponseEntity.ok().body(ResponseObject.builder()
                    .message(localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_ATTACHMENTS_SUCCESSFULLY))
                    .status(HttpStatus.OK)
                    .data(attachments)
                    .build());

        } catch (DataNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ResponseObject.builder()
                    .message(localizationUtils.getLocalizedMessage(e.getMessage()))
                    .status(HttpStatus.NOT_FOUND)
                    .build());
        } catch (InvalidParamException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ResponseObject.builder()
                    .message(localizationUtils.getLocalizedMessage(e.getMessage()))
                    .status(HttpStatus.BAD_REQUEST)
                    .build());
        } catch (FileUploadValidationException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(ResponseObject.builder()
                    .message(localizationUtils.getLocalizedMessage(e.getMessageKey(), e.getArgs()))
                    .status(e.getHttpStatus())
                    .build());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseObject.builder()
                    .message(localizationUtils.getLocalizedMessage(MessageKeys.UPLOAD_ATTACHMENTS_FILE_STORAGE_ERROR, e.getMessage()))
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ResponseObject.builder()
                    .message(localizationUtils.getLocalizedMessage(MessageKeys.ERROR_OCCURRED_DEFAULT, e.getMessage()))
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build());
        }
    }
}