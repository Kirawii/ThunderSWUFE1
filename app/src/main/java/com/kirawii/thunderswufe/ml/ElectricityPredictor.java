package com.kirawii.thunderswufe.ml;

import android.content.Context;
import android.util.Log;
import com.kirawii.thunderswufe.data.database.ElectricityRecord;
import org.json.JSONArray;
import org.json.JSONObject;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ElectricityPredictor {
    private static final String LSTM_MODEL_FILE = "lstm_model.tflite";
    private static final String LR_KERAS_MODEL_FILE = "linear_regression_keras.tflite";
    private static final String SCALER_PARAMS_FILE = "scaler_params.json";
    private static final int DEFAULT_INPUT_LENGTH = 7;
    private static final int MODEL_OUTPUT_STEPS = 1;
    private static final int PREDICTION_HORIZON_DAYS = 3;
    private static final float FLOAT_COMPARISON_THRESHOLD = 1e-6f;

    private final Context context;
    private Interpreter lstmInterpreter;
    private Interpreter lrKerasInterpreter;
    private float[] scalerScale;
    private float[] scalerMin;

    public ElectricityPredictor(Context context) {
        this.context = context;
        loadAllModelsAndParams();
    }

    private void loadAllModelsAndParams() {
        lstmInterpreter = loadTFLiteModel(LSTM_MODEL_FILE);
        if (lstmInterpreter != null) {
            Log.i("ElectricityPredictor", "LSTM model '" + LSTM_MODEL_FILE + "' loaded successfully.");
        } else {
            Log.e("ElectricityPredictor", "Failed to load LSTM model '" + LSTM_MODEL_FILE + "'.");
        }

        lrKerasInterpreter = loadTFLiteModel(LR_KERAS_MODEL_FILE);
        if (lrKerasInterpreter != null) {
            Log.i("ElectricityPredictor", "Keras Linear Regression model '" + LR_KERAS_MODEL_FILE + "' loaded successfully.");
        } else {
            Log.e("ElectricityPredictor", "Failed to load Keras Linear Regression model '" + LR_KERAS_MODEL_FILE + "'.");
        }

        loadScalerParams(SCALER_PARAMS_FILE);
    }

    private Interpreter loadTFLiteModel(String modelFileName) {
        try {
            return new Interpreter(loadModelFile(modelFileName));
        } catch (IOException e) {
            Log.e("ElectricityPredictor", "Error loading TFLite model '" + modelFileName + "': " + e.getMessage(), e);
            return null;
        }
    }

    private ByteBuffer loadModelFile(String modelFileName) throws IOException {
        String modelPath = modelFileName;
        FileInputStream inputStream = new FileInputStream(modelPath);
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = 0;
        long declaredLength = fileChannel.size();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private void loadScalerParams(String fileName) {
        try {
            String jsonString = readAssetFile(fileName);
            JSONObject jsonObject = new JSONObject(jsonString);
            JSONArray scaleJsonArray = jsonObject.getJSONArray("scale_");
            JSONArray minJsonArray = jsonObject.getJSONArray("min_");

            scalerScale = new float[scaleJsonArray.length()];
            scalerMin = new float[minJsonArray.length()];

            for (int i = 0; i < scaleJsonArray.length(); i++) {
                scalerScale[i] = (float) scaleJsonArray.getDouble(i);
            }
            for (int i = 0; i < minJsonArray.length(); i++) {
                scalerMin[i] = (float) minJsonArray.getDouble(i);
            }

        } catch (Exception e) {
            Log.e("ElectricityPredictor", "Error loading scaler params '" + fileName + "': " + e.getMessage(), e);
            scalerScale = null;
            scalerMin = null;
        }
    }

    private String readAssetFile(String fileName) throws IOException {
        return context.getAssets().open(fileName).bufferedReader().use(BufferedReader::readText);
    }

    private float scaleUsage(float rawValue) {
        if (scalerScale == null || scalerMin == null || scalerScale.length == 0) {
            Log.w("ElectricityPredictor", "Scaler not initialized, returning raw value for scaling.");
            return rawValue;
        }
        return rawValue * scalerScale[0] + scalerMin[0];
    }

    private float inverseScaleUsage(float scaledValue) {
        if (scalerScale == null || scalerMin == null || scalerScale.length == 0 || Math.abs(scalerScale[0]) < FLOAT_COMPARISON_THRESHOLD) {
            Log.w("ElectricityPredictor", "Scaler not initialized or scale is zero, returning scaled value for inverse scaling.");
            return scaledValue;
        }
        return (scaledValue - scalerMin[0]) / scalerScale[0];
    }

    public PredictionResult predictFutureUsage(List<ElectricityRecord> records, ModelType modelTypeToUse) {
        if (records.isEmpty()) {
            return new PredictionResult(null, Collections.emptyList(), 0.0f, "没有历史数据");
        }

        List<ElectricityRecord> sortedRecords = records.stream()
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .collect(Collectors.toList());

        double currentBalance = sortedRecords.get(sortedRecords.size() - 1).getBalance();

        if (sortedRecords.size() < DEFAULT_INPUT_LENGTH) {
            Log.w("ElectricityPredictor", "历史数据不足 (需要 " + DEFAULT_INPUT_LENGTH + ", 现有 " + sortedRecords.size() + ")，尝试简单预测。");
            if (sortedRecords.size() >= 2) {
                double historicalDailyUsage = calculateHistoricalDailyUsage(sortedRecords);
                return simpleLinearPrediction(currentBalance, historicalDailyUsage);
            } else {
                return new PredictionResult(null, Collections.emptyList(), 0.0f, "历史数据不足以进行任何预测");
            }
        }

        List<Float> initialRawHistory = sortedRecords.subList(
                Math.max(0, sortedRecords.size() - DEFAULT_INPUT_LENGTH),
                sortedRecords.size()
        ).stream()
                .map(record -> (float) record.getChange())
                .collect(Collectors.toList());

        List<Float> initialScaledHistory = initialRawHistory.stream()
                .map(this::scaleUsage)
                .collect(Collectors.toList());

        Interpreter activeInterpreter;
        String modelNameForLog;

        switch (modelTypeToUse) {
            case LSTM:
                activeInterpreter = lstmInterpreter;
                modelNameForLog = "LSTM";
                break;
            case LINEAR_REGRESSION_KERAS:
                activeInterpreter = lrKerasInterpreter;
                modelNameForLog = "Keras LR";
                break;
            case SIMPLE_LINEAR:
                Log.i("ElectricityPredictor", "Using simple linear prediction as requested.");
                double historicalDailyUsage = calculateHistoricalDailyUsage(sortedRecords);
                return simpleLinearPrediction(currentBalance, historicalDailyUsage);
            default:
                throw new IllegalArgumentException("Unknown model type: " + modelTypeToUse);
        }

        if (activeInterpreter == null) {
            Log.e("ElectricityPredictor", modelNameForLog + " interpreter is null. Falling back to simple linear prediction.");
            double historicalDailyUsage = calculateHistoricalDailyUsage(sortedRecords);
            return simpleLinearPrediction(currentBalance, historicalDailyUsage);
        }

        List<DailyPrediction> dailyPredictions = new ArrayList<>();
        double tempRemainingBalance = currentBalance;
        LocalDateTime today = LocalDateTime.now();
        List<Float> currentScaledHistory = new ArrayList<>(initialScaledHistory);

        try {
            for (int dayOffset = 0; dayOffset < PREDICTION_HORIZON_DAYS; dayOffset++) {
                ByteBuffer inputBuffer = ByteBuffer.allocateDirect(DEFAULT_INPUT_LENGTH * 4)
                        .order(ByteOrder.nativeOrder());
                for (float value : currentScaledHistory) {
                    inputBuffer.putFloat(value);
                }
                inputBuffer.rewind();

                ByteBuffer outputBuffer = ByteBuffer.allocateDirect(MODEL_OUTPUT_STEPS * 4)
                        .order(ByteOrder.nativeOrder());

                activeInterpreter.run(inputBuffer, outputBuffer);
                outputBuffer.rewind();

                float predictedScaledUsage = outputBuffer.getFloat();
                float predictedRawUsage = inverseScaleUsage(predictedScaledUsage);

                tempRemainingBalance -= predictedRawUsage;
                dailyPredictions.add(new DailyPrediction(
                        today.plusDays(dayOffset + 1),
                        Math.max(0.0, predictedRawUsage),
                        tempRemainingBalance
                ));

                if (currentScaledHistory.size() > 0) {
                    currentScaledHistory.remove(0);
                }
                currentScaledHistory.add(predictedScaledUsage);

                if (tempRemainingBalance <= FLOAT_COMPARISON_THRESHOLD) {
                    tempRemainingBalance = 0.0;
                    for (int remainingDayOffset = dayOffset + 1; remainingDayOffset < PREDICTION_HORIZON_DAYS; remainingDayOffset++) {
                        dailyPredictions.add(new DailyPrediction(
                                today.plusDays(remainingDayOffset + 1),
                                0.0,
                                0.0
                        ));
                    }
                    break;
                }
            }

            double historicalDailyUsage = calculateHistoricalDailyUsage(sortedRecords);
            Integer daysUntilEmptyByModel = calculateDaysUntilEmpty(dailyPredictions, currentBalance, historicalDailyUsage);

            return new PredictionResult(
                    daysUntilEmptyByModel,
                    dailyPredictions,
                    calculateConfidence(dailyPredictions, historicalDailyUsage),
                    null
            );

        } catch (Exception e) {
            Log.e("ElectricityPredictor", "Error running TFLite inference with " + modelNameForLog + ": " + e.getMessage(), e);
            double historicalDailyUsage = calculateHistoricalDailyUsage(sortedRecords);
            return simpleLinearPrediction(currentBalance, historicalDailyUsage, "模型(" + modelNameForLog + ")预测失败: " + e.getLocalizedMessage());
        }
    }

    private Integer calculateDaysUntilEmpty(List<DailyPrediction> predictions, double currentBalance, double historicalDailyUsage) {
        if (predictions.isEmpty()) return null;

        boolean allPredictedUsageIsZero = predictions.stream()
                .allMatch(p -> p.getPredictedUsage() <= FLOAT_COMPARISON_THRESHOLD);

        if (allPredictedUsageIsZero && currentBalance > FLOAT_COMPARISON_THRESHOLD) {
            return null;
        }

        double balanceForDaysCalc = currentBalance;
        int days = 0;

        for (DailyPrediction prediction : predictions) {
            if (balanceForDaysCalc >= prediction.getPredictedUsage()) {
                balanceForDaysCalc -= prediction.getPredictedUsage();
                days++;
            } else {
                if (prediction.getPredictedUsage() > FLOAT_COMPARISON_THRESHOLD) {
                    days += (int) (balanceForDaysCalc / prediction.getPredictedUsage());
                }
                balanceForDaysCalc = 0.0;
                break;
            }
        }

        if (days == predictions.size() && balanceForDaysCalc > FLOAT_COMPARISON_THRESHOLD) {
            double lastPredictedUsage = predictions.get(predictions.size() - 1).getPredictedUsage();
            if (lastPredictedUsage > FLOAT_COMPARISON_THRESHOLD) {
                days += (int) (balanceForDaysCalc / lastPredictedUsage);
            } else if (historicalDailyUsage > FLOAT_COMPARISON_THRESHOLD) {
                days += (int) (balanceForDaysCalc / historicalDailyUsage);
            }
        }

        return days;
    }

    private double calculateHistoricalDailyUsage(List<ElectricityRecord> records) {
        if (records.size() < 2) return 0.0;

        double totalNetUsage = records.stream()
                .mapToDouble(ElectricityRecord::getChange)
                .sum();

        LocalDateTime firstTimestamp = records.get(0).getTimestamp();
        LocalDateTime lastTimestamp = records.get(records.size() - 1).getTimestamp();
        double daysBetween = ChronoUnit.DAYS.between(firstTimestamp, lastTimestamp);

        if (daysBetween > 0 && totalNetUsage > 0) {
            return totalNetUsage / daysBetween;
        } else if (records.size() == 1 && records.get(0).getChange() > 0) {
            return records.get(0).getChange();
        } else {
            return 0.0;
        }
    }

    private PredictionResult simpleLinearPrediction(double currentBalance, double historicalDailyUsage, String customError) {
        if (historicalDailyUsage <= FLOAT_COMPARISON_THRESHOLD && currentBalance > FLOAT_COMPARISON_THRESHOLD) {
            List<DailyPrediction> zeroPredictions = new ArrayList<>();
            for (int i = 0; i < PREDICTION_HORIZON_DAYS; i++) {
                zeroPredictions.add(new DailyPrediction(
                        LocalDateTime.now().plusDays(i + 1),
                        0.0,
                        currentBalance
                ));
            }
            return new PredictionResult(null, zeroPredictions, 0.5f, customError != null ? customError : "日均用电为零但有余额");
        }

        if (historicalDailyUsage <= FLOAT_COMPARISON_THRESHOLD && currentBalance <= FLOAT_COMPARISON_THRESHOLD) {
            return new PredictionResult(0, Collections.emptyList(), 1.0f, customError);
        }

        List<DailyPrediction> dailyPredictions = new ArrayList<>();
        double tempRemainingBalance = currentBalance;
        LocalDateTime today = LocalDateTime.now();

        for (int i = 0; i < PREDICTION_HORIZON_DAYS; i++) {
            double predictedUsageForDay = historicalDailyUsage;
            tempRemainingBalance -= predictedUsageForDay;
            if (tempRemainingBalance < 0) tempRemainingBalance = 0.0;
            dailyPredictions.add(new DailyPrediction(
                    today.plusDays(i + 1),
                    Math.max(0.0, predictedUsageForDay),
                    tempRemainingBalance
            ));
            if (tempRemainingBalance == 0.0) break;
        }

        while (dailyPredictions.size() < PREDICTION_HORIZON_DAYS && tempRemainingBalance == 0.0) {
            dailyPredictions.add(new DailyPrediction(
                    today.plusDays(dailyPredictions.size() + 1),
                    0.0,
                    0.0
            ));
        }

        Integer daysUntilEmpty = historicalDailyUsage > FLOAT_COMPARISON_THRESHOLD
                ? (int) (currentBalance / historicalDailyUsage)
                : null;

        return new PredictionResult(daysUntilEmpty, dailyPredictions, 0.7f, customError);
    }

    private float calculateConfidence(List<DailyPrediction> modelPredictions, double historicalDailyUsage) {
        if (modelPredictions.isEmpty()) return 0.0f;

        double avgModelPredictedUsage = modelPredictions.stream()
                .mapToDouble(DailyPrediction::getPredictedUsage)
                .average()
                .orElse(0.0);

        if (historicalDailyUsage <= FLOAT_COMPARISON_THRESHOLD) {
            return Math.abs(avgModelPredictedUsage) <= FLOAT_COMPARISON_THRESHOLD ? 0.9f : 0.1f;
        }

        double difference = Math.abs(avgModelPredictedUsage - historicalDailyUsage);
        double normalizedDifference = difference / historicalDailyUsage;
        return (float) Math.max(0.0, Math.min(1.0, 1.0 - normalizedDifference));
    }

    public void close() {
        if (lstmInterpreter != null) {
            lstmInterpreter.close();
            lstmInterpreter = null;
        }
        if (lrKerasInterpreter != null) {
            lrKerasInterpreter.close();
            lrKerasInterpreter = null;
        }
        Log.i("ElectricityPredictor", "All TFLite interpreters closed.");
    }

    public enum ModelType {
        LSTM,
        LINEAR_REGRESSION_KERAS,
        SIMPLE_LINEAR
    }

    public static class PredictionResult {
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

    public static class DailyPrediction {
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
} 