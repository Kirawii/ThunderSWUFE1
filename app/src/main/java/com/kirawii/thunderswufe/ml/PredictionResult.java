package com.kirawii.thunderswufe.ml;

import java.time.LocalDateTime;
import java.util.List;

public class PredictionResult {
    private final Integer daysUntilEmpty;
    private final List<DailyPrediction> predictions;
    private final float confidence;
    private final String error;

    public PredictionResult(Integer daysUntilEmpty, List<DailyPrediction> predictions,
                          float confidence, String error) {
        this.daysUntilEmpty = daysUntilEmpty;
        this.predictions = predictions;
        this.confidence = confidence;
        this.error = error;
    }

    public Integer getDaysUntilEmpty() { return daysUntilEmpty; }
    public List<DailyPrediction> getPredictions() { return predictions; }
    public float getConfidence() { return confidence; }
    public String getError() { return error; }
} 