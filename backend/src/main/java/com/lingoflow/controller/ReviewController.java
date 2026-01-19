package com.lingoflow.controller;

import com.lingoflow.dto.ApiResponse;
import com.lingoflow.entity.User;
import com.lingoflow.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * 获取今日待复习队列
     */
    @GetMapping("/queue")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReviewQueue(
            @AuthenticationPrincipal User user) {

        Map<String, Object> result = reviewService.getReviewQueue(user.getId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 提交熟悉度自评
     */
    @PostMapping("/rating")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitRating(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> request) {

        Long vocabularyId = Long.valueOf(request.get("vocabularyId").toString());
        String rating = (String) request.get("rating"); // "known" or "unknown"

        Map<String, Object> result = reviewService.submitRating(user.getId(), vocabularyId, rating);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 提交测试答案
     */
    @PostMapping("/answer")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitAnswer(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> request) {

        Long vocabularyId = Long.valueOf(request.get("vocabularyId").toString());
        String answer = (String) request.get("answer");
        boolean isFromErrorQueue = Boolean.TRUE.equals(request.get("isFromErrorQueue"));
        Integer responseTimeMs = request.get("responseTimeMs") != null
                ? Integer.valueOf(request.get("responseTimeMs").toString())
                : null;

        Map<String, Object> result = reviewService.submitAnswer(
                user.getId(), vocabularyId, answer, isFromErrorQueue, responseTimeMs);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
