package com.movie_app.recommend_service.document;

import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.sql.Date;
import java.util.List;

@Document(indexName = "movies")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MovieDocument {
    @Id
    private String id;

    private String title;
    private List<String> genres;
    private String description;
    private Date releaseDate;
    private List<String> actors;
    private List<String> directors;
    private Double rating;
    private Long viewCount;

    @Field(type = FieldType.Dense_Vector, dims = 100)
    private float[] movieVector; // vector đặc trưng của phim
}
