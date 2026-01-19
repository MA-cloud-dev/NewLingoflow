package com.lingoflow.controller;

import com.lingoflow.dto.ApiResponse;
import com.lingoflow.entity.User;
import com.lingoflow.entity.Word;
import com.lingoflow.service.LearningService;
import com.lingoflow.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningController {

    private final WordService wordService;
    private final LearningService learningService;

    @GetMapping("/words")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWordsForLearning(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "10") int count,
            @RequestParam(required = false) String difficulty) {

        List<Word> words = wordService.getWordsForLearning(user.getId(), difficulty, count);

        Map<String, Object> data = new HashMap<>();
        data.put("words", words);
        data.put("total", words.size());

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping("/article")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateArticle(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> request) {

        @SuppressWarnings("unchecked")
        List<Long> vocabularyIds = (List<Long>) request.get("vocabularyIds");
        String difficulty = (String) request.get("difficulty");
        String length = (String) request.get("length");
        String theme = (String) request.get("theme");

        Map<String, Object> result = learningService.generateArticle(user.getId(), vocabularyIds, difficulty, length,
                theme);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/sentence")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitSentence(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Object> request) {

        Long sessionId = Long.valueOf(request.get("sessionId").toString());
        Long vocabularyId = Long.valueOf(request.get("vocabularyId").toString());
        String sentence = (String) request.get("sentence");

        Map<String, Object> result = learningService.submitSentence(user.getId(), sessionId, vocabularyId, sentence);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
