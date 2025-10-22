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
            WeightedArticleMapper mapper = session.getMapper(WeightedArticleMapper.class);

            List<Map<String, Object>> results = mapper.selectArticleWeightAverage();

            System.out.println("=== 📈 기사별 평균 가중치 (단어수 보정) ===");
            for (Map<String, Object> row : results) {
                long articleId = ((Number) row.get("articleId")).longValue();
                double avgWeight = ((Number) row.get("avgWeight")).doubleValue();
                long tokenCount = ((Number) row.get("tokenCount")).longValue();

                System.out.printf("Article ID: %-5d | Avg Weight: %.6f | Tokens: %d%n",
                        articleId, avgWeight, tokenCount);
            }

            System.out.println("✅ 평균 가중치 계산 완료!");
        }
    }
}
