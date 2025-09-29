import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.KomoranResult;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class stopwordRemover {

    private static Set<String> STOPWORDS = new HashSet<>();

    static {
        // 불용어 파일 로드
        try (BufferedReader br = new BufferedReader(new FileReader("C:/graduation_thesis/StockVolatilityPredictor/stopword.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    STOPWORDS.add(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String jdbcUrl = "jdbc:mysql://localhost:3306/sv_predictor?serverTimezone=UTC";
        String dbUser = "scoula";
        String dbPassword = "1234";

        Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
            // 1) news_articles 테이블에서 기사 가져오기
            String selectSql = "SELECT article_id, content, publish_time FROM news_articles";
            try (PreparedStatement ps = conn.prepareStatement(selectSql);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    long articleId = rs.getLong("article_id");
                    String content = rs.getString("content");
                    Timestamp publishTime = rs.getTimestamp("publish_time");

                    // 2) 텍스트 정제
                    content = content.replaceAll("Copyright ©.*", "");
                    content = content.replaceAll("[^가-힣a-zA-Z0-9\\s]", " ");
                    content = content.replaceAll("http[s]?://\\S+", " ");
                    content = content.replaceAll("\\s+", " ").trim();

                    // 3) 명사 추출
                    KomoranResult result = komoran.analyze(content);
                    List<String> keywords = result.getNouns().stream()
                            .filter(word -> !STOPWORDS.contains(word))
                            .collect(Collectors.toList());

                    // 4) 빈도 계산
                    Map<String, Long> frequencyMap = keywords.stream()
                            .collect(Collectors.groupingBy(w -> w, Collectors.counting()));

                    // 5) article_tokens 테이블에 insert
                    String insertSql = "INSERT INTO article_tokens(article_id, token, frequency, publish_time) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement insertPs = conn.prepareStatement(insertSql)) {
                        for (Map.Entry<String, Long> entry : frequencyMap.entrySet()) {
                            insertPs.setLong(1, articleId);
                            insertPs.setString(2, entry.getKey());
                            insertPs.setLong(3, entry.getValue());
                            insertPs.setTimestamp(4, publishTime);
                            insertPs.addBatch();
                        }
                        insertPs.executeBatch();
                    }
                }
            }

            System.out.println("모든 기사 키워드 처리가 완료되었습니다.");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
