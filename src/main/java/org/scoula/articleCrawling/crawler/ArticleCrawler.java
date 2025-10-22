package org.scoula.articleCrawling.crawler;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

public class ArticleCrawler {

    static class Article {
        String publishTime;
        String title;
        String content;
        String url;

        Article(String publishTime, String title, String content, String url) {
            this.publishTime = publishTime;
            this.title = title;
            this.content = content;
            this.url = url;
        }
    }

    public static void main(String[] args) {
        WebDriverManager.chromedriver().setup();

        // ✅ Selenium 기본 옵션
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // 필요 시 제거
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/141.0.7390.108 Safari/537.36");

        // ✅ MySQL 연결 정보
        String jdbcURL = "jdbc:mysql://localhost:3306/sv_predictor?useSSL=false&serverTimezone=Asia/Seoul";
        String dbUser = "scoula";
        String dbPassword = "1234";

        // ✅ 언론사 리스트
        List<String> pressIds = Arrays.asList(
                "16d4PV266g2j-N3GYq", "16bOiOx4gG2S18EPLj", "16O8sbGdUflWYlfqu4", "16FyB2ukEzHjC3cheN", "16gmjhh-4e7TNHMCYw"
        );
        List<String> pressNames = Arrays.asList(
                "조선일보", "동아일보", "매일경제", "한국경제", "머니투데이"
        );

        Set<String> visitedLinks = new HashSet<>();
        int batchSize = 50;
        Random random = new Random();

        // ✅ 날짜 포맷
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        Calendar calendar = Calendar.getInstance();

        ChromeDriver driver = null;

        try (Connection conn = DriverManager.getConnection(jdbcURL, dbUser, dbPassword)) {
            String insertSQL = "INSERT INTO news_articles (title, content, publish_time) VALUES (?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertSQL);
            int batchCount = 0;

            java.util.Date start = dateFormat.parse("20221017000000");
            java.util.Date finalEnd = dateFormat.parse("20251017000000");

            int periodIndex = 0;

            // ✅ 6개월 단위 반복
            while (start.before(finalEnd)) {
                java.util.Date tempStart = (java.util.Date) start.clone();
                calendar.setTime(start);
                calendar.add(Calendar.MONTH, 6);
                java.util.Date tempEnd = calendar.getTime();
                if (tempEnd.after(finalEnd)) tempEnd = finalEnd;

                String startDate = dateFormat.format(tempStart);
                String endDate = dateFormat.format(tempEnd);
                periodIndex++;

                System.out.println("🗓️ [" + periodIndex + "번째 구간] 크롤링 기간: " + startDate + " ~ " + endDate);

                // ✅ 각 구간마다 새 ChromeDriver 시작
                if (driver != null) {
                    try { driver.quit(); } catch (Exception ignored) {}
                }
                driver = new ChromeDriver(options);

                // ✅ 언론사별 반복
                for (int i = 0; i < pressIds.size(); i++) {
                    String pressId = pressIds.get(i);
                    String pressName = pressNames.get(i);

                    int page = 1;
                    Set<String> prevPageLinks = new HashSet<>();
                    int repeatCount = 0;

                    while (true) {
                        String url = String.format(
                                "https://search.daum.net/search?p=%d&period=u&q=삼성전자+주가&w=news&DA=STC&cp=%s&cpname=%s&sd=%s&ed=%s&article_type=photo",
                                page, pressId, pressName, startDate, endDate
                        );

                        try {
                            driver.get(url);
                        } catch (Exception e) {
                            System.out.println("❌ 페이지 로드 오류: " + e.getMessage());
                            break;
                        }

                        // Alert 처리
                        try {
                            Alert alert = driver.switchTo().alert();
                            alert.accept();
                            driver.get(url);
                        } catch (NoAlertPresentException ignored) {}

                        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
                        List<WebElement> articles = null;
                        int retries = 3;

                        while (retries > 0) {
                            try {
                                articles = wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(By.cssSelector("div.item-title a")));
                                break;
                            } catch (TimeoutException e) {
                                retries--;
                                driver.navigate().refresh();
                            }
                        }

                        if (articles == null || articles.isEmpty()) break;

                        Set<String> currentPageLinks = new HashSet<>();
                        for (WebElement article : articles) currentPageLinks.add(article.getAttribute("href"));

                        if (currentPageLinks.equals(prevPageLinks)) repeatCount++;
                        else repeatCount = 0;
                        prevPageLinks = currentPageLinks;

                        if (repeatCount >= 3) break;

                        for (WebElement article : articles) {
                            String title = article.getText().trim();
                            String link = article.getAttribute("href");

                            if (!title.contains("삼성")) continue;
                            if (visitedLinks.contains(link)) continue;
                            visitedLinks.add(link);

                            try {
                                Document doc = Jsoup.connect(link)
                                        .timeout(10000)
                                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                                                "(KHTML, like Gecko) Chrome/141.0.7390.108 Safari/537.36")
                                        .referrer("https://www.google.com")
                                        .get();

                                String publishTime = "1970-01-01 00:00:00";
                                Element timeElement = doc.selectFirst("span.num_date");
                                if (timeElement != null) {
                                    try {
                                        SimpleDateFormat srcFormat = new SimpleDateFormat("yyyy. M. d. HH:mm");
                                        SimpleDateFormat destFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                        java.util.Date date = srcFormat.parse(timeElement.text());
                                        publishTime = destFormat.format(date);
                                    } catch (ParseException ignored) {}
                                }

                                Elements paragraphs = doc.select(".news_view p");
                                if (paragraphs.isEmpty()) paragraphs = doc.select("#mArticle p");

                                StringBuilder contentBuilder = new StringBuilder();
                                for (Element p : paragraphs) contentBuilder.append(p.text()).append(" ");
                                String content = contentBuilder.toString().trim();

                                pstmt.setString(1, title);
                                pstmt.setString(2, content);
                                pstmt.setString(3, publishTime);
                                pstmt.addBatch();
                                batchCount++;

                                if (batchCount % batchSize == 0) {
                                    pstmt.executeBatch();
                                }

                                Thread.sleep(1000 + random.nextInt(2000));

                            } catch (IOException e) {
                                System.out.println("본문 크롤링 실패 (" + link + "): " + e.getMessage());
                            } catch (InterruptedException ignored) {}
                        }

                        try {
                            Thread.sleep(2000 + random.nextInt(3000));
                        } catch (InterruptedException ignored) {}

                        page++;
                    }
                }

                if (batchCount % batchSize != 0) {
                    pstmt.executeBatch();
                }

                System.out.println("✅ [" + periodIndex + "번째 구간 완료] 누적 기사 수: " + visitedLinks.size());

                // ✅ 크롤러 잠깐 휴식
                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}

                // 다음 기간으로 이동
                calendar.setTime(tempEnd);
                calendar.add(Calendar.SECOND, 1);
                start = calendar.getTime();
            }

            System.out.println("🎉 전체 크롤링 완료 — 총 수집 기사 수: " + visitedLinks.size());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (driver != null) driver.quit();
        }
    }
}
