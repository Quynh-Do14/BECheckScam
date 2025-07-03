package com.example.checkscamv2.service;

import com.example.checkscamv2.dto.request.MistakeRequest;
import com.example.checkscamv2.dto.request.MistakeStatusUpdateRequest;
import com.example.checkscamv2.dto.response.MistakeResponse;
import com.example.checkscamv2.entity.Attachment;
import com.example.checkscamv2.exception.CheckScamException;
import com.example.checkscamv2.exception.DataNotFoundException; // <-- Thêm import này
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MistakeService {
    MistakeResponse submitMistake(MistakeRequest request, List<MultipartFile> attachments) throws CheckScamException;
    Page<MistakeResponse> getAllMistakes(String status, Pageable pageable);
    MistakeResponse getMistakeDetails(Long mistakeId);
    MistakeResponse updateMistakeStatus(Long mistakeId, MistakeStatusUpdateRequest request);

    @Transactional
    List<Attachment> uploadFile(Long reportId, List<MultipartFile> files) throws Exception;

    // --- THÊM CÁC PHƯƠNG THỨC MỚI DƯỚI ĐÂY ---

    /**
     * Duyệt một khiếu nại (Mistake) dựa trên ID.
     * Cập nhật trạng thái của khiếu nại thành PROCESSED.
     *
     * @param mistakeId ID của khiếu nại cần duyệt.
     * @return MistakeResponse của khiếu nại đã được cập nhật.
     * @throws DataNotFoundException nếu không tìm thấy khiếu nại.
     */
    MistakeResponse approveMistake(Long mistakeId) throws DataNotFoundException;

    /**
     * Từ chối một khiếu nại (Mistake) dựa trên ID.
     * Cập nhật trạng thái của khiếu nại thành REJECTED.
     *
     * @param mistakeId ID của khiếu nại cần từ chối.
     * @return MistakeResponse của khiếu nại đã được cập nhật.
     * @throws DataNotFoundException nếu không tìm thấy khiếu nại.
     */
    MistakeResponse rejectMistake(Long mistakeId) throws DataNotFoundException;
}