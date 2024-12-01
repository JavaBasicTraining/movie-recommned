package com.movie_app.recommend_service.document;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;

import java.sql.Date;

@Document(indexName = "user_preferences")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPreferenceDocument {
    @Id
    private String id;

    private Long userId;
    private String movieId;
    private Double rating;
    private Date watchedAt;
    private Double watchDuration;
    private Boolean completed;
}
