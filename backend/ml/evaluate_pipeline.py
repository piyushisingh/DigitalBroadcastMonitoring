"""
Digital Broadcast Monitoring - Standalone Pipeline Evaluator

Loads the saved fitted pipeline and evaluates its performance on the
held-out test set with group-wise video isolation.
"""

import os
import sys
import json
import joblib
from sklearn.metrics import classification_report

CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
BACKEND_DIR = os.path.dirname(CURRENT_DIR)
if BACKEND_DIR not in sys.path:
    sys.path.insert(0, BACKEND_DIR)

from ml.config import MODEL_FILE_PATH, METADATA_FILE_PATH, DATASET_CSV_PATH
from ml.data_loader import get_dataset_splits
from ml.train_pipeline import evaluate_model


def run_evaluation():
    if not os.path.exists(MODEL_FILE_PATH):
        raise FileNotFoundError(
            f"Trained model not found at '{MODEL_FILE_PATH}'. Run 'python backend/ml/train_pipeline.py' first."
        )

    print(f"Loading trained pipeline from: {MODEL_FILE_PATH}")
    pipeline = joblib.load(MODEL_FILE_PATH)

    metadata = {}
    if os.path.exists(METADATA_FILE_PATH):
        with open(METADATA_FILE_PATH, "r", encoding="utf-8") as f:
            metadata = json.load(f)

    decision_threshold = metadata.get("decision_threshold", 0.50)

    print(f"Model Name:         {metadata.get('model_name', 'Unknown')}")
    print(f"Model Version:      {metadata.get('model_version', 'Unknown')}")
    print(f"Selected Estimator: {metadata.get('selected_model', 'Unknown')}")
    print(f"Decision Threshold: {decision_threshold:.2f}")

    print("\nLoading held-out test partition...")
    _, _, (X_test, y_test, test_videos) = get_dataset_splits(DATASET_CSV_PATH)
    print(f"Test samples: {len(y_test)} across {len(test_videos)} distinct videos.")

    metrics = evaluate_model(pipeline, X_test, y_test, threshold=decision_threshold)

    print("\n" + "=" * 60)
    print(f"HELD-OUT TEST EVALUATION METRICS (Threshold = {decision_threshold:.2f})")
    print("=" * 60)
    print(f"Accuracy:          {metrics['accuracy']:.4f}")
    print(f"Balanced Accuracy: {metrics['balanced_accuracy']:.4f}")
    print(f"Macro F1:          {metrics['macro_f1']:.4f}")
    print(f"ROC-AUC:           {metrics['roc_auc']:.4f}")
    print(f"PR-AUC:            {metrics.get('pr_auc', 0.0):.4f}")
    print(f"Class 0 (No Stalling) - P: {metrics['class_0_no_stalling']['precision']}, R: {metrics['class_0_no_stalling']['recall']}, F1: {metrics['class_0_no_stalling']['f1']}")
    print(f"Class 1 (Stalling)    - P: {metrics['class_1_stalling']['precision']}, R: {metrics['class_1_stalling']['recall']}, F1: {metrics['class_1_stalling']['f1']}")
    print(f"Confusion Matrix:  {metrics['confusion_matrix']}")
    print("=" * 60)

    if hasattr(pipeline, "predict_proba"):
        probs = pipeline.predict_proba(X_test)[:, 1]
        y_pred = (probs >= decision_threshold).astype(int)
    else:
        y_pred = pipeline.predict(X_test)

    print("\nClassification Report:")
    print(classification_report(y_test, y_pred, target_names=["No Stalling (0)", "Stalling (1)"]))

    return metrics


if __name__ == "__main__":
    run_evaluation()
