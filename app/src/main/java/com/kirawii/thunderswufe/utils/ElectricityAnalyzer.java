package com.kirawii.thunderswufe.utils;

import com.kirawii.thunderswufe.data.database.ElectricityRecord;
import com.kirawii.thunderswufe.utils.AnomalyType;
import com.kirawii.thunderswufe.utils.UsageAnomaly;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ElectricityAnalyzer {
    private static final double SUDDEN_CHANGE_THRESHOLD = 5.0; // 突然变化阈值（度）
    private static final double HIGH_USAGE_THRESHOLD = 10.0; // 高用电阈值（度/天）
    private static final int ANALYSIS_WINDOW_DAYS = 7; // 分析窗口（天）

    private ElectricityAnalyzer() {
        // 私有构造函数防止实例化
    }

    /**
     * 分析电量记录，检测异常情况
     */
    public static List<UsageAnomaly> analyzeUsagePattern(List<ElectricityRecord> records) {
        if (records.size() < 2) {
            return Collections.emptyList();
        }

        List<UsageAnomaly> anomalies = new ArrayList<>();
        List<ElectricityRecord> sortedRecords = new ArrayList<>(records);
        sortedRecords.sort((r1, r2) -> r2.getTimestamp().compareTo(r1.getTimestamp()));

        // 检查突然变化
        for (int i = 0; i < sortedRecords.size() - 1; i++) {
            ElectricityRecord current = sortedRecords.get(i);
            ElectricityRecord previous = sortedRecords.get(i + 1);

            long timeDiff = ChronoUnit.HOURS.between(previous.getTimestamp(), current.getTimestamp());
            if (timeDiff == 0) {
                continue;
            }

            double hourlyChange = Math.abs(current.getChange()) / timeDiff;
            if (hourlyChange > SUDDEN_CHANGE_THRESHOLD) {
                anomalies.add(new UsageAnomaly(
                        AnomalyType.SUDDEN_CHANGE,
                        current.getTimestamp(),
                        hourlyChange,
                        String.format("检测到用电量突然变化：%.2f度/小时", hourlyChange)
                ));
            }
        }

        // 检查持续高用电
        LocalDateTime now = LocalDateTime.now();
        List<ElectricityRecord> recentRecords = new ArrayList<>();
        for (ElectricityRecord record : sortedRecords) {
            if (ChronoUnit.DAYS.between(record.getTimestamp(), now) <= ANALYSIS_WINDOW_DAYS) {
                recentRecords.add(record);
            }
        }

        if (recentRecords.size() >= 2) {
            double totalChange = recentRecords.stream()
                    .mapToDouble(ElectricityRecord::getChange)
                    .sum();

            double days = ChronoUnit.DAYS.between(
                    recentRecords.get(recentRecords.size() - 1).getTimestamp(),
                    recentRecords.get(0).getTimestamp()
            );

            if (days > 0) {
                double dailyAverage = Math.abs(totalChange) / days;
                if (dailyAverage > HIGH_USAGE_THRESHOLD) {
                    anomalies.add(new UsageAnomaly(
                            AnomalyType.HIGH_USAGE,
                            now,
                            dailyAverage,
                            String.format("近期用电量较高：平均%.2f度/天", dailyAverage)
                    ));
                }
            }
        }

        return anomalies;
    }

    /**
     * 生成节能建议
     */
    public static List<String> generateSavingTips(List<UsageAnomaly> anomalies) {
        List<String> tips = new ArrayList<>();

        boolean hasHighUsage = anomalies.stream()
                .anyMatch(anomaly -> anomaly.getType() == AnomalyType.HIGH_USAGE);
        if (hasHighUsage) {
            tips.add("• 检查是否有大功率电器长时间运行");
            tips.add("• 建议在用电高峰期（18:00-22:00）减少用电");
            tips.add("• 及时关闭不使用的电器电源");
        }

        boolean hasSuddenChange = anomalies.stream()
                .anyMatch(anomaly -> anomaly.getType() == AnomalyType.SUDDEN_CHANGE);
        if (hasSuddenChange) {
            tips.add("• 检查是否有电器故障");
            tips.add("• 避免同时使用多个大功率电器");
        }

        return tips;
    }
} 