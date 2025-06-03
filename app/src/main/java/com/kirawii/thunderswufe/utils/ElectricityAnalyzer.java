package com.kirawii.thunderswufe.utils;

import com.kirawii.thunderswufe.data.database.ElectricityRecord;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.HashSet;

public class ElectricityAnalyzer {
    private static final double SUDDEN_CHANGE_THRESHOLD = 5.0;
    private static final double HIGH_USAGE_THRESHOLD = 10.0;
    private static final int ANALYSIS_WINDOW_DAYS = 7;

    public static List<UsageAnomaly> analyzeUsagePattern(List<ElectricityRecord> records) {
        if (records.size() < 2) return Collections.emptyList();
        List<UsageAnomaly> anomalies = new ArrayList<>();
        List<ElectricityRecord> sortedRecords = new ArrayList<>(records);
        sortedRecords.sort(Comparator.comparing(ElectricityRecord::getTimestamp).reversed());

        // 检查突然变化
        for (int i = 0; i < sortedRecords.size() - 1; i++) {
            ElectricityRecord current = sortedRecords.get(i);
            ElectricityRecord previous = sortedRecords.get(i + 1);
            long timeDiff = ChronoUnit.HOURS.between(previous.getTimestamp(), current.getTimestamp());
            if (timeDiff == 0L) continue;
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

        List<ElectricityRecord> recentRecords = new ArrayList<>();
        for (ElectricityRecord record : sortedRecords) {
            if (ChronoUnit.DAYS.between(record.getTimestamp(), LocalDateTime.now()) <= ANALYSIS_WINDOW_DAYS) {
                recentRecords.add(record);
            }
        }
        // 去重：同一timestamp只保留一条
        List<ElectricityRecord> uniqueRecords = new ArrayList<>();
        HashSet<LocalDateTime> seenTimestamps = new HashSet<>();
        for (ElectricityRecord r : recentRecords) {
            if (seenTimestamps.add(r.getTimestamp())) {
                uniqueRecords.add(r);
            }
        }
        if (uniqueRecords.size() >= 2) {
            double totalChange = 0.0;
            StringBuilder sb = new StringBuilder();
            sb.append("[ElectricityAnalyzer] recentRecords for daily average calculation:\n");
            for (ElectricityRecord record : uniqueRecords) {
                totalChange += record.getChange();
                sb.append(record.getTimestamp()).append(", change=").append(record.getChange()).append("\n");
            }
            double days = ChronoUnit.DAYS.between(
                    uniqueRecords.get(uniqueRecords.size() - 1).getTimestamp(),
                    uniqueRecords.get(0).getTimestamp()
            );
            sb.append("totalChange=").append(totalChange).append(", days=").append(days).append("\n");
            if (days > 0) {
                double dailyAverage = Math.abs(totalChange) / days;
                sb.append("dailyAverage=").append(dailyAverage);
                android.util.Log.d("ElectricityAnalyzer", sb.toString());
                if (dailyAverage > HIGH_USAGE_THRESHOLD) {
                    anomalies.add(new UsageAnomaly(
                            AnomalyType.HIGH_USAGE,
                            LocalDateTime.now(),
                            dailyAverage,
                            String.format("近期用电量较高：平均%.2f度/天", dailyAverage)
                    ));
                }
            } else {
                android.util.Log.d("ElectricityAnalyzer", sb.toString());
            }
        }
        // 如果没有异常，添加NORMAL类型
        if (anomalies.isEmpty()) {
            anomalies.add(new UsageAnomaly(
                AnomalyType.NORMAL,
                LocalDateTime.now(),
                0.0,
                "用电一切正常，感谢您的绿色生活方式，继续保持！"
            ));
        }
        return anomalies;
    }

    public static List<String> generateSavingTips(List<UsageAnomaly> anomalies) {
        List<String> tips = new ArrayList<>();
        boolean hasHighUsage = false;
        boolean hasSuddenChange = false;
        for (UsageAnomaly anomaly : anomalies) {
            if (anomaly.getType() == AnomalyType.HIGH_USAGE) hasHighUsage = true;
            if (anomaly.getType() == AnomalyType.SUDDEN_CHANGE) hasSuddenChange = true;
        }
        if (hasHighUsage) {
            tips.add("• 检查是否有大功率电器长时间运行");
            tips.add("• 建议在用电高峰期（18:00-22:00）减少用电");
            tips.add("• 及时关闭不使用的电器电源");
        }
        if (hasSuddenChange) {
            tips.add("• 检查是否有电器故障");
            tips.add("• 避免同时使用多个大功率电器");
        }
        boolean hasNormal = false;
        for (UsageAnomaly anomaly : anomalies) {
            if (anomaly.getType() == AnomalyType.NORMAL) hasNormal = true;
        }
        if (hasNormal) {
            tips.add("• 继续保持，感谢您的绿色生活方式！");
        }
        return tips;
    }

    public enum AnomalyType {
        SUDDEN_CHANGE,
        HIGH_USAGE,
        NORMAL
    }

    public static class UsageAnomaly {
        private final AnomalyType type;
        private final LocalDateTime timestamp;
        private final double value;
        private final String message;

        public UsageAnomaly(AnomalyType type, LocalDateTime timestamp, double value, String message) {
            this.type = type;
            this.timestamp = timestamp;
            this.value = value;
            this.message = message;
        }

        public AnomalyType getType() { return type; }
        public LocalDateTime getTimestamp() { return timestamp; }
        public double getValue() { return value; }
        public String getMessage() { return message; }
    }
} 