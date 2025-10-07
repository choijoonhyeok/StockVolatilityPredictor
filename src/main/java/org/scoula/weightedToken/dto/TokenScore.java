package org.scoula.weightedToken.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor  // ✅ 기본 생성자 추가
public class TokenScore {
    private long tokenId;
    private String token;
    private long articleId;
    private double tfidf;              // TF-IDF
    private double normalized;         // 표준화된 TF-IDF
    private double volatility;         // 매칭된 변동률
    private double normalizedVol;      // 표준화된 변동률
    private double finalWeightedScore; // TF-IDF X 변동률
    private LocalDateTime publishTime; // 기사 발행일

    // 선택적으로 생성자 유지 (편의상)
    public TokenScore(long tokenId, String token, long articleId, double tfidf, LocalDateTime publishTime) {
        this.tokenId = tokenId;
        this.token = token;
        this.articleId = articleId;
        this.tfidf = tfidf;
        this.publishTime = publishTime;
    }
}
