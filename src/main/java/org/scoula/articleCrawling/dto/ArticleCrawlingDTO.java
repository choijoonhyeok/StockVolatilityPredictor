package org.scoula.articleCrawling.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArticleCrawlingDTO {
    private Long articleId;
    private String title;
    private String content;
    private Date publishDate;
}
