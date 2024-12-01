package com.movie_app.recommend_service.controller;

import com.movie_app.recommend_service.document.MovieDocument;
import com.movie_app.recommend_service.service.MovieRecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class MovieRecommendationController {

    private final MovieRecommendationService recommendationService;

    @GetMapping("/personalized/{userId}")
    public ResponseEntity<List<MovieDocument>> getPersonalizedRecommendations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(recommendationService.getPersonalizedRecommendations(userId, size));
    }

    @GetMapping("/similar/{movieId}")
    public ResponseEntity<List<MovieDocument>> getSimilarMovies(
            @PathVariable String movieId,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(recommendationService.getSimilarMovies(movieId, size));
    }
}
