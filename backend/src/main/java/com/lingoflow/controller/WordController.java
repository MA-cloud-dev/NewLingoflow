package com.lingoflow.controller;

import com.lingoflow.dto.ApiResponse;
import com.lingoflow.entity.Word;
import com.lingoflow.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWords(
            @RequestParam(required = false) String difficulty) {

        List<Word> words;
        if (difficulty != null && !difficulty.isEmpty()) {
            words = wordService.getWordsByDifficulty(difficulty);
        } else {
            words = wordService.getAllWords();
        }

        Map<String, Object> data = new HashMap<>();
        data.put("words", words);
        data.put("total", words.size());

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Word>> getWordById(@PathVariable Long id) {
        Word word = wordService.getWordById(id);
        if (word == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(word));
    }
}
