package com.hybrid.framework.api.models.realworld;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents the {@code article} object in RealWorld API responses (create, get, update article).
 * Used exclusively for <b>response deserialization</b>; request bodies are loaded from JSON payload files.
 * 
 * @Data (Lombok)
Generates getters, setters, toString(), equals(), and hashCode() for all fields. Jackson needs getters/setters (or public fields) to map JSON properties like "slug" and "title" onto Java fields.

@NoArgsConstructor (Lombok)
Adds a no-arg constructor: ArticlePayload(). Jackson creates an empty object first, then sets fields from JSON. Without it, only a constructor with parameters would exist and deserialization can fail.

@JsonIgnoreProperties(ignoreUnknown = true) (Jackson)
If the API returns extra fields not declared on the class (e.g. favorited, favoritesCount), Jackson ignores them instead of throwing an error. Useful when the API adds fields or your model only maps a subset.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArticlePayload {

    private String slug;
    private String title;
    private String description;
    private String body;
    private List<String> tagList;
    private String createdAt;
    private String updatedAt;
    private boolean favorited;
    private int favoritesCount;
    private AuthorPayload author;
}
