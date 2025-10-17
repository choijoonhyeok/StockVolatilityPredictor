package org.scoula.weightedToken;

import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.scoula.weightedToken.dto.TokenScore;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WeightedToken {

    private final SqlSessionFactory sqlSessionFactory;

    public WeightedToken(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public static void main(String[] args) {
        // MySQL 연결 설정
        PooledDataSource dataSource = new PooledDataSource();
        dataSource.setDriver("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://localhost:3306/sv_predictor?serverTimezone=Asia/Seoul");
        dataSource.setUsername("scoula");
        dataSource.setPassword("1234");

        Environment environment = new Environment("development", new JdbcTransactionFactory(), dataSource);
        Configuration configuration = new Configuration(environment);
        configuration.addMapper(org.scoula.weightedToken.WeightedTokenMapper.class);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);

        WeightedToken wt = new WeightedToken(sqlSessionFactory);
        wt.calculateAndPrintTFIDF();
    }

    public void calculateAndPrintTFIDF() {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            org.scoula.weightedToken.WeightedTokenMapper mapper = session.getMapper(org.scoula.weightedToken.WeightedTokenMapper.class);

            // 전체 문서 수 조회
            int totalDocs = mapper.selectTotalDocs();
            List<TokenScore> scores = mapper.selectAllTFIDF(totalDocs);

            // 1단계: TF-IDF 평균 및 표준편차 계산
            double mean = scores.stream().mapToDouble(TokenScore::getTfidf).average().orElse(0.0);
            double stddev = Math.sqrt(scores.stream()
                    .mapToDouble(s -> Math.pow(s.getTfidf() - mean, 2))
                    .average().orElse(0.0));

            // 2단계: dailyVolatility 세팅
            for (TokenScore ts : scores) {
                LocalDateTime publishDateTime = ts.getPublishTime();
                LocalTime marketClose = LocalTime.of(15, 30);
                LocalDate tradeDate = publishDateTime.toLocalTime().isAfter(marketClose)
                        ? publishDateTime.toLocalDate().plusDays(1)
                        : publishDateTime.toLocalDate();

                Map<String, Object> volMap = mapper.selectNextAvailableVolatility(tradeDate);
                if (volMap == null) {
                    volMap = mapper.selectNextAvailableVolatility(tradeDate.plusDays(1));
                }

                if (volMap != null) {
                    // trade_date: java.sql.Date → LocalDate → LocalDateTime
                    java.sql.Date dbTradeDate = (java.sql.Date) volMap.get("trade_date");
                    ts.setTradeDate(dbTradeDate.toLocalDate().atStartOfDay());

                    // daily_volatility: BigDecimal → double
                    BigDecimal dailyVol = (BigDecimal) volMap.get("daily_volatility");
                    ts.setDailyVolatility(dailyVol != null ? dailyVol.doubleValue() : 0.0);
                } else {
                    ts.setTradeDate(tradeDate.atStartOfDay());
                    ts.setDailyVolatility(0.0);
                }
            }

            // 3단계: dailyVolatility 평균 및 표준편차 계산
            double meanVol = scores.stream().mapToDouble(TokenScore::getDailyVolatility).average().orElse(0.0);
            double stdVol = Math.sqrt(scores.stream()
                    .mapToDouble(ts -> Math.pow(ts.getDailyVolatility() - meanVol, 2))
                    .average().orElse(0.0));

            // 4단계: 표준화 및 최종 점수 계산
            int count = 0;
            for (TokenScore ts : scores) {
                count++;

                // TF-IDF 표준화
                ts.setNormalized(stddev == 0 ? 0 : (ts.getTfidf() - mean) / stddev);

                // 변동률 표준화
                ts.setNormalizedVol(stdVol == 0 ? 0 : (ts.getDailyVolatility() - meanVol) / stdVol);

                // 최종 점수 계산
                ts.setFinalWeightedScore(ts.getNormalized() * ts.getNormalizedVol());

                System.out.printf(
                        "Token: %-15s | Publish_time: %s | trade_date: %s | daily_volatility: %.6f | TF-IDF: %.6f | Normalized TF-IDF: %.6f | NormalizedVol: %.6f | FinalScore: %.6f | Count: %d%n",
                        ts.getToken(),
                        ts.getPublishTime(),
                        ts.getTradeDate(),
                        ts.getDailyVolatility(),
                        ts.getTfidf(),
                        ts.getNormalized(),
                        ts.getNormalizedVol(),
                        ts.getFinalWeightedScore(),
                        count
                );
               // mapper.insertKeyword(ts.getToken(), ts.getFinalWeightedScore());
            }

            //5단계 동일 단어끼리 가중치 평균 내기
            Map<String,Double> keywoirdweightMap = scores.stream()
                            .collect(Collectors.groupingBy(
                                    TokenScore::getToken,
                                    Collectors.averagingDouble(TokenScore::getFinalWeightedScore)));

            //DB 저장
            for(Map.Entry<String,Double> entry : keywoirdweightMap.entrySet()) {
                mapper.insertKeyword(entry.getKey(), entry.getValue());
            }

            session.commit();

            System.out.println("전체 문서 수: " + totalDocs);
            System.out.println("✅ TF-IDF × 변동률 계산 완료!");
        }
    }
}



