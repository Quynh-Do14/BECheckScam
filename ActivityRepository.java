// ActivityRepository.java
package com.checkscam.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.checkscam.model.Activity;

import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {
    
    Page<Activity> findByActionType(Activity.ActionType actionType, Pageable pageable);
    
    long countByActionType(Activity.ActionType actionType);
    
    @Query("SELECT COUNT(a) FROM Activity a WHERE DATE(a.createdAt) = CURRENT_DATE")
    long countTodayActivities();
    
    @Query("SELECT a FROM Activity a WHERE " +
           "a.metadata LIKE '%\"risk_level\":\"high\"%' OR " +
           "a.metadata LIKE '%\"result\":\"scam\"%' OR " +
           "a.actionType = 'UPLOAD' " +
           "ORDER BY a.createdAt DESC")
    List<Activity> findDangerousActivities(Pageable pageable);
    
    List<Activity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}