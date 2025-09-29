package org.scoula.stockCrawling;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class volatilityAnalyzer {
    public static void main(String[] args) {
        String jdbUrl = "jdbc:mysql://localhost:3306/sv_predictor?serverTimezone=Asia/Seoul";
        String user = "scoula";
        String password = "1234";

        try (Connection conn = DriverManager.getConnection(jdbUrl, user, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT trade_date, daily_volatility FROM stock_prices ORDER BY trade_date")) {

            List<Double> volList = new ArrayList<>();
            List<Date> dateList = new ArrayList<>();

            while (rs.next()) {
                dateList.add(rs.getDate("trade_date"));
                volList.add(rs.getDouble("daily_volatility") * 100); // %로 변환
            }

            if (volList.isEmpty()) {
                System.out.println("데이터가 없습니다.");
                return;
            }

            // 평균 계산
            double mean = volList.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            // 분산 계산
            double variance = volList.stream().mapToDouble(v -> Math.pow(v - mean, 2)).average().orElse(0);
            // 표준편차
            double stddev = Math.sqrt(variance);

            System.out.printf("평균 변동률: %.2f%%%n", mean);
            System.out.printf("표준편차: %.2f%%%n", stddev);

            // -----------------------------
            // 1️⃣ 라인 차트: 일별 변동률
            XYSeries series = new XYSeries("Daily Volatility");
            for (int i = 0; i < volList.size(); i++) {
                series.add(i, volList.get(i)); // x축: index, y축: 변동률
            }
            XYSeriesCollection dataset = new XYSeriesCollection();
            dataset.addSeries(series);

            JFreeChart lineChart = ChartFactory.createXYLineChart(
                    "Samsung Electronics Daily Volatility",
                    "Days",
                    "Volatility (%)",
                    dataset,
                    PlotOrientation.VERTICAL,
                    true, true, false
            );

            // -----------------------------
            // 2️⃣ 히스토그램: 변동률 분포
            double[] volArray = volList.stream().mapToDouble(Double::doubleValue).toArray();
            HistogramDataset histDataset = new HistogramDataset();
            histDataset.addSeries("Volatility Distribution", volArray, 50); // 50 bins

            JFreeChart histChart = ChartFactory.createHistogram(
                    "Distribution of Daily Volatility",
                    "Volatility (%)",
                    "Frequency",
                    histDataset,
                    PlotOrientation.VERTICAL,
                    true, true, false
            );

            // -----------------------------
            // 그래프 표시
            JFrame frame = new JFrame("Volatility Charts");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 800);
            frame.setLayout(new java.awt.GridLayout(2,1));

            frame.add(new ChartPanel(lineChart));
            frame.add(new ChartPanel(histChart));

            frame.setVisible(true);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
