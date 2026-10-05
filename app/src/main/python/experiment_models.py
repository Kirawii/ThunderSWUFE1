"""Leakage-resistant forecasting experiments for ThunderSWUFE.

This script never writes raw readings. It reports aggregate metrics only and
keeps the last 45 feature rows untouched until the final acceptance test.
"""

from __future__ import annotations

import argparse
import json
from dataclasses import dataclass
from pathlib import Path

import numpy as np
import pandas as pd
from sklearn.ensemble import (
    ExtraTreesRegressor,
    GradientBoostingRegressor,
    HistGradientBoostingRegressor,
    RandomForestRegressor,
)
from sklearn.linear_model import ElasticNet, Ridge
from sklearn.metrics import mean_absolute_error, mean_squared_error
from sklearn.model_selection import TimeSeriesSplit
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler


FINAL_HOLDOUT_DAYS = 45
MIN_COVERAGE_HOURS = 18.0
MAX_INTERVAL_HOURS = 12.0
RANDOM_STATE = 42


def load_interval_allocated_daily(csv_path: Path) -> pd.DataFrame:
    frame = pd.read_csv(csv_path, usecols=["Timestamp", "Balance"])
    frame["Timestamp"] = pd.to_datetime(frame["Timestamp"], errors="coerce")
    frame["Balance"] = pd.to_numeric(frame["Balance"], errors="coerce")
    frame = frame.dropna().sort_values("Timestamp").drop_duplicates("Timestamp")

    usage_by_day: dict[pd.Timestamp, float] = {}
    coverage_by_day: dict[pd.Timestamp, float] = {}
    rows = list(frame.itertuples(index=False))
    for previous, current in zip(rows, rows[1:]):
        start = pd.Timestamp(previous.Timestamp)
        end = pd.Timestamp(current.Timestamp)
        interval_hours = (end - start).total_seconds() / 3600.0
        if interval_hours <= 0 or interval_hours > MAX_INTERVAL_HOURS:
            continue
        total_usage = max(float(previous.Balance) - float(current.Balance), 0.0)
        cursor = start
        while cursor < end:
            boundary = min(end, cursor.normalize() + pd.Timedelta(days=1))
            hours = (boundary - cursor).total_seconds() / 3600.0
            day = cursor.normalize()
            coverage_by_day[day] = coverage_by_day.get(day, 0.0) + hours
            usage_by_day[day] = usage_by_day.get(day, 0.0) + total_usage * hours / interval_hours
            cursor = boundary

    daily = pd.DataFrame({
        "usage": pd.Series(usage_by_day),
        "coverage_hours": pd.Series(coverage_by_day),
    }).sort_index()
    daily = daily[daily["coverage_hours"] >= MIN_COVERAGE_HOURS].copy()
    if len(daily) < 120:
        raise ValueError(f"Only {len(daily)} well-covered days; at least 120 required")
    return daily


def make_features(daily: pd.DataFrame) -> tuple[pd.DataFrame, pd.Series]:
    usage = daily["usage"]
    features = pd.DataFrame(index=daily.index)
    for lag in range(1, 15):
        features[f"lag_{lag}"] = usage.shift(lag)
    for window in (3, 7, 14):
        history = usage.shift(1).rolling(window)
        features[f"mean_{window}"] = history.mean()
        features[f"std_{window}"] = history.std().fillna(0.0)
    features["trend_7"] = usage.shift(1).rolling(7).apply(
        lambda values: np.polyfit(np.arange(len(values)), values, 1)[0], raw=True
    )
    features["trend_14"] = usage.shift(1).rolling(14).apply(
        lambda values: np.polyfit(np.arange(len(values)), values, 1)[0], raw=True
    )
    weekday = features.index.dayofweek.to_numpy()
    month = features.index.month.to_numpy()
    features["weekday_sin"] = np.sin(2 * np.pi * weekday / 7)
    features["weekday_cos"] = np.cos(2 * np.pi * weekday / 7)
    features["month_sin"] = np.sin(2 * np.pi * month / 12)
    features["month_cos"] = np.cos(2 * np.pi * month / 12)
    features["coverage_lag_1"] = daily["coverage_hours"].shift(1) / 24.0
    complete = features.dropna()
    return complete, usage.loc[complete.index]


def candidates():
    return {
        "ridge_0.1": make_pipeline(StandardScaler(), Ridge(alpha=0.1)),
        "ridge_1": make_pipeline(StandardScaler(), Ridge(alpha=1.0)),
        "ridge_10": make_pipeline(StandardScaler(), Ridge(alpha=10.0)),
        "ridge_100": make_pipeline(StandardScaler(), Ridge(alpha=100.0)),
        "elastic_net": make_pipeline(
            StandardScaler(), ElasticNet(alpha=0.05, l1_ratio=0.2, max_iter=20000)
        ),
        "gradient_boosting": GradientBoostingRegressor(
            n_estimators=80, learning_rate=0.03, max_depth=2,
            min_samples_leaf=6, loss="huber", random_state=RANDOM_STATE,
        ),
        "hist_gradient_boosting": HistGradientBoostingRegressor(
            max_iter=100, learning_rate=0.05, max_leaf_nodes=7,
            min_samples_leaf=10, l2_regularization=2.0, loss="absolute_error",
            random_state=RANDOM_STATE,
        ),
        "random_forest": RandomForestRegressor(
            n_estimators=200, max_depth=4, min_samples_leaf=5,
            max_features=0.7, random_state=RANDOM_STATE, n_jobs=-1,
        ),
        "extra_trees": ExtraTreesRegressor(
            n_estimators=200, max_depth=4, min_samples_leaf=5,
            max_features=0.7, random_state=RANDOM_STATE, n_jobs=-1,
        ),
    }


def baseline_predictions(x: pd.DataFrame) -> dict[str, np.ndarray]:
    result = {
        "previous_day": x["lag_1"].to_numpy(),
        "same_weekday": x["lag_7"].to_numpy(),
        "mean_3": x["mean_3"].to_numpy(),
        "median_3": x[["lag_1", "lag_2", "lag_3"]].median(axis=1).to_numpy(),
        "mean_7": x["mean_7"].to_numpy(),
        "blend_week_mean": 0.5 * x["lag_7"].to_numpy() + 0.5 * x["mean_7"].to_numpy(),
    }
    for weight in (0.25, 0.5, 0.75):
        result[f"blend_previous_mean3_{weight}"] = (
            weight * x["lag_1"].to_numpy() + (1.0 - weight) * x["mean_3"].to_numpy()
        )
    for alpha in (0.2, 0.4, 0.6, 0.8):
        weights = np.asarray([alpha * (1.0 - alpha) ** lag for lag in range(14)])
        weights /= weights.sum()
        result[f"ewma_{alpha}"] = x[[f"lag_{lag}" for lag in range(1, 15)]].to_numpy() @ weights
    for damping in (0.1, 0.25, 0.5):
        result[f"damped_trend_{damping}"] = np.maximum(
            x["lag_1"].to_numpy()
            + damping * (x["lag_1"].to_numpy() - x["lag_2"].to_numpy()),
            0.0,
        )
    return result


def online_predictions(x: pd.DataFrame, y: pd.Series) -> dict[str, np.ndarray]:
    """Causal expert selection: every decision uses only already observed targets."""
    experts = baseline_predictions(x)
    output: dict[str, list[float]] = {
        "online_select_14": [], "online_select_28": [], "online_blend_28": []
    }
    actual = y.to_numpy(dtype=float)
    eligible = ("previous_day", "ewma_0.8", "mean_3")
    for position in range(len(x)):
        current = {name: float(experts[name][position]) for name in eligible}
        for window in (14, 28):
            start = max(0, position - window)
            if position - start < 7:
                chosen = "previous_day"
            else:
                errors = {
                    name: float(np.mean(np.abs(experts[name][start:position] - actual[start:position])))
                    for name in eligible
                }
                chosen = min(errors, key=errors.get)
            output[f"online_select_{window}"].append(current[chosen])
        start = max(0, position - 28)
        if position - start < 7:
            blended = current["previous_day"]
        else:
            errors = np.asarray([
                np.mean(np.abs(experts[name][start:position] - actual[start:position]))
                for name in eligible
            ])
            inverse = 1.0 / np.maximum(errors, 0.1)
            weights = inverse / inverse.sum()
            blended = float(sum(weights[i] * current[name] for i, name in enumerate(eligible)))
        output["online_blend_28"].append(blended)
    return {name: np.asarray(values) for name, values in output.items()}


def metric_dict(actual, predicted) -> dict[str, float]:
    actual = np.asarray(actual, dtype=float)
    predicted = np.clip(np.asarray(predicted, dtype=float), 0.0, None)
    denominator = np.abs(actual) + np.abs(predicted)
    ratios = np.zeros_like(denominator)
    np.divide(
        2 * np.abs(predicted - actual), denominator,
        out=ratios, where=denominator > 1e-9,
    )
    smape = np.mean(ratios)
    return {
        "mae": float(mean_absolute_error(actual, predicted)),
        "rmse": float(np.sqrt(mean_squared_error(actual, predicted))),
        "smape": float(smape),
    }


def cross_validate(x: pd.DataFrame, y: pd.Series) -> dict:
    splitter = TimeSeriesSplit(n_splits=4, test_size=21)
    online = online_predictions(x, y)
    results: dict[str, list[dict[str, float]]] = {
        name: [] for name in [*baseline_predictions(x).keys(), *online.keys(), *candidates().keys()]
    }
    for train_index, validation_index in splitter.split(x):
        x_train, y_train = x.iloc[train_index], y.iloc[train_index]
        x_validation, y_validation = x.iloc[validation_index], y.iloc[validation_index]
        for name, prediction in baseline_predictions(x_validation).items():
            results[name].append(metric_dict(y_validation, prediction))
        for name, prediction in online.items():
            results[name].append(metric_dict(y_validation, prediction[validation_index]))
        upper = float(np.quantile(y_train, 0.99) * 1.5)
        for name, model in candidates().items():
            model.fit(x_train, y_train)
            prediction = np.clip(model.predict(x_validation), 0.0, upper)
            results[name].append(metric_dict(y_validation, prediction))
    summary = {}
    for name, folds in results.items():
        summary[name] = {
            "mean_mae": float(np.mean([fold["mae"] for fold in folds])),
            "std_mae": float(np.std([fold["mae"] for fold in folds])),
            "mean_rmse": float(np.mean([fold["rmse"] for fold in folds])),
            "mean_smape": float(np.mean([fold["smape"] for fold in folds])),
            "fold_mae": [fold["mae"] for fold in folds],
        }
    return summary


def block_win_rate(actual, candidate, baseline, block_size=14) -> float:
    wins = []
    for start in range(0, len(actual), block_size):
        stop = min(start + block_size, len(actual))
        if stop - start < 7:
            continue
        candidate_mae = mean_absolute_error(actual[start:stop], candidate[start:stop])
        baseline_mae = mean_absolute_error(actual[start:stop], baseline[start:stop])
        wins.append(candidate_mae < baseline_mae)
    return float(np.mean(wins)) if wins else 0.0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("csv", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()

    daily = load_interval_allocated_daily(args.csv)
    x, y = make_features(daily)
    development_x, holdout_x = x.iloc[:-FINAL_HOLDOUT_DAYS], x.iloc[-FINAL_HOLDOUT_DAYS:]
    development_y, holdout_y = y.iloc[:-FINAL_HOLDOUT_DAYS], y.iloc[-FINAL_HOLDOUT_DAYS:]
    cv = cross_validate(development_x, development_y)
    baseline_names = [*baseline_predictions(development_x), *online_predictions(development_x, development_y)]
    best_baseline = min(baseline_names, key=lambda name: cv[name]["mean_mae"])
    model_names = list(candidates())
    best_model_name = min(model_names, key=lambda name: cv[name]["mean_mae"])
    best_model = candidates()[best_model_name]
    best_model.fit(development_x, development_y)
    clip_upper = float(np.quantile(development_y, 0.99) * 1.5)
    candidate_prediction = np.clip(best_model.predict(holdout_x), 0.0, clip_upper)
    all_baselines = baseline_predictions(x)
    all_baselines.update(online_predictions(x, y))
    baseline_prediction = all_baselines[best_baseline][-FINAL_HOLDOUT_DAYS:]
    candidate_metrics = metric_dict(holdout_y, candidate_prediction)
    baseline_metrics = metric_dict(holdout_y, baseline_prediction)
    relative_mae_gain = 1.0 - candidate_metrics["mae"] / baseline_metrics["mae"]
    win_rate = block_win_rate(
        holdout_y.to_numpy(), candidate_prediction, baseline_prediction
    )
    accepted = bool(relative_mae_gain >= 0.05 and win_rate >= 0.6)

    report = {
        "protocol": {
            "daily_rows": len(daily),
            "feature_rows": len(x),
            "development_rows": len(development_x),
            "final_holdout_rows": len(holdout_x),
            "holdout_start": str(holdout_x.index[0].date()),
            "holdout_end": str(holdout_x.index[-1].date()),
            "minimum_coverage_hours": MIN_COVERAGE_HOURS,
            "maximum_interval_hours": MAX_INTERVAL_HOURS,
        },
        "cross_validation": cv,
        "selected": {
            "model": best_model_name,
            "baseline": best_baseline,
        },
        "final_holdout": {
            "candidate": candidate_metrics,
            "baseline": baseline_metrics,
            "relative_mae_gain": float(relative_mae_gain),
            "fourteen_day_block_win_rate": win_rate,
        },
        "acceptance": {
            "required_relative_mae_gain": 0.05,
            "required_block_win_rate": 0.6,
            "accepted": accepted,
        },
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
