import org.junit.jupiter.api.Test;
import org.scoula.BackendApplication;
import org.scoula.articleCrawling.dto.ArticleCrawlingDTO;
import org.scoula.articleCrawling.mapper.ArticleCrawlingMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@SpringBootTest(classes = BackendApplication.class)
class ArticleCrawlingMapperTest {

    @Autowired
    private ArticleCrawlingMapper mapper;

    @Test
    void insertArticle() {
        ArticleCrawlingDTO dto = new ArticleCrawlingDTO();
        dto.setTitle("title");
        dto.setContent("content");
        dto.setPublishDate(Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()));

        mapper.insertArticle(dto);
    }

    @Test
    void selectArticle() {

        // 전체 뉴스 기사 조회
        List<ArticleCrawlingDTO> articles = mapper.selectArticle();

        // 결과 출력
        System.out.println(articles);
    }
}