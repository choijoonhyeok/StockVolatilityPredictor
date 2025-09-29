package org.scoula.articleCrawling.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class stockCrawler {
    public static void main(String[] args) throws IOException {
        // DB 연결 설정
        String jdbcUrl = "jdbc:mysql://localhost:3306/sv_predictor?serverTimezone=Asia/Seoul";
        String dbUser = "scoula";
        String dbPassword = "1234";

        // 날짜 포맷
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        // 날짜 범위
        LocalDate startDate = LocalDate.of(2020, 9, 26);
        LocalDate endDate = LocalDate.of(2025, 9, 26);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword)) {
            String insertSQL = "INSERT INTO stock_prices (stock_code, trade_date, open_price, close_price, high_price, low_price, daily_volatility) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement pstmt = conn.prepareStatement(insertSQL);

            double prevClose = 0; // 전일 종가 저장용

            for (int page = 1; page <= 500; page++) {
                String url = "https://finance.naver.com/item/sise_day.naver?code=005930&page=" + page;
                Document doc = Jsoup.connect(url).get();
                Elements rows = doc.select("table.type2 tr");

                for (Element row : rows) {
                    Elements cols = row.select("td");
                    if (cols.size() < 7) continue;

                    String dateText = cols.get(0).text().trim();
                    if (dateText.isEmpty()) continue;

                    LocalDate date = LocalDate.parse(dateText, formatter);

                    if (date.isBefore(startDate)) {
                        return; // 더 과거 데이터는 필요 없음
                    }

                    if (!date.isBefore(startDate) && !date.isAfter(endDate)) {
                        try {
                            double close = Double.parseDouble(cols.get(1).text().replace(",", ""));
                            double open = Double.parseDouble(cols.get(3).text().replace(",", ""));
                            double high = Double.parseDouble(cols.get(4).text().replace(",", ""));
                            double low = Double.parseDouble(cols.get(5).text().replace(",", ""));

                            // 전일 종가 기준 절대 변동률 계산
                            double dailyVolatility = 0;
                            if (prevClose != 0) {
                                dailyVolatility = Math.abs((close - prevClose) / prevClose);
                            }

                            // DB Insert
                            pstmt.setString(1, "005930");   // 삼성전자 종목코드
                            pstmt.setDate(2, Date.valueOf(date));
                            pstmt.setDouble(3, open);
                            pstmt.setDouble(4, close);
                            pstmt.setDouble(5, high);
                            pstmt.setDouble(6, low);
                            pstmt.setDouble(7, dailyVolatility);

                            pstmt.executeUpdate();

                            System.out.printf("저장됨: %s | 종가:%f | 시가:%f | 고가:%f | 저가:%f | 변동폭: %.2f%%%n",
                                    date, close, open, high, low, dailyVolatility * 100);

                            prevClose = close; // 다음 날 계산용

                        } catch (NumberFormatException e) {
                            // 숫자 변환 실패 (공백 데이터 등) → 스킵
                        }
                    }
                }
            }
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
