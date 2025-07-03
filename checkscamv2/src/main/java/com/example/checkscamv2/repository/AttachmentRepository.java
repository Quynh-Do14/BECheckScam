package com.example.checkscamv2.repository;

import com.example.checkscamv2.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByReportIdIn(List<Long> reportIds);
}