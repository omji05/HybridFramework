package com.hybrid.framework.api.models.realworld;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Top-level envelope wrapping the {@code article} field in RealWorld API responses.
 * Used exclusively for <b>response deserialization</b>.
 *

 * <pre>
 * {
 *   "article": { "slug": "...", "title": "...", "body": "..." }
 * }
 * </pre>
 * @Data:: Generates boilerplate: getters/setters, toString(), equals(), and hashCode() for the article field
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ArticleWrapper {

    private ArticlePayload article;
}
