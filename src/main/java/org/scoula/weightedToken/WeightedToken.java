package org.scoula.weightedToken;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.scoula.weightedToken.dto.TokenScore;

import java.util.List;

public class WeightedToken {

    private final SqlSessionFactory sqlSessionFactory;

    // 생성자에서 SqlSessionFactory 주입
    public WeightedToken(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public static void main(String[] args) {

        // 1️⃣ 직접 SqlSessionFactory 생성
        PooledDataSource dataSource = new PooledDataSource();
        dataSource.setDriver("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/sv_predictor?serverTimezone=Asia/Seoul");
        dataSource.setUsername("scoula");
        dataSource.setPassword("1234");

        Environment environment = new Environment("development", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(WeightedTokenMapper.class);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);

        WeightedToken wt = new WeightedToken(sqlSessionFactory);
        wt.calculateAndPrintTFIDF();
    }

    public void calculateAndPrintTFIDF() {
        try (SqlSession session = sqlSessionFactory.openSession()) {

            WeightedTokenMapper mapper = session.getMapper(WeightedTokenMapper.class);

            // 전체 문서 수 가져오기
            int totalDocs = mapper.selectTotalDocs();

            // TF-IDF 계산
            List<TokenScore> scores = mapper.selectAllTFIDF(totalDocs);

            // 평균과 표준편차 계산
            double mean = scores.stream().mapToDouble(TokenScore::getTfidf).average().orElse(0.0);
            double stddev = Math.sqrt(scores.stream()
                    .mapToDouble(s -> Math.pow(s.getTfidf() - mean, 2))
                    .average().orElse(0.0));

            // 정규화 계산 및 출력
            for (TokenScore ts : scores) {
                ts.setNormalized((stddev == 0) ? 0 : (ts.getTfidf() - mean) / stddev);
                System.out.printf("Token: %-15s | Article: %d | TF-IDF: %.6f | Normalized: %.6f%n",
                        ts.getToken(), ts.getArticleId(), ts.getTfidf(), ts.getNormalized());
            }

            System.out.println("✅ TF-IDF 가중치 계산 및 출력 완료!");
        }
    }
}
