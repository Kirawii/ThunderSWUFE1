package com.kirawii.thunderswufe.ml;

import java.time.LocalDateTime;

public class DailyPrediction {
    private final LocalDateTime date;
    private final double predictedUsage;
    private final double remainingBalance;

    public DailyPrediction(LocalDateTime date, double predictedUsage, double remainingBalance) {
        this.date = date;
        this.predictedUsage = predictedUsage;
        this.remainingBalance = remainingBalance;
    }

    public LocalDateTime getDate() { return date; }
    public double getPredictedUsage() { return predictedUsage; }
    public double getRemainingBalance() { return remainingBalance; }
} 