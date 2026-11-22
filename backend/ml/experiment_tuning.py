"""
Digital Broadcast Monitoring - ML Model Improvement & Hyperparameter Tuning Experiment

Investigates:
1. Baseline vs Feature Engineering (throughput dynamics from the 7 initial signals).
2. Multiple model architectures:
   - DummyClassifier (baseline)
   - LogisticRegression
   - RandomForestClassifier
   - ExtraTreesClassifier
   - HistGradientBoostingClassifier
3. Group-aware cross-validation on the training set (no leakage across video groups).
4. Class weighting analysis (uniform vs balanced).
5. Validation set candidate comparison and ranking.
6. Validation-based decision threshold optimization.
7. Final evaluation on the held-out test set.
8. Preservation of results in experiment_results.json without premature artifact promotion.
"""

import os
import sys
import time
import json
import numpy as np
from sklearn.base import BaseEstimator, TransformerMixin
from sklearn.pipeline import Pipeline
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.impute import SimpleImputer
from sklearn.dummy import DummyClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier, ExtraTreesClassifier, HistGradientBoostingClassifier
from sklearn.model_selection import GroupKFold
from sklearn.metrics import (
    accuracy_score,
    balanced_accuracy_score,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
    average_precision_score,
    confusion_matrix,
    classification_report,
)

CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
BACKEND_DIR = os.path.dirname(CURRENT_DIR)
if BACKEND_DIR not in sys.path:
    sys.path.insert(0, BACKEND_DIR)

from ml.config import DATASET_CSV_PATH, RANDOM_SEED
from ml.data_loader import load_raw_dataset, split_dataset_by_video

EXPERIMENT_RESULTS_PATH = os.path.join(CURRENT_DIR, "experiment_results.json")


class EarlyThroughputDynamicsTransformer(BaseEstimator, TransformerMixin):
    """
    Computes domain-specific early-session throughput dynamics from the 7 permitted inputs.
    Input: shape (N, 7) - [year, location, bandwidth, adblock, t1, t5, t10]
    Output: shape (N, 13) - original 7 inputs + 6 derived throughput dynamic features:
        - ratio_5_1: (t5 + 1) / (t1 + 1) [ramp-up acceleration in first 5s]
        - ratio_10_5: (t10 + 1) / (t5 + 1) [ramp-up sustaining factor between 5s and 10s]
        - delta_10_1: t10 - t1 [net throughput difference over 10s]
        - t_min: min(t1, t5, t10) [critical throughput floor]
        - t_mean: (t1 + t5 + t10) / 3.0 [average initial throughput]
        - log_t10: log(1 + t10) [log scale representation]
    """

    def fit(self, X, y=None):
        return self

    def transform(self, X):
        X = np.asarray(X)
        t1 = X[:, 4].astype(float)
        t5 = X[:, 5].astype(float)
        t10 = X[:, 6].astype(float)

        ratio_5_1 = (t5 + 1.0) / (t1 + 1.0)
        ratio_10_5 = (t10 + 1.0) / (t5 + 1.0)
        delta_10_1 = t10 - t1
        t_min = np.minimum(np.minimum(t1, t5), t10)
        t_mean = (t1 + t5 + t10) / 3.0
        log_t10 = np.log1p(np.maximum(0.0, t10))

        derived = np.column_stack([ratio_5_1, ratio_10_5, delta_10_1, t_min, t_mean, log_t10])
        return np.hstack([X, derived])


def build_experiment_preprocessor(use_derived_features=False):
    """
    Builds the ColumnTransformer for raw or expanded features.
    """
    if use_derived_features:
        # Categorical columns remain indices 0, 1, 2, 3
        # Numeric columns: 4, 5, 6 (original) + 7, 8, 9, 10, 11, 12 (derived)
        cat_cols = [0, 1, 2, 3]
        num_cols = [4, 5, 6, 7, 8, 9, 10, 11, 12]
        preprocessor = ColumnTransformer(
            transformers=[
                (
                    "num",
                    Pipeline([
                        ("imputer", SimpleImputer(strategy="median")),
                        ("scaler", StandardScaler()),
                    ]),
                    num_cols,
                ),
                (
                    "cat",
                    Pipeline([
                        ("imputer", SimpleImputer(strategy="most_frequent")),
                        ("encoder", OneHotEncoder(handle_unknown="ignore", sparse_output=False)),
                    ]),
                    cat_cols,
                ),
            ]
        )
        return Pipeline([
            ("derive", EarlyThroughputDynamicsTransformer()),
            ("col_transform", preprocessor),
        ])
    else:
        cat_cols = [0, 1, 2, 3]
        num_cols = [4, 5, 6]
        return ColumnTransformer(
            transformers=[
                (
                    "num",
                    Pipeline([
                        ("imputer", SimpleImputer(strategy="median")),
                        ("scaler", StandardScaler()),
                    ]),
                    num_cols,
                ),
                (
                    "cat",
                    Pipeline([
                        ("imputer", SimpleImputer(strategy="most_frequent")),
                        ("encoder", OneHotEncoder(handle_unknown="ignore", sparse_output=False)),
                    ]),
                    cat_cols,
                ),
            ]
        )


def evaluate_predictions(y_true, y_pred, y_prob=None):
    """
    Computes all standard evaluation metrics.
    """
    acc = float(accuracy_score(y_true, y_pred))
    bal_acc = float(balanced_accuracy_score(y_true, y_pred))
    macro_f1 = float(f1_score(y_true, y_pred, average="macro"))

    p0 = float(precision_score(y_true, y_pred, pos_label=0, zero_division=0))
    r0 = float(recall_score(y_true, y_pred, pos_label=0, zero_division=0))
    f0 = float(f1_score(y_true, y_pred, pos_label=0, zero_division=0))

    p1 = float(precision_score(y_true, y_pred, pos_label=1, zero_division=0))
    r1 = float(recall_score(y_true, y_pred, pos_label=1, zero_division=0))
    f1 = float(f1_score(y_true, y_pred, pos_label=1, zero_division=0))

    if y_prob is not None:
        roc_auc = float(roc_auc_score(y_true, y_prob))
        pr_auc = float(average_precision_score(y_true, y_prob))
    else:
        roc_auc = 0.5
        pr_auc = float(np.mean(y_true))

    cm = confusion_matrix(y_true, y_pred).tolist()

    return {
        "accuracy": round(acc, 4),
        "balanced_accuracy": round(bal_acc, 4),
        "macro_f1": round(macro_f1, 4),
        "roc_auc": round(roc_auc, 4),
        "pr_auc": round(pr_auc, 4),
        "stalling_class": {"precision": round(p1, 4), "recall": round(r1, 4), "f1": round(f1, 4)},
        "no_stalling_class": {"precision": round(p0, 4), "recall": round(r0, 4), "f1": round(f0, 4)},
        "confusion_matrix": cm,
    }


def run_experiments():
    print("=" * 80)
    print("DIGITAL BROADCAST MONITORING - MODEL IMPROVEMENT EXPERIMENT")
    print("=" * 80)

    # 1. Load data
    X, y, groups = load_raw_dataset(DATASET_CSV_PATH)
    (X_train, y_train, train_v), (X_val, y_val, val_v), (X_test, y_test, test_v) = split_dataset_by_video(
        X, y, groups, random_seed=RANDOM_SEED
    )

    train_groups = groups[np.isin(groups, list(train_v))]

    print(f"Dataset split sizes:")
    print(f"  Train: {len(y_train)} sessions, {len(train_v)} videos (stalling: {y_train.mean():.4f})")
    print(f"  Val:   {len(y_val)} sessions, {len(val_v)} videos (stalling: {y_val.mean():.4f})")
    print(f"  Test:  {len(y_test)} sessions, {len(test_v)} videos (stalling: {y_test.mean():.4f})")
    assert len(train_v & val_v) == 0 and len(train_v & test_v) == 0 and len(val_v & test_v) == 0

    # 2. Define Candidate Configurations
    candidate_configs = [
        # Baselines
        {
            "name": "Dummy_MostFrequent",
            "model": DummyClassifier(strategy="most_frequent"),
            "derived_features": False,
        },
        {
            "name": "LogisticRegression_Baseline",
            "model": LogisticRegression(max_iter=1000, random_state=RANDOM_SEED),
            "derived_features": False,
        },
        {
            "name": "RandomForest_Baseline_100",
            "model": RandomForestClassifier(n_estimators=100, random_state=RANDOM_SEED, n_jobs=-1),
            "derived_features": False,
        },
        {
            "name": "ExtraTrees_Baseline_100",
            "model": ExtraTreesClassifier(n_estimators=100, random_state=RANDOM_SEED, n_jobs=-1),
            "derived_features": False,
        },
        {
            "name": "HistGradientBoosting_Baseline",
            "model": HistGradientBoostingClassifier(random_state=RANDOM_SEED),
            "derived_features": False,
        },
        # Feature Engineering: 7 original + 6 throughput dynamics
        {
            "name": "RandomForest_Dynamics_100",
            "model": RandomForestClassifier(n_estimators=100, random_state=RANDOM_SEED, n_jobs=-1),
            "derived_features": True,
        },
        {
            "name": "ExtraTrees_Dynamics_100",
            "model": ExtraTreesClassifier(n_estimators=100, random_state=RANDOM_SEED, n_jobs=-1),
            "derived_features": True,
        },
        {
            "name": "HistGradientBoosting_Dynamics",
            "model": HistGradientBoostingClassifier(random_state=RANDOM_SEED),
            "derived_features": True,
        },
        # Tuned Architectures & Class Weights
        {
            "name": "RandomForest_Tuned_d16_n200",
            "model": RandomForestClassifier(
                n_estimators=200, max_depth=16, min_samples_leaf=2, random_state=RANDOM_SEED, n_jobs=-1
            ),
            "derived_features": True,
        },
        {
            "name": "RandomForest_Balanced_d16_n200",
            "model": RandomForestClassifier(
                n_estimators=200,
                max_depth=16,
                min_samples_leaf=2,
                class_weight="balanced_subsample",
                random_state=RANDOM_SEED,
                n_jobs=-1,
            ),
            "derived_features": True,
        },
        {
            "name": "ExtraTrees_Tuned_d18_n200",
            "model": ExtraTreesClassifier(
                n_estimators=200, max_depth=18, min_samples_leaf=2, random_state=RANDOM_SEED, n_jobs=-1
            ),
            "derived_features": True,
        },
        {
            "name": "ExtraTrees_Balanced_d18_n200",
            "model": ExtraTreesClassifier(
                n_estimators=200,
                max_depth=18,
                min_samples_leaf=2,
                class_weight="balanced",
                random_state=RANDOM_SEED,
                n_jobs=-1,
            ),
            "derived_features": True,
        },
        {
            "name": "HistGradientBoosting_Tuned_lr05_l2",
            "model": HistGradientBoostingClassifier(
                learning_rate=0.05, max_iter=200, min_samples_leaf=15, l2_regularization=0.5, random_state=RANDOM_SEED
            ),
            "derived_features": True,
        },
        {
            "name": "HistGradientBoosting_Balanced_lr05",
            "model": HistGradientBoostingClassifier(
                learning_rate=0.05, max_iter=200, min_samples_leaf=15, class_weight="balanced", random_state=RANDOM_SEED
            ),
            "derived_features": True,
        },
    ]

    # 3. Video-Group Cross-Validation on Training Set (5 Folds)
    print("\n--- PHASE 1: 5-FOLD VIDEO-GROUP CROSS-VALIDATION (TRAINING SET ONLY) ---")
    gkf = GroupKFold(n_splits=5)
    cv_summary = {}

    for cfg in candidate_configs:
        name = cfg["name"]
        print(f"Cross-validating {name} across 5 video folds...")
        fold_macro_f1s = []
        fold_bal_accs = []
        fold_rocs = []

        for fold_idx, (trn_idx, val_idx) in enumerate(gkf.split(X_train, y_train, groups=train_groups)):
            X_tr, y_tr = X_train[trn_idx], y_train[trn_idx]
            X_va, y_va = X_train[val_idx], y_train[val_idx]

            pipe = Pipeline([
                ("prep", build_experiment_preprocessor(cfg["derived_features"])),
                ("clf", cfg["model"]),
            ])
            pipe.fit(X_tr, y_tr)
            preds = pipe.predict(X_va)

            if hasattr(pipe, "predict_proba"):
                probs = pipe.predict_proba(X_va)[:, 1]
                auc = roc_auc_score(y_va, probs)
            else:
                auc = 0.5

            fold_macro_f1s.append(f1_score(y_va, preds, average="macro"))
            fold_bal_accs.append(balanced_accuracy_score(y_va, preds))
            fold_rocs.append(auc)

        cv_summary[name] = {
            "cv_mean_macro_f1": round(float(np.mean(fold_macro_f1s)), 4),
            "cv_std_macro_f1": round(float(np.std(fold_macro_f1s)), 4),
            "cv_mean_bal_acc": round(float(np.mean(fold_bal_accs)), 4),
            "cv_mean_roc_auc": round(float(np.mean(fold_rocs)), 4),
        }
        print(
            f"  Mean Macro F1: {cv_summary[name]['cv_mean_macro_f1']:.4f} (+/- {cv_summary[name]['cv_std_macro_f1']:.4f}) | "
            f"Mean Bal Acc: {cv_summary[name]['cv_mean_bal_acc']:.4f} | "
            f"Mean ROC-AUC: {cv_summary[name]['cv_mean_roc_auc']:.4f}"
        )

    # 4. Validation Set Evaluation & Model Selection
    print("\n--- PHASE 2: CANDIDATE COMPARISON ON VALIDATION SET ---")
    val_metrics = {}
    fitted_pipelines = {}

    for cfg in candidate_configs:
        name = cfg["name"]
        pipe = Pipeline([
            ("prep", build_experiment_preprocessor(cfg["derived_features"])),
            ("clf", cfg["model"]),
        ])

        t0 = time.perf_counter()
        pipe.fit(X_train, y_train)
        fit_time = time.perf_counter() - t0

        t1 = time.perf_counter()
        y_val_pred = pipe.predict(X_val)
        y_val_prob = pipe.predict_proba(X_val)[:, 1] if hasattr(pipe, "predict_proba") else None
        infer_latency_ms = (time.perf_counter() - t1) / len(X_val) * 1000

        metrics = evaluate_predictions(y_val, y_val_pred, y_val_prob)
        metrics["training_time_s"] = round(fit_time, 2)
        metrics["inference_latency_ms"] = round(infer_latency_ms, 3)
        metrics["cv_mean_macro_f1"] = cv_summary[name]["cv_mean_macro_f1"]

        val_metrics[name] = metrics
        fitted_pipelines[name] = pipe

        print(f"[{name}]")
        print(
            f"  Val Macro F1: {metrics['macro_f1']:.4f} | Bal Acc: {metrics['balanced_accuracy']:.4f} | "
            f"Acc: {metrics['accuracy']:.4f} | ROC-AUC: {metrics['roc_auc']:.4f} | PR-AUC: {metrics['pr_auc']:.4f}"
        )
        print(
            f"  Stalling: Recall={metrics['stalling_class']['recall']:.4f}, "
            f"Precision={metrics['stalling_class']['precision']:.4f}, F1={metrics['stalling_class']['f1']:.4f}"
        )

    # Rank candidates by Validation Macro F1
    sorted_candidates = sorted(val_metrics.items(), key=lambda kv: kv[1]["macro_f1"], reverse=True)
    best_candidate_name = sorted_candidates[0][0]
    best_candidate_metrics = sorted_candidates[0][1]
    best_candidate_pipe = fitted_pipelines[best_candidate_name]

    print("\n" + "=" * 60)
    print(f"TOP CANDIDATE SELECTED: {best_candidate_name}")
    print(f"Validation Macro F1: {best_candidate_metrics['macro_f1']:.4f}")
    print(f"Validation Stalling Recall: {best_candidate_metrics['stalling_class']['recall']:.4f}")
    print(f"Validation Stalling Precision: {best_candidate_metrics['stalling_class']['precision']:.4f}")
    print("=" * 60)

    # 5. Threshold Optimization on Validation Set Only
    print("\n--- PHASE 3: DECISION THRESHOLD TUNING (VALIDATION SET ONLY) ---")
    val_probs = best_candidate_pipe.predict_proba(X_val)[:, 1]

    threshold_table = []
    threshold_range = np.arange(0.20, 0.85, 0.05)
    best_threshold = 0.50
    best_threshold_macro_f1 = -1.0

    print(f"{'Threshold':<10}{'Precision':<12}{'Recall':<10}{'Macro F1':<12}{'Bal Acc':<10}{'FPR':<10}")
    print("-" * 64)

    for thresh in threshold_range:
        thresh = round(float(thresh), 2)
        y_thresh_pred = (val_probs >= thresh).astype(int)

        prec = precision_score(y_val, y_thresh_pred, pos_label=1, zero_division=0)
        rec = recall_score(y_val, y_thresh_pred, pos_label=1, zero_division=0)
        mf1 = f1_score(y_val, y_thresh_pred, average="macro")
        bacc = balanced_accuracy_score(y_val, y_thresh_pred)

        cm = confusion_matrix(y_val, y_thresh_pred)
        tn, fp, fn, tp = cm.ravel()
        fpr = fp / (fp + tn) if (fp + tn) > 0 else 0.0

        row = {
            "threshold": thresh,
            "stalling_precision": round(prec, 4),
            "stalling_recall": round(rec, 4),
            "macro_f1": round(mf1, 4),
            "balanced_accuracy": round(bacc, 4),
            "false_positive_rate": round(fpr, 4),
        }
        threshold_table.append(row)
        print(f"{thresh:<10.2f}{prec:<12.4f}{rec:<10.4f}{mf1:<12.4f}{bacc:<10.4f}{fpr:<10.4f}")

        # Choose threshold that optimizes Macro F1 on validation set
        if mf1 > best_threshold_macro_f1:
            best_threshold_macro_f1 = mf1
            best_threshold = thresh

    print(f"\nOptimal Selected Decision Threshold: {best_threshold:.2f} (Val Macro F1 = {best_threshold_macro_f1:.4f})")

    # 6. Single Final Evaluation on Held-Out Test Set
    print("\n--- PHASE 4: FINAL EVALUATION ON HELD-OUT TEST SET ---")
    test_probs = best_candidate_pipe.predict_proba(X_test)[:, 1]

    # Baseline threshold 0.50 vs Optimized threshold
    test_pred_default = (test_probs >= 0.50).astype(int)
    test_pred_opt = (test_probs >= best_threshold).astype(int)

    test_metrics_default = evaluate_predictions(y_test, test_pred_default, test_probs)
    test_metrics_opt = evaluate_predictions(y_test, test_pred_opt, test_probs)

    print(f"Final Test Evaluation for '{best_candidate_name}':")
    print(f"  [Default Threshold 0.50]")
    print(
        f"    Accuracy: {test_metrics_default['accuracy']:.4f} | Bal Acc: {test_metrics_default['balanced_accuracy']:.4f} | "
        f"Macro F1: {test_metrics_default['macro_f1']:.4f} | ROC-AUC: {test_metrics_default['roc_auc']:.4f} | PR-AUC: {test_metrics_default['pr_auc']:.4f}"
    )
    print(
        f"    Stalling Precision: {test_metrics_default['stalling_class']['precision']:.4f} | "
        f"Recall: {test_metrics_default['stalling_class']['recall']:.4f}"
    )
    print(f"    Confusion Matrix: {test_metrics_default['confusion_matrix']}")

    print(f"  [Selected Threshold {best_threshold:.2f}]")
    print(
        f"    Accuracy: {test_metrics_opt['accuracy']:.4f} | Bal Acc: {test_metrics_opt['balanced_accuracy']:.4f} | "
        f"Macro F1: {test_metrics_opt['macro_f1']:.4f}"
    )
    print(
        f"    Stalling Precision: {test_metrics_opt['stalling_class']['precision']:.4f} | "
        f"Recall: {test_metrics_opt['stalling_class']['recall']:.4f}"
    )
    print(f"    Confusion Matrix: {test_metrics_opt['confusion_matrix']}")

    # Save complete experiment results
    experiment_data = {
        "timestamp": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
        "dataset_split": {
            "train_sessions": len(y_train),
            "train_videos": len(train_v),
            "val_sessions": len(y_val),
            "val_videos": len(val_v),
            "test_sessions": len(y_test),
            "test_videos": len(test_v),
        },
        "cv_summary": cv_summary,
        "validation_metrics": val_metrics,
        "selected_candidate": {
            "name": best_candidate_name,
            "metrics": best_candidate_metrics,
        },
        "threshold_tuning": {
            "table": threshold_table,
            "selected_threshold": best_threshold,
            "validation_macro_f1_at_threshold": best_threshold_macro_f1,
        },
        "final_test_evaluation": {
            "threshold_0_50": test_metrics_default,
            "selected_threshold": test_metrics_opt,
        },
    }

    with open(EXPERIMENT_RESULTS_PATH, "w", encoding="utf-8") as f:
        json.dump(experiment_data, f, indent=2)
    print(f"\nExperiment results saved separately to: {EXPERIMENT_RESULTS_PATH}")

    return best_candidate_name, best_candidate_pipe, best_threshold, test_metrics_opt


if __name__ == "__main__":
    run_experiments()
