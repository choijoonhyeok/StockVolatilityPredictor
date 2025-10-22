package org.scoula.ArticleVolatilityThreshold;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.session.*;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

public class ArticleVolatilityThreshold {

    public static void main(String[] args) {
        // MySQL 설정
        PooledDataSource dataSource = new PooledDataSource();
        dataSource.setDriver("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/sv_predictor?serverTimezone=Asia/Seoul");
        dataSource.setUsername("scoula");
        dataSource.setPassword("1234");

        Environment environment = new Environment("development", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(ArticleVolatilityThresholdMapper.class);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);

        try (SqlSession session = sqlSessionFactory.openSession()) {
            ArticleVolatilityThresholdMapper mapper = session.getMapper(ArticleVolatilityThresholdMapper.class);

            double volatilityThreshold =  0.0235; // 변동률 기준 (%)
            Double thresholdScore = mapper.calculateThresholdScore(volatilityThreshold);

            if (thresholdScore != null) {
                System.out.printf(
                        "📊 변동률 %.2f%% 이상인 기사들의 최소 평균 가중치(임계점): %.6f%n",
                        volatilityThreshold,
                        thresholdScore
                );
            } else {
                System.out.println("⚠️ 기준 변동률 이상의 기사 데이터가 없습니다.");
            }
        }
    }
}
