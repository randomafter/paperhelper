package com.history.creation.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.history.creation.dto.FeedbackRequest;
import com.history.creation.entity.Feedback;
import com.history.creation.mapper.FeedbackMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FeedbackService {

    private final FeedbackMapper feedbackMapper;

    public FeedbackService(FeedbackMapper feedbackMapper) {
        this.feedbackMapper = feedbackMapper;
    }

    @Transactional
    public void submitFeedback(Long userId, String username, FeedbackRequest req) {
        if (req.getContent() == null || req.getContent().trim().isEmpty()) {
            throw new RuntimeException("反馈内容不能为空");
        }
        if (req.getContent().length() > 1000) {
            throw new RuntimeException("反馈内容不能超过1000个字符");
        }
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setUsername(username);
        feedback.setContent(req.getContent().trim());
        feedback.setStatus("pending");
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setUpdatedAt(LocalDateTime.now());
        feedbackMapper.insert(feedback);
    }

    public List<Feedback> getAllFeedback() {
        return feedbackMapper.selectList(new LambdaQueryWrapper<Feedback>().orderByDesc(Feedback::getCreatedAt));
    }

    public List<Feedback> getFeedbackByStatus(String status) {
        return feedbackMapper.selectList(new LambdaQueryWrapper<Feedback>()
                .eq(Feedback::getStatus, status)
                .orderByDesc(Feedback::getCreatedAt));
    }

    @Transactional
    public void markAsRead(Long feedbackId) {
        Feedback feedback = feedbackMapper.selectById(feedbackId);
        if (feedback != null) {
            feedback.setStatus("read");
            feedback.setUpdatedAt(LocalDateTime.now());
            feedbackMapper.updateById(feedback);
        }
    }

    @Transactional
    public void markAsResolved(Long feedbackId) {
        Feedback feedback = feedbackMapper.selectById(feedbackId);
        if (feedback != null) {
            feedback.setStatus("resolved");
            feedback.setUpdatedAt(LocalDateTime.now());
            feedbackMapper.updateById(feedback);
        }
    }
}
