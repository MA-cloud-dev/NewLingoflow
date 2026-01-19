package com.lingoflow.controller;

import com.lingoflow.dto.ApiResponse;
import com.lingoflow.entity.User;
import com.lingoflow.entity.Vocabulary;
import com.lingoflow.service.VocabularyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vocabulary")
@RequiredArgsConstructor
public class VocabularyController {

    private final VocabularyService vocabularyService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserVocabulary(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        List<Vocabulary> vocabulary = vocabularyService.getUserVocabulary(user.getId(), status, page, pageSize);
        int total = vocabularyService.getUserVocabularyCount(user.getId(), status);

        Map<String, Object> data = new HashMap<>();
        data.put("vocabulary", vocabulary);
        data.put("total", total);
        data.put("page", page);
        data.put("pageSize", pageSize);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> addToVocabulary(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, Long> request) {

        Long wordId = request.get("wordId");
        Map<String, Object> result = vocabularyService.addToVocabulary(user.getId(), wordId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeFromVocabulary(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {

        vocabularyService.removeFromVocabulary(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
