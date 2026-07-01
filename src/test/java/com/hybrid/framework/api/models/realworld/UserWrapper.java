package com.hybrid.framework.api.models.realworld;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top-level envelope wrapping the {@code user} field in RealWorld API responses.
 * Used exclusively for <b>response deserialization</b>.
 *
 * <pre>
 * {
 *   "user": { "email": "...", "token": "...", "username": "..." }
 * }
 * </pre>
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserWrapper {

    private UserPayload user;
}
