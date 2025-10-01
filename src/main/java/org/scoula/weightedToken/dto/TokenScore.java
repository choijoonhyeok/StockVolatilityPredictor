package org.scoula.weightedToken.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TokenScore {
    long tokenId;
    String token;
    long articleId;
    double tfidf;
    double normalized;

    TokenScore(long tokenId, String token, long articleId, double tfidf) {
        this.tokenId = tokenId;
        this.token = token;
        this.articleId = articleId;
        this.tfidf = tfidf;
    }
}