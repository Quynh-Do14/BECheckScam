package com.example.checkscamv2.service.Impl;

import com.example.checkscamv2.repository.ForumPostRepository;
import com.example.checkscamv2.service.ViewCountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViewCountServiceImpl implements ViewCountService {

    private final ForumPostRepository forumPostRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void incrementPostViewCount(Long postId) {
        try {
            forumPostRepository.incrementViewCount(postId);
            log.debug("Incremented view count for post: {}", postId);
        } catch (Exception e) {
            log.warn("Failed to increment view count for post: {}, error: {}", postId, e.getMessage());
            // Don't throw exception to avoid breaking the main flow
        }
    }
}
