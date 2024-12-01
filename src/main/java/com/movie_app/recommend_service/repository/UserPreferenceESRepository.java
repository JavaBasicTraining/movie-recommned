package com.movie_app.recommend_service.repository;

import com.movie_app.recommend_service.document.UserPreferenceDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPreferenceESRepository extends ElasticsearchRepository<UserPreferenceDocument, String> {
    List<UserPreferenceDocument> findByUserId(Long userId);
}