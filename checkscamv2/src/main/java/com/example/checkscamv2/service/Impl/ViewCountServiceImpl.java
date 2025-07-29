package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.repository.ForumPostRepository;
import com.example.checkscamv2.service.ViewCountService;
import com.example.checkscamv2.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViewCountServiceImpl implements ViewCountService {

    private final ForumPostRepository forumPostRepository;
    
    // Cache to prevent duplicate views from same user within 5 minutes
    private final ConcurrentMap<String, Long> recentViews = new ConcurrentHashMap<>();
    private static final long VIEW_COOLDOWN_MS = 5 * 60 * 1000; // 5 minutes

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementPostViewCount(Long postId) {
        try {
            // Get current user (can be null for anonymous users)
            String userIdentifier = SecurityUtil.getCurrentUserLogin()
                    .orElse("anonymous_" + Thread.currentThread().getId());
            
            String viewKey = userIdentifier + "_" + postId;
            long currentTime = System.currentTimeMillis();
            
            // Check if user already viewed this post recently
            Long lastViewTime = recentViews.get(viewKey);
            if (lastViewTime != null && (currentTime - lastViewTime) < VIEW_COOLDOWN_MS) {
                log.debug("Skipping view count increment for post {} - user {} viewed recently", 
                         postId, userIdentifier);
                return;
            }
            
            // Increment view count atomically
            forumPostRepository.incrementViewCount(postId);
            
            // Update cache with current time
            recentViews.put(viewKey, currentTime);
            
            // Clean up old entries (simple cleanup every 100 increments)
            if (recentViews.size() % 100 == 0) {
                cleanupOldViews(currentTime);
            }
            
            log.debug("Incremented view count for post: {} by user: {}", postId, userIdentifier);
        } catch (Exception e) {
            log.warn("Failed to increment view count for post: {}, error: {}", postId, e.getMessage());
            // Don't throw exception to avoid breaking the main flow
        }
    }
    
    private void cleanupOldViews(long currentTime) {
        recentViews.entrySet().removeIf(entry -> 
            (currentTime - entry.getValue()) > VIEW_COOLDOWN_MS);
        log.debug("Cleaned up old view cache entries, remaining: {}", recentViews.size());
    }
}
