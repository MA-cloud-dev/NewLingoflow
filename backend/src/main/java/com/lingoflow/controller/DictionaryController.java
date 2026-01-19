package com.lingoflow.controller;

import com.lingoflow.dto.ApiResponse;
import com.lingoflow.entity.Dictionary;
import com.lingoflow.service.DictionaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 词典控制器
 * Phase 6: 词典系统
 */
@RestController
@RequestMapping("/api/dictionaries")
@RequiredArgsConstructor
@CrossOrigin
public class DictionaryController {

    private final DictionaryService dictionaryService;

    /**
     * GET /api/dictionaries
     * 获取所有词典列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<Dictionary>>> getAllDictionaries() {
        List<Dictionary> dictionaries = dictionaryService.getAllDictionaries();
        return ResponseEntity.ok(ApiResponse.success(dictionaries));
    }

    /**
     * GET /api/dictionaries/{id}/progress
     * 获取用户在特定词典的学习进度
     */
    @GetMapping("/{id}/progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserProgress(
            @PathVariable Long id,
            @RequestAttribute Long userId) {
        Map<String, Object> progress = dictionaryService.getUserDictionaryProgress(userId, id);
        return ResponseEntity.ok(ApiResponse.success(progress));
    }
}
