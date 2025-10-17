package org.scoula.ArticleWeightCalculator;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.util.List;
import java.util.Map;

public class ArticleWeightCalculator {

    private final SqlSessionFactory sqlSessionFactory;

    public ArticleWeightCalculator(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public static void main(String[] args) {
        // ✅ MySQL 연결 설정
        PooledDataSource dataSource = new PooledDataSource();
        dataSource.setDriver("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/sv_predictor?serverTimezone=Asia/Seoul");
        dataSource.setUsername("scoula");
        dataSource.setPassword("1234");

        Environment environment = new Environment("development", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(org.scoula.ArticleWeightCalculator.WeightedArticleMapper.class);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);

        // ✅ 실행
        ArticleWeightCalculator calc = new ArticleWeightCalculator(sqlSessionFactory);
        calc.calculateTotalArticleWeights();
    }

    public void calculateTotalArticleWeights() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            org.scoula.ArticleWeightCalculator.WeightedArticleMapper mapper = session.getMapper(org.scoula.ArticleWeightCalculator.WeightedArticleMapper.class);

            // 각 기사(article_id)별 총 가중치 합계 조회
            List<Map<String, Object>> results = mapper.selectArticleWeightSum();

            System.out.println("=== 📊 기사별 총 가중치 합계 ===");
            for (Map<String, Object> row : results) {
                long articleId = ((Number) row.get("articleId")).longValue();
                double totalWeight = ((Number) row.get("totalWeight")).doubleValue();

                System.out.printf("Article ID: %-5d | Total Weight: %.6f%n", articleId, totalWeight);
            }

            System.out.println("✅ 모든 기사에 대한 가중치 합계 계산 완료!");
        }
    }
}
