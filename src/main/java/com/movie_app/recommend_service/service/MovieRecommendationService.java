package com.movie_app.recommend_service.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MoreLikeThisQuery;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.json.JsonData;
import com.movie_app.recommend_service.document.MovieDocument;
import com.movie_app.recommend_service.document.UserPreferenceDocument;
import com.movie_app.recommend_service.repository.MovieESRepository;
import com.movie_app.recommend_service.repository.UserPreferenceESRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class MovieRecommendationService {

    private final ElasticsearchClient esClient;

    private final MovieESRepository movieRepository;

    private final UserPreferenceESRepository preferenceRepository;

    public List<MovieDocument> getPersonalizedRecommendations(Long userId, int size) {
        try {
            // 1. Lấy thể loại yêu thích của user
            Map<String, Double> genreScores = calculateUserGenrePreferences(userId);

            // 2. Tạo bool query
            BoolQuery.Builder boolQuery = new BoolQuery.Builder();

            // Thêm điều kiện cho từng thể loại với boost tương ứng
            genreScores.forEach((genre, score) -> {
                boolQuery.should(s -> s
                        .match(m -> m
                                .field("genres")
                                .query(genre)
                                .boost((float) score.doubleValue())
                        )
                );
            });

            // Thêm điều kiện rating >= 4.0
            boolQuery.must(m -> m
                    .range(r -> r
                            .field("rating")
                            .gte(JsonData.of(4.0))
                    )
            );

            // Exclude phim đã xem
            List<String> watchedMovies = getWatchedMovieIds(userId);
            if (!watchedMovies.isEmpty()) {
                boolQuery.mustNot(m -> m
                        .ids(i -> i
                                .values(watchedMovies)
                        )
                );
            }

            // Thực hiện search
            SearchResponse<MovieDocument> response = esClient.search(s -> s
                            .index("movies")
                            .query(boolQuery.build()._toQuery())
                            .sort(sort -> sort
                                    .field(f -> f.field("rating").order(SortOrder.Desc))
                            )
                            .sort(sort -> sort
                                    .field(f -> f.field("viewCount").order(SortOrder.Desc))
                            )
                            .size(size),
                    MovieDocument.class
            );

            // Parse kết quả
            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting personalized recommendations", e);
            return Collections.emptyList();
        }
    }

    public List<MovieDocument> getSimilarMovies(String movieId, int size) {
        try {
            // Tạo more-like-this query
            MoreLikeThisQuery.Builder moreLikeThis = new MoreLikeThisQuery.Builder();
            moreLikeThis
                    .fields("title", "description", "genres", "actors", "directors")
                    .like(l -> l
                            .document(d -> d
                                    .index("movies")
                                    .id(movieId)
                            )
                    )
                    .minTermFreq(1)
                    .maxQueryTerms(12);

            // Thực hiện search
            SearchResponse<MovieDocument> response = esClient.search(s -> s
                            .index("movies")
                            .query(q -> q
                                    .moreLikeThis(moreLikeThis.build())
                            )
                            .size(size),
                    MovieDocument.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting similar movies", e);
            return Collections.emptyList();
        }
    }

    private Map<String, Double> calculateUserGenrePreferences(Long userId) {
        List<UserPreferenceDocument> userPreferences = preferenceRepository.findByUserId(userId);

        Map<String, Double> genreScores = new HashMap<>();
        Map<String, Integer> genreCounts = new HashMap<>();

        for (UserPreferenceDocument pref : userPreferences) {
            MovieDocument movie = movieRepository.findById(pref.getMovieId()).orElse(null);
            if (movie != null) {
                for (String genre : movie.getGenres()) {
                    genreScores.merge(genre, pref.getRating(), Double::sum);
                    genreCounts.merge(genre, 1, Integer::sum);
                }
            }
        }

        // Tính trung bình cho mỗi thể loại
        genreScores.forEach((genre, score) ->
                genreScores.put(genre, score / genreCounts.get(genre)));

        return genreScores;
    }

    private List<String> getWatchedMovieIds(Long userId) {
        return preferenceRepository.findByUserId(userId).stream()
                .map(UserPreferenceDocument::getMovieId)
                .collect(Collectors.toList());
    }

    // Thêm method để đề xuất phim theo xu hướng
    public List<MovieDocument> getTrendingMovies(int size) {
        try {
            // Tạo bool query
            SearchResponse<MovieDocument> response = esClient.search(s -> s
                            .index("movies")
                            .query(q -> q
                                    .bool(b -> b
                                            .must(m -> m
                                                    .range(r -> r
                                                            .field("releaseDate")
                                                            .gte(JsonData.fromJson("now-30d")) // Lấy phim trong 30 ngày gần đây
                                                            .format("strict_date_optional_time")
                                                    )
                                            )
                                            .must(m -> m
                                                    .range(r -> r
                                                            .field("rating")
                                                            .gte(JsonData.of(0))
                                                    )
                                            )
                                    )
                            )
                            .sort(sort -> sort
                                    .field(f -> f
                                            .field("viewCount")
                                            .order(SortOrder.Desc)
                                    )
                            )
                            .sort(sort -> sort
                                    .field(f -> f
                                            .field("rating")
                                            .order(SortOrder.Desc)
                                    )
                            )
                            .sort(sort -> sort
                                    .field(f -> f
                                            .field("releaseDate")
                                            .order(SortOrder.Desc)
                                    )
                            )
                            .size(size),
                    MovieDocument.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error getting trending movies: ", e);
            return Collections.emptyList();
        }
    }

    // Thêm method để đề xuất phim theo mùa/sự kiện
    public List<MovieDocument> getSeasonalRecommendations(String season, int size) {
        try {
            // Tạo bool query kết hợp mùa và rating
            SearchResponse<MovieDocument> response = esClient.search(s -> s
                            .index("movies")
                            .query(q -> q
                                    .bool(b -> b
                                            .must(m -> m
                                                    .match(mt -> mt
                                                            .field("seasonTags")
                                                            .query(season)
                                                    )
                                            )
                                            .must(m -> m
                                                    .range(r -> r
                                                            .field("rating")
                                                            .gte(JsonData.of(4.0))
                                                    )
                                            )
                                    )
                            )
                            .sort(sort -> sort
                                    .field(f -> f.field("rating").order(SortOrder.Desc))
                            )
                            .size(size),
                    MovieDocument.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting seasonal recommendations", e);
            return Collections.emptyList();
        }
    }
}
