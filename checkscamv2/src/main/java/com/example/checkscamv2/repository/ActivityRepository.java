package com.example.checkscamv2.repository;

import com.example.checkscamv2.constant.ActivityType;
import com.example.checkscamv2.entity.Activity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    
    Page<Activity> findByActionTypeOrderByCreatedAtDesc(ActivityType actionType, Pageable pageable);
    
    Page<Activity> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    @Query("SELECT a FROM Activity a WHERE a.actionType IN ('UPLOAD', 'REPORT', 'JOIN') ORDER BY a.createdAt DESC")
    Page<Activity> findValidActivitiesOrderByCreatedAtDesc(Pageable pageable);
    
    long countByActionType(ActivityType actionType);
    
    @Query("SELECT COUNT(a) FROM Activity a WHERE DATE(a.createdAt) = CURRENT_DATE")
    long countTodayActivities();
    
    @Query("SELECT a FROM Activity a WHERE " +
           "a.metadata LIKE '%\"category\":\"warning\"%' OR " +
           "a.actionType = 'REPORT' " +
           "ORDER BY a.createdAt DESC")
    List<Activity> findDangerousActivities(Pageable pageable);
    
    List<Activity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    
    @Query("SELECT a FROM Activity a WHERE a.userId = :userId AND a.actionType = :actionType ORDER BY a.createdAt DESC")
    List<Activity> findByUserIdAndActionType(@Param("userId") Long userId, @Param("actionType") ActivityType actionType, Pageable pageable);
}