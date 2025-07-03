package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.constant.ErrorCodeEnum;
import com.example.checkscamv2.dto.request.MistakeDetailRequest;
import com.example.checkscamv2.dto.request.MistakeStatusUpdateRequest;
import com.example.checkscamv2.dto.response.MistakeDetailResponse;
import com.example.checkscamv2.entity.Mistake;
import com.example.checkscamv2.entity.MistakeDetail;
import com.example.checkscamv2.entity.Attachment;
import com.example.checkscamv2.exception.CheckScamException;
import com.example.checkscamv2.exception.FileUploadValidationException;
import com.example.checkscamv2.exception.InvalidCaptchaException;
import com.example.checkscamv2.exception.DataNotFoundException;
import com.example.checkscamv2.constant.MistakeStatus; // Đây là enum của bạn
import com.example.checkscamv2.dto.request.MistakeRequest;
import com.example.checkscamv2.dto.response.MistakeResponse;
import com.example.checkscamv2.repository.AttachmentRepository;
import com.example.checkscamv2.repository.MistakeDetailRepository;
import com.example.checkscamv2.repository.MistakeRepository;
import com.example.checkscamv2.service.CaptchaService;
import com.example.checkscamv2.service.MistakeService;
import com.example.checkscamv2.component.FileUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MistakeServiceImpl implements MistakeService {

    private final MistakeRepository mistakeRepository;
    private final MistakeDetailRepository mistakeDetailRepository;
    private final AttachmentRepository attachmentRepository;
    private final CaptchaService captchaService;
    private final FileUtils fileUtils;

    @Override
    @Transactional
    public MistakeResponse submitMistake(MistakeRequest request, List<MultipartFile> attachments) throws CheckScamException {
        if (!captchaService.verify(request.getCaptchaToken())) {
            throw new InvalidCaptchaException("Captcha verification failed.");
        }

        Mistake mistake = Mistake.builder()
                .emailAuthorMistake(request.getEmailAuthorMistake())
                .complaintReason(request.getComplaintReason())
                .status(MistakeStatus.PENDING)
                .dateMistake(LocalDateTime.now())
                .mistakeDetails(new ArrayList<>())
                .attachments(new ArrayList<>())
                .build();

        Mistake savedMistake = mistakeRepository.save(mistake);

        if (request.getMistakeDetails() != null && !request.getMistakeDetails().isEmpty()) {
            for (MistakeDetailRequest detailRequest : request.getMistakeDetails()) {
                MistakeDetail mistakeDetail = MistakeDetail.builder()
                        .mistake(savedMistake)
                        .type(detailRequest.getType())
                        .info(detailRequest.getInfo())
                        .build();
                mistakeDetailRepository.save(mistakeDetail);
                savedMistake.getMistakeDetails().add(mistakeDetail);
            }
        }

        if (attachments != null && !attachments.isEmpty()) {
            List<MultipartFile> validFiles = attachments.stream()
                    .filter(f -> f != null && !f.isEmpty() && f.getSize() > 0)
                    .toList();

            for (MultipartFile file : validFiles) {
                try {
                    String storedName = fileUtils.storeFile(file);
                    Attachment attachment = Attachment.builder()
                            .mistake(savedMistake)
                            .url(storedName)
                            .build();
                    attachmentRepository.save(attachment);
                    savedMistake.getAttachments().add(attachment);
                } catch (Exception e) {
                    System.err.println("Failed to store attachment for mistake: " + e.getMessage());
                }
            }
        }

        return mapToMistakeResponse(savedMistake);
    }

    @Override
    public Page<MistakeResponse> getAllMistakes(String status, Pageable pageable) {
        Page<Mistake> mistakesPage;
        if (status != null && !status.isEmpty()) {
            try {
                MistakeStatus mistakeStatus = MistakeStatus.valueOf(status.toUpperCase());
                mistakesPage = mistakeRepository.findByStatus(mistakeStatus, pageable);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid mistake/complaint status: " + status);
            }
        } else {
            mistakesPage = mistakeRepository.findAll(pageable);
        }
        return mistakesPage.map(this::mapToMistakeResponse);
    }

    @Override
    public MistakeResponse getMistakeDetails(Long mistakeId) {
        Mistake mistake = mistakeRepository.findById(mistakeId)
                .orElseThrow(() -> new DataNotFoundException("Mistake/Complaint not found with ID: " + mistakeId));

        return mapToMistakeResponse(mistake);
    }

    @Override
    @Transactional
    public MistakeResponse updateMistakeStatus(Long mistakeId, MistakeStatusUpdateRequest request) {
        Mistake mistake = mistakeRepository.findById(mistakeId)
                .orElseThrow(() -> new DataNotFoundException("Mistake/Complaint not found with ID: " + mistakeId));

        mistake.setStatus(request.getStatus());

        Mistake updatedMistake = mistakeRepository.save(mistake);

        // TODO: Gửi email thông báo cho người khiếu nại về trạng thái mới
        return mapToMistakeResponse(updatedMistake);
    }

    // --- CẬP NHẬT LOGIC APPROVE MISTAKE DƯỚI ĐÂY ---
    @Override
    @Transactional
    public MistakeResponse approveMistake(Long mistakeId) throws DataNotFoundException {
        Mistake mistake = mistakeRepository.findById(mistakeId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy khiếu nại với id: " + mistakeId));

        // Cập nhật trạng thái sang APPROVED bằng enum
        mistake.setStatus(MistakeStatus.APPROVED);
        Mistake updatedMistake = mistakeRepository.save(mistake);
        return mapToMistakeResponse(updatedMistake);
    }

    // --- LOGIC REJECT MISTAKE KHÔNG ĐỔI ---
    @Override
    @Transactional
    public MistakeResponse rejectMistake(Long mistakeId) throws DataNotFoundException {
        Mistake mistake = mistakeRepository.findById(mistakeId)
                .orElseThrow(() -> new DataNotFoundException("Không tìm thấy khiếu nại với id: " + mistakeId));

        // Cập nhật trạng thái sang REJECTED bằng enum
        mistake.setStatus(MistakeStatus.REJECTED);
        Mistake updatedMistake = mistakeRepository.save(mistake);
        return mapToMistakeResponse(updatedMistake);
    }
    // --- KẾT THÚC CÁC PHẦN CẬP NHẬT ---


    private MistakeResponse mapToMistakeResponse(Mistake mistake) {
        MistakeResponse response = new MistakeResponse();
        response.setId(mistake.getId());
        response.setEmailAuthorMistake(mistake.getEmailAuthorMistake());
        response.setComplaintReason(mistake.getComplaintReason());
        response.setStatus(mistake.getStatus());
        response.setDateMistake(mistake.getDateMistake());

        if (mistake.getMistakeDetails() != null) {
            response.setMistakeDetails(mistake.getMistakeDetails().stream()
                    .map(this::mapToMistakeDetailResponse)
                    .collect(Collectors.toList()));
        }

        if (mistake.getAttachments() != null) {
            response.setAttachmentUrls(mistake.getAttachments().stream()
                    .map(Attachment::getUrl)
                    .collect(Collectors.toList()));
        }

        return response;
    }

    private MistakeDetailResponse mapToMistakeDetailResponse(MistakeDetail mistakeDetail) {
        return new MistakeDetailResponse(mistakeDetail.getId(), mistakeDetail.getType(), mistakeDetail.getInfo());
    }

    @Transactional
    @Override
    public List<Attachment> uploadFile(Long mistakeId, List<MultipartFile> files) throws Exception {
        Mistake mistake = mistakeRepository.findById(mistakeId)
                .orElseThrow(() -> new CheckScamException(ErrorCodeEnum.NOT_FOUND));

        List<MultipartFile> validFiles =
                (files == null ? List.<MultipartFile>of() : files).stream()
                        .filter(f -> f != null && !f.isEmpty() && f.getSize() > 0)
                        .toList();

        if (validFiles.isEmpty()) {
            return List.of();
        }

        List<Attachment> saved = new ArrayList<>();

        for (MultipartFile file : validFiles) {
            if (file.getSize() > 10 * 1024 * 1024) {
                throw new FileUploadValidationException(
                        "Kích thước file vượt quá 10MB: " + file.getOriginalFilename(),
                        HttpStatus.PAYLOAD_TOO_LARGE);
            }
            if (file.getContentType() == null
                    || !file.getContentType().startsWith("image/")) {
                throw new FileUploadValidationException(
                        "File phải là hình ảnh: " + file.getOriginalFilename(),
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE);
            }

            String storedName = fileUtils.storeFile(file);

            Attachment toSave = Attachment.builder()
                    .mistake(mistake)
                    .url(storedName)
                    .build();

            saved.add(attachmentRepository.save(toSave));
        }
        return saved;
    }
}