"""Train and evaluate the exact daily-usage model consumed by the Android app.

The input CSV is supplied explicitly and is never bundled into the APK. Expected
columns: Timestamp and Balance. Usage is derived only from positive balance drops;
balance increases (recharges) are not counted as consumption.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

import numpy as np
import pandas as pd


def load_daily_usage(csv_path: Path, minimum_samples_per_day: int) -> pd.Series:
    frame = pd.read_csv(csv_path, usecols=["Timestamp", "Balance"])
    frame["Timestamp"] = pd.to_datetime(frame["Timestamp"], errors="coerce")
    frame["Balance"] = pd.to_numeric(frame["Balance"], errors="coerce")
    frame = frame.dropna().sort_values("Timestamp").drop_duplicates("Timestamp")
    frame["Usage"] = (frame["Balance"].shift(1) - frame["Balance"]).clip(lower=0.0)
    frame["Date"] = frame["Timestamp"].dt.date
    grouped = frame.groupby("Date").agg(usage=("Usage", "sum"), samples=("Usage", "size"))
    complete = grouped[grouped["samples"] >= minimum_samples_per_day]["usage"]
    if len(complete) < 30:
        raise ValueError("At least 30 sufficiently sampled days are required")
    return complete.astype(float)


def sequences(values: np.ndarray, lag_days: int) -> tuple[np.ndarray, np.ndarray]:
    features, targets = [], []
    for index in range(lag_days, len(values)):
        features.append(values[index - lag_days:index])
        targets.append(values[index])
    return np.asarray(features, dtype=float), np.asarray(targets, dtype=float)


def chronological_split(x: np.ndarray, y: np.ndarray):
    train_end = max(1, int(len(x) * 0.6))
    validation_end = max(train_end + 1, int(len(x) * 0.8))
    if validation_end >= len(x):
        raise ValueError("Not enough sequences for chronological train/validation/test splits")
    return (
        (x[:train_end], y[:train_end]),
        (x[train_end:validation_end], y[train_end:validation_end]),
        (x[validation_end:], y[validation_end:]),
    )


def fit_ridge(x: np.ndarray, y: np.ndarray, alpha: float):
    mean = x.mean(axis=0)
    scale = x.std(axis=0)
    scale[scale == 0] = 1.0
    standardized = (x - mean) / scale
    design = np.column_stack([np.ones(len(standardized)), standardized])
    penalty = np.eye(design.shape[1]) * alpha
    penalty[0, 0] = 0.0
    weights = np.linalg.solve(design.T @ design + penalty, design.T @ y)
    return float(weights[0]), weights[1:], mean, scale


def predict(x, intercept, coefficients, mean, scale, clip_upper):
    values = intercept + ((x - mean) / scale) @ coefficients
    return np.clip(values, 0.0, clip_upper)


def metrics(actual: np.ndarray, predicted: np.ndarray) -> dict[str, float]:
    error = predicted - actual
    denominator = np.abs(actual) + np.abs(predicted)
    smape = np.mean(np.where(denominator > 1e-9, 2.0 * np.abs(error) / denominator, 0.0))
    return {
        "mae": float(np.mean(np.abs(error))),
        "rmse": float(np.sqrt(np.mean(error ** 2))),
        "smape": float(smape),
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("csv", type=Path, help="Private source CSV; do not commit it")
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--lag-days", type=int, default=7)
    parser.add_argument("--alpha", type=float, default=1.0)
    parser.add_argument("--minimum-samples-per-day", type=int, default=12)
    args = parser.parse_args()

    daily = load_daily_usage(args.csv, args.minimum_samples_per_day)
    x, y = sequences(daily.to_numpy(), args.lag_days)
    train, validation, test = chronological_split(x, y)
    intercept, coefficients, mean, scale = fit_ridge(*train, alpha=args.alpha)
    clip_upper = float(np.quantile(train[1], 0.99) * 1.5)

    ridge_validation = predict(validation[0], intercept, coefficients, mean, scale, clip_upper)
    ridge_test = predict(test[0], intercept, coefficients, mean, scale, clip_upper)
    moving_average_test = test[0].mean(axis=1)
    persistence_test = test[0][:, -1]
    validation_metrics = metrics(validation[1], ridge_validation)
    training_mean = float(train[1].mean())
    test_ridge_metrics = metrics(test[1], ridge_test)
    reliability = float(np.clip(1.0 - test_ridge_metrics["mae"] / max(training_mean, 1e-9), 0.0, 1.0))

    artifact = {
        "model_type": "ridge_daily_usage_v1",
        "lag_days": args.lag_days,
        "intercept": intercept,
        "coefficients": coefficients.tolist(),
        "feature_mean": mean.tolist(),
        "feature_scale": scale.tolist(),
        "clip_upper": clip_upper,
        "holdout_reliability": reliability,
        "validation_metrics": validation_metrics,
        "test_metrics": {
            "ridge": test_ridge_metrics,
            "moving_average": metrics(test[1], moving_average_test),
            "persistence": metrics(test[1], persistence_test),
        },
        "training": {
            "daily_rows": int(len(daily)),
            "train_sequences": int(len(train[0])),
            "validation_sequences": int(len(validation[0])),
            "test_sequences": int(len(test[0])),
            "trained_through": str(daily.index[-1]),
        },
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(artifact, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(artifact["test_metrics"], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
