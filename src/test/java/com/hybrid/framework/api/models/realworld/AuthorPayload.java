package com.hybrid.framework.api.models.realworld;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the {@code author} object nested inside article responses.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorPayload {

    private String username;
    private String bio;
    private String image;
    private boolean following;
}
