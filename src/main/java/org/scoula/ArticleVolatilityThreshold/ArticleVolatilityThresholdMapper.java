package org.scoula.ArticleVolatilityThreshold;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ArticleVolatilityThresholdMapper {

    @Select("""

            SELECT AVG(article_score)\s
FROM (
    SELECT\s
        t.article_id,
        SUM(t.frequency * COALESCE(k.weight, 0)) AS article_score
    FROM article_tokens t
    LEFT JOIN article_keywords k ON t.token = k.keyword
    JOIN news_articles n ON t.article_id = n.article_id
    JOIN stock_prices s\s
        ON DATE(n.publish_time) = s.trade_date  -- 기사 날짜와 주가 날짜 매칭
    WHERE s.daily_volatility >= #{volatilityThreshold}  -- 변동률 필터
    GROUP BY t.article_id
) AS scores

        """)
    Double calculateThresholdScore(@Param("volatilityThreshold") double volatilityThreshold);
}
