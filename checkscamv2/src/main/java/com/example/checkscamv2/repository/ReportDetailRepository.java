package com.example.checkscamv2.repository;

import com.example.checkscamv2.entity.ReportDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportDetailRepository extends JpaRepository<ReportDetail, Long> {
    List<ReportDetail> findByInfoAndType(String info, Integer type);
    List<ReportDetail> findByInfoAndTypeAndStatus(String info, Integer type, Integer status);
    List<ReportDetail> findByReportId(Long reportId);
}
