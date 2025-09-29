package org.scoula.articleCrawling.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.scoula.articleCrawling.dto.ArticleCrawlingDTO;

import java.util.List;

@Mapper
public interface ArticleCrawlingMapper {
    // 기사내용 저장
    void insertArticle(ArticleCrawlingDTO dto);

    // 기사내용 조회
    List<ArticleCrawlingDTO> selectArticle();

    int countByTitleAndContent(@Param("title") String title, @Param("content") String content);
}
