package com.hybrid.framework.api.models.realworld;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the {@code user} object in RealWorld API responses (login, register, get-current-user).
 * Used exclusively for <b>response deserialization</b>; request bodies are loaded from JSON payload files.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPayload {

    private String email;
    private String token;
    private String username;
    private String bio;
    private String image;
}
