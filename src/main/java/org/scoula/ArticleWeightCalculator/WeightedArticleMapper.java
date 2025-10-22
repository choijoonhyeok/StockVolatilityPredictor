package org.scoula.ArticleWeightCalculator;

import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface WeightedArticleMapper {

    // ✅ (1) 기사별 총 가중치 합계
    @Select("""
        SELECT
            at.article_id AS articleId,
            SUM(ak.weight) AS totalWeight
        FROM article_tokens at
        JOIN article_keywords ak
            ON at.token = ak.keyword
        GROUP BY at.article_id
        ORDER BY totalWeight DESC
    """)
    List<Map<String, Object>> selectArticleWeightSum();


    // ✅ (2) 기사별 평균 가중치 (길이 보정)
    @Select("""
        SELECT
            at.article_id AS articleId,
            AVG(ak.weight) AS avgWeight,
            COUNT(at.token) AS tokenCount
        FROM article_tokens at
        JOIN article_keywords ak
            ON at.token = ak.keyword
        GROUP BY at.article_id
        ORDER BY avgWeight DESC
    """)
    List<Map<String, Object>> selectArticleWeightAverage();
}