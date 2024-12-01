-- MySQL Schema
CREATE TABLE user_preferences
(
    user_id   BIGINT,
    movie_id  BIGINT,
    rating    FLOAT,
    timestamp TIMESTAMP,
    PRIMARY KEY (user_id, movie_id)
);

CREATE TABLE movie_features
(
    movie_id     BIGINT PRIMARY KEY,
    title        VARCHAR(255),
    genres       TEXT, -- Lưu genres dưới dạng chuỗi JSON: ["Action", "Drama", "Thriller"]
    release_date DATE,
    features     JSON, -- Lưu vector đặc trưng của phim dưới dạng JSON
    INDEX idx_movie_id (movie_id)
);

-- Bảng bổ sung cho genres (normalized design)
CREATE TABLE movie_genres
(
    movie_genre_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    movie_id       BIGINT,
    genre          VARCHAR(50),
    FOREIGN KEY (movie_id) REFERENCES movie_features (movie_id),
    INDEX idx_movie_id (movie_id)
);