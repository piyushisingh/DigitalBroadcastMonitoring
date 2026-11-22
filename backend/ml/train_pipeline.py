"""
Digital Broadcast Monitoring - Reproducible Model Training and Persistence Pipeline

Trains candidate classifiers on group-split training data, validates on validation set,
evaluates best model once on the held-out test set, and saves the complete fitted pipeline
along with full metadata and the optimal decision threshold.
"""

import os
import sys
import json
import datetime
import platform
import joblib
import numpy as np
import sklearn
from sklearn.dummy import DummyClassifier
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier, ExtraTreesClassifier, HistGradientBoostingClassifier
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

from ml.config import (
    DATASET_CSV_PATH,
    MODEL_DIR,
    MODEL_FILE_PATH,
    METADATA_FILE_PATH,
    INITIAL_FEATURES,
    CATEGORICAL_FEATURES,
    NUMERIC_FEATURES,
    TARGET_COLUMN,
    CLASS_NAMES,
    RANDOM_SEED,
    DATASET_PROVENANCE,
    TRAIN_RATIO,
    VAL_RATIO,
    TEST_RATIO,
)
from ml.data_loader import get_dataset_splits
from ml.pipeline import build_pipeline


def evaluate_model(pipeline, X, y, threshold=0.50):
    """
    Computes a comprehensive dictionary of evaluation metrics for a fitted pipeline
    given a specific decision threshold.
    """
    if hasattr(pipeline, "predict_proba"):
        y_prob = pipeline.predict_proba(X)[:, 1]
        auc = float(roc_auc_score(y, y_prob))
        pr_auc = float(average_precision_score(y, y_prob))
        y_pred = (y_prob >= threshold).astype(int)
    elif hasattr(pipeline, "decision_function"):
        y_score = pipeline.decision_function(X)
        auc = float(roc_auc_score(y, y_score))
        pr_auc = float(average_precision_score(y, y_score))
        y_pred = pipeline.predict(X)
    else:
        auc = 0.5
        pr_auc = float(np.mean(y))
        y_pred = pipeline.predict(X)

    acc = float(accuracy_score(y, y_pred))
    bal_acc = float(balanced_accuracy_score(y, y_pred))
    macro_f1 = float(f1_score(y, y_pred, average="macro"))

    p0 = float(precision_score(y, y_pred, pos_label=0, zero_division=0))
    r0 = float(recall_score(y, y_pred, pos_label=0, zero_division=0))
    f0 = float(f1_score(y, y_pred, pos_label=0, zero_division=0))

    p1 = float(precision_score(y, y_pred, pos_label=1, zero_division=0))
    r1 = float(recall_score(y, y_pred, pos_label=1, zero_division=0))
    f1 = float(f1_score(y, y_pred, pos_label=1, zero_division=0))

    cm = confusion_matrix(y, y_pred).tolist()

    return {
        "accuracy": round(acc, 4),
        "balanced_accuracy": round(bal_acc, 4),
        "macro_f1": round(macro_f1, 4),
        "roc_auc": round(auc, 4),
        "pr_auc": round(pr_auc, 4),
        "class_0_no_stalling": {"precision": round(p0, 4), "recall": round(r0, 4), "f1": round(f0, 4)},
        "class_1_stalling": {"precision": round(p1, 4), "recall": round(r1, 4), "f1": round(f1, 4)},
        "confusion_matrix": cm,
        "decision_threshold": threshold,
    }


def find_optimal_threshold(pipeline, X_val, y_val):
    """
    Evaluates decision thresholds on validation data to optimize Macro F1 while balancing recall.
    """
    if not hasattr(pipeline, "predict_proba"):
        return 0.50, []

    val_probs = pipeline.predict_proba(X_val)[:, 1]
    best_thresh = 0.50
    best_mf1 = -1.0
    thresh_table = []

    for thresh in np.arange(0.20, 0.85, 0.05):
        thresh = round(float(thresh), 2)
        y_pred = (val_probs >= thresh).astype(int)
        prec = float(precision_score(y_val, y_pred, pos_label=1, zero_division=0))
        rec = float(recall_score(y_val, y_pred, pos_label=1, zero_division=0))
        mf1 = float(f1_score(y_val, y_pred, average="macro"))
        bacc = float(balanced_accuracy_score(y_val, y_pred))

        cm = confusion_matrix(y_val, y_pred)
        tn, fp, fn, tp = cm.ravel()
        fpr = float(fp / (fp + tn)) if (fp + tn) > 0 else 0.0

        thresh_table.append({
            "threshold": thresh,
            "stalling_precision": round(prec, 4),
            "stalling_recall": round(rec, 4),
            "macro_f1": round(mf1, 4),
            "balanced_accuracy": round(bacc, 4),
            "false_positive_rate": round(fpr, 4),
        })

        if mf1 > best_mf1:
            best_mf1 = mf1
            best_thresh = thresh

    return best_thresh, thresh_table


def train_and_evaluate_all():
    """
    Executes the entire training, comparison, persistence, and test evaluation workflow.
    """
    print("=" * 70)
    print("DIGITAL BROADCAST MONITORING - ML TRAINING PIPELINE")
    print("=" * 70)

    # 1. Load and split data
    print(f"Loading dataset from: {DATASET_CSV_PATH}")
    (X_train, y_train, train_v), (X_val, y_val, val_v), (X_test, y_test, test_v) = get_dataset_splits()

    total_samples = len(y_train) + len(y_val) + len(y_test)
    print(f"Dataset loaded: {total_samples} total rows.")
    print(f"  Train: {len(y_train)} rows across {len(train_v)} videos (Stalling rate: {y_train.mean():.4f})")
    print(f"  Val:   {len(y_val)} rows across {len(val_v)} videos (Stalling rate: {y_val.mean():.4f})")
    print(f"  Test:  {len(y_test)} rows across {len(test_v)} videos (Stalling rate: {y_test.mean():.4f})")

    # Verify zero group leakage
    overlap_tv = len(train_v & val_v)
    overlap_tt = len(train_v & test_v)
    overlap_vt = len(val_v & test_v)
    print(f"Group leakage check (video IDs overlap):")
    print(f"  Train-Val: {overlap_tv}, Train-Test: {overlap_tt}, Val-Test: {overlap_vt}")
    if overlap_tv > 0 or overlap_tt > 0 or overlap_vt > 0:
        raise RuntimeError("Data leakage detected across video group splits!")

    # 2. Define candidates for comparison (using the 7 input signals + throughput dynamics)
    candidates = {
        "DummyClassifier": (DummyClassifier(strategy="most_frequent"), False),
        "LogisticRegression": (LogisticRegression(max_iter=1000, random_state=RANDOM_SEED), False),
        "RandomForestClassifier_Baseline": (
            RandomForestClassifier(n_estimators=100, random_state=RANDOM_SEED, n_jobs=-1),
            False,
        ),
        "ExtraTreesClassifier_Baseline": (
            ExtraTreesClassifier(n_estimators=100, random_state=RANDOM_SEED, n_jobs=-1),
            False,
        ),
        "HistGradientBoosting_Dynamics": (
            HistGradientBoostingClassifier(random_state=RANDOM_SEED),
            True,
        ),
        "RandomForest_Balanced_Tuned": (
            RandomForestClassifier(
                n_estimators=200,
                max_depth=16,
                min_samples_leaf=2,
                class_weight="balanced_subsample",
                random_state=RANDOM_SEED,
                n_jobs=-1,
            ),
            True,
        ),
    }

    validation_results = {}
    fitted_pipelines = {}

    print("\n--- PHASE 1: CANDIDATE MODEL COMPARISON (TRAIN & VALIDATION ONLY) ---")
    for name, (estimator, use_derived) in candidates.items():
        print(f"\nTraining pipeline for: {name} (derived_features={use_derived})...")
        pipeline = build_pipeline(estimator, use_derived_features=use_derived)
        pipeline.fit(X_train, y_train)
        metrics = evaluate_model(pipeline, X_val, y_val, threshold=0.50)
        validation_results[name] = metrics
        fitted_pipelines[name] = pipeline

        print(f"  Validation Balanced Accuracy: {metrics['balanced_accuracy']:.4f}")
        print(f"  Validation Macro F1:          {metrics['macro_f1']:.4f}")
        print(f"  Validation ROC-AUC:           {metrics['roc_auc']:.4f}")
        print(f"  Validation PR-AUC:            {metrics['pr_auc']:.4f}")
        print(f"  Validation Accuracy:          {metrics['accuracy']:.4f}")
        print(f"  Stalling Precision / Recall:  {metrics['class_1_stalling']['precision']:.4f} / {metrics['class_1_stalling']['recall']:.4f}")

    # 3. Model selection based on validation Macro F1
    best_model_name = max(validation_results.keys(), key=lambda k: validation_results[k]["macro_f1"])
    best_pipeline = fitted_pipelines[best_model_name]
    print(f"\nBest Candidate Selected based on Validation Macro F1: {best_model_name}")

    # 4. Decision Threshold Tuning on Validation data
    best_threshold, threshold_table = find_optimal_threshold(best_pipeline, X_val, y_val)
    print(f"Selected Decision Threshold from Validation Data: {best_threshold:.2f}")

    # 5. Final evaluation ONCE on held-out test set
    print("\n--- PHASE 2: FINAL EVALUATION ON HELD-OUT TEST SET ---")
    test_metrics = evaluate_model(best_pipeline, X_test, y_test, threshold=best_threshold)
    print(f"Test Set Evaluation Results for '{best_model_name}' (threshold={best_threshold}):")
    print(f"  Test Accuracy:          {test_metrics['accuracy']:.4f}")
    print(f"  Test Balanced Accuracy: {test_metrics['balanced_accuracy']:.4f}")
    print(f"  Test Macro F1:          {test_metrics['macro_f1']:.4f}")
    print(f"  Test ROC-AUC:           {test_metrics['roc_auc']:.4f}")
    print(f"  Test PR-AUC:            {test_metrics['pr_auc']:.4f}")
    print(f"  Stalling Precision:     {test_metrics['class_1_stalling']['precision']:.4f}")
    print(f"  Stalling Recall:        {test_metrics['class_1_stalling']['recall']:.4f}")
    print(f"  Confusion Matrix:       {test_metrics['confusion_matrix']}")

    test_probs = best_pipeline.predict_proba(X_test)[:, 1] if hasattr(best_pipeline, "predict_proba") else None
    y_test_pred = (test_probs >= best_threshold).astype(int) if test_probs is not None else best_pipeline.predict(X_test)
    print("\nDetailed Test Classification Report:")
    print(classification_report(y_test, y_test_pred, target_names=["No Stalling (0)", "Stalling (1)"]))

    # 6. Persist complete pipeline
    os.makedirs(MODEL_DIR, exist_ok=True)
    joblib.dump(best_pipeline, MODEL_FILE_PATH)
    print(f"Complete fitted pipeline saved to: {MODEL_FILE_PATH}")

    # 7. Store rich model metadata
    metadata = {
        "model_name": "EarlySessionStallingClassifier",
        "model_version": "1.1.0",
        "estimator_type": type(best_pipeline.named_steps["classifier"]).__name__,
        "selected_model": best_model_name,
        "decision_threshold": best_threshold,
        "score_semantics": "uncalibrated_model_estimate",
        "training_timestamp": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "python_version": platform.python_version(),
        "scikit_learn_version": sklearn.__version__,
        "dataset": {
            "name": DATASET_PROVENANCE["name"],
            "attribution": DATASET_PROVENANCE["attribution"],
            "csv_path": DATASET_CSV_PATH,
            "total_rows": total_samples,
            "train_samples": len(y_train),
            "val_samples": len(y_val),
            "test_samples": len(y_test),
            "train_videos": len(train_v),
            "val_videos": len(val_v),
            "test_videos": len(test_v),
            "split_ratios": {"train": TRAIN_RATIO, "val": VAL_RATIO, "test": TEST_RATIO},
            "split_methodology": "GroupShuffleSplit by video identifier with zero inter-split overlap",
            "random_seed": RANDOM_SEED,
            "feature_selection_rationale": (
                "Only the initial seven session and traffic signals available within the first 10 seconds "
                "(year, location, bandwidth, adblock, dl_throughput_first1s, dl_throughput_first5s, "
                "dl_throughput_first10s) are accepted as inputs. An internal transformer computes early throughput "
                "dynamics (ramp-up ratio, sustaining ratio, drift delta, throughput floor, and mean) strictly "
                "from these 7 inputs to maximize early pattern recognition."
            ),
        },
        "features": {
            "feature_names": INITIAL_FEATURES,
            "input_feature_names": INITIAL_FEATURES,
            "categorical_features": CATEGORICAL_FEATURES,
            "numeric_features": NUMERIC_FEATURES,
            "numeric_units": "Bytes/s",
        },
        "preprocessing": {
            "feature_transformer": "EarlyThroughputDynamicsTransformer",
            "numeric_pipeline": ["SimpleImputer(strategy='median')", "StandardScaler()"],
            "categorical_pipeline": [
                "SimpleImputer(strategy='most_frequent')",
                "OneHotEncoder(handle_unknown='ignore', sparse_output=False)",
            ],
        },
        "target": {
            "name": TARGET_COLUMN,
            "class_mapping": CLASS_NAMES,
            "description": "True indicates stalling occurred during video playback; False indicates smooth playback.",
        },
        "validation_comparison": validation_results,
        "validation_threshold_tuning": threshold_table,
        "test_evaluation": test_metrics,
        "provenance_disclaimer": DATASET_PROVENANCE["disclaimer"],
    }

    with open(METADATA_FILE_PATH, "w", encoding="utf-8") as f:
        json.dump(metadata, f, indent=2)
    print(f"Model metadata saved to: {METADATA_FILE_PATH}")

    print("\n" + "=" * 70)
    print("TRAINING AND PERSISTENCE COMPLETE")
    print("=" * 70)

    return best_pipeline, metadata


if __name__ == "__main__":
    train_and_evaluate_all()
