package org.scoula.ArticleWeightCalculator;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

public interface WeightedArticleMapper {

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
}