package com.lingoflow.controller;

import com.lingoflow.dto.ApiResponse;
import com.lingoflow.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 获取用户学习统计概览
     */
    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getOverview(
            @AuthenticationPrincipal User user) {

        Long userId = user.getId();

        // 今日学习词数
        Integer todayLearned = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vocabulary WHERE user_id = ? AND DATE(created_at) = CURDATE()",
                Integer.class, userId);

        // 待复习词数
        Integer pendingReview = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vocabulary WHERE user_id = ? AND next_review_date <= NOW()",
                Integer.class, userId);

        // 总词汇量
        Integer totalWords = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM vocabulary WHERE user_id = ?",
                Integer.class, userId);

        // 连续学习天数 (简化计算)
        Integer streakDays = calculateStreak(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("todayLearned", todayLearned != null ? todayLearned : 0);
        result.put("pendingReview", pendingReview != null ? pendingReview : 0);
        result.put("totalWords", totalWords != null ? totalWords : 0);
        result.put("streakDays", streakDays);
        result.put("dailyGoal", 20);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 获取周学习数据 (过去7天)
     * 返回每天的学习数量和复习数量
     */
    @GetMapping("/weekly")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getWeeklyStats(
            @AuthenticationPrincipal User user) {

        Long userId = user.getId();
        List<Map<String, Object>> weeklyData = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);

            // 学习数量：当天新增的单词
            Integer learnCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vocabulary WHERE user_id = ? AND DATE(created_at) = ?",
                    Integer.class, userId, date);

            // 复习数量：当天的复习记录数
            Integer reviewCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM review_records WHERE user_id = ? AND DATE(reviewed_at) = ?",
                    Integer.class, userId, date);

            Map<String, Object> dayData = new HashMap<>();
            dayData.put("date", date.format(formatter));
            dayData.put("count", learnCount != null ? learnCount : 0);
            dayData.put("reviewCount", reviewCount != null ? reviewCount : 0);
            weeklyData.add(dayData);
        }

        return ResponseEntity.ok(ApiResponse.success(weeklyData));
    }

    private Integer calculateStreak(Long userId) {
        // 简化的连续学习天数计算
        Integer streak = 0;
        LocalDate date = LocalDate.now();

        for (int i = 0; i < 365; i++) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM vocabulary WHERE user_id = ? AND DATE(created_at) = ?",
                    Integer.class, userId, date.minusDays(i));

            if (count != null && count > 0) {
                streak++;
            } else if (i > 0) {
                break;
            }
        }

        return streak;
    }
}
