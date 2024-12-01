package com.movie_app.recommend_service.repository;

import com.movie_app.recommend_service.document.MovieDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovieESRepository extends ElasticsearchRepository<MovieDocument, String> {
}


