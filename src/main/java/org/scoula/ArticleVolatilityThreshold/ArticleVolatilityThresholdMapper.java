package org.scoula.ArticleVolatilityThreshold;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ArticleVolatilityThresholdMapper {

    @Select("""
        SELECT MIN(article_avg_weight) AS threshold
        FROM (
            SELECT 
                t.article_id,
                SUM(k.weight) / COUNT(t.token) AS article_avg_weight
            FROM article_tokens t
            LEFT JOIN article_keywords k 
                ON t.token = k.keyword
            JOIN news_articles n 
                ON t.article_id = n.article_id
            JOIN stock_prices s 
                ON DATE(s.trade_date) = DATE(n.publish_time + INTERVAL 1 DAY)  -- 기사 다음날 매칭
            WHERE ABS(s.daily_volatility) >= #{volatilityThreshold}          -- 변동률 기준
            GROUP BY t.article_id
        ) AS weighted_articles;
    """)
    Double calculateThresholdScore(@Param("volatilityThreshold") double volatilityThreshold);
}
