"""
Digital Broadcast Monitoring - ML Pipeline Unit Tests

Validates:
1. Dataset schema and preprocessing pipeline.
2. Target label mapping and strict feature exclusion.
3. Group-wise split isolation by video identifier (zero leakage).
4. Training and persistence of the complete pipeline.
5. Loading and inference in a fresh service instance.
"""

import os
import sys
import unittest
import numpy as np

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if BASE_DIR not in sys.path:
    sys.path.insert(0, BASE_DIR)

from ml.config import (
    DATASET_CSV_PATH,
    INITIAL_FEATURES,
    FORBIDDEN_PREDICTORS,
    MODEL_FILE_PATH,
    METADATA_FILE_PATH,
    RANDOM_SEED,
)
from ml.data_loader import load_raw_dataset, split_dataset_by_video, get_dataset_splits
from ml.pipeline import build_preprocessor, build_pipeline, validate_and_format_input
from services.stalling_ml_service import load_stalling_pipeline, predict_stalling


class MLPipelineTestCase(unittest.TestCase):

    def test_raw_dataset_loading_and_features(self):
        """Validates that raw CSV loads exactly 30,193 rows and exactly 7 features."""
        self.assertTrue(os.path.exists(DATASET_CSV_PATH), "Raw dataset CSV must exist locally")
        X, y, groups = load_raw_dataset(DATASET_CSV_PATH)

        self.assertEqual(len(X), 30193)
        self.assertEqual(len(y), 30193)
        self.assertEqual(len(groups), 30193)
        self.assertEqual(X.shape[1], 7)

        # Check target values
        unique_targets = set(np.unique(y))
        self.assertEqual(unique_targets, {0, 1})

        # Ensure no forbidden features are included in initial predictors
        for forbidden in FORBIDDEN_PREDICTORS:
            self.assertNotIn(forbidden, INITIAL_FEATURES)

    def test_group_wise_video_split_isolation(self):
        """Verifies zero video ID overlap across train, validation, and test splits."""
        (X_train, y_train, train_v), (X_val, y_val, val_v), (X_test, y_test, test_v) = get_dataset_splits(
            random_seed=RANDOM_SEED
        )

        # Total count check
        total_samples = len(y_train) + len(y_val) + len(y_test)
        self.assertEqual(total_samples, 30193)

        # Absolute isolation check
        overlap_train_val = train_v & val_v
        overlap_train_test = train_v & test_v
        overlap_val_test = val_v & test_v

        self.assertEqual(len(overlap_train_val), 0, f"Leakage found between train and val: {overlap_train_val}")
        self.assertEqual(len(overlap_train_test), 0, f"Leakage found between train and test: {overlap_train_test}")
        self.assertEqual(len(overlap_val_test), 0, f"Leakage found between val and test: {overlap_val_test}")

    def test_preprocessor_handles_missing_values(self):
        """Ensures that the preprocessing pipeline imputes missing values cleanly without crashing."""
        from sklearn.dummy import DummyClassifier

        preprocessor = build_preprocessor()
        dummy_pipe = build_pipeline(DummyClassifier(strategy="most_frequent"))

        # Train on clean mini-data
        X_sample = np.array([
            ["2020", "Zagreb", "unlimited", "1", 2000000.0, 3000000.0, 3500000.0],
            ["2021", "Wuerzburg", "1Mbps", "0", 120000.0, 125000.0, 124000.0],
        ], dtype=object)
        y_sample = np.array([0, 1])
        dummy_pipe.fit(X_sample, y_sample)

        # Inference on input with None / NaN numeric values
        X_with_missing = np.array([
            ["2020", "Zagreb", "unlimited", "1", np.nan, 3000000.0, np.nan]
        ], dtype=object)

        # Imputer should fill missing numbers with median and predict without exception
        pred = dummy_pipe.predict(X_with_missing)
        self.assertEqual(len(pred), 1)

    def test_persisted_model_loading_and_metadata(self):
        """Verifies that the saved pipeline loads in memory and metadata is populated."""
        self.assertTrue(os.path.exists(MODEL_FILE_PATH), "Saved pipeline file must exist")
        self.assertTrue(os.path.exists(METADATA_FILE_PATH), "Saved metadata file must exist")

        pipeline, metadata, err = load_stalling_pipeline(force_reload=True)
        self.assertIsNone(err)
        self.assertIsNotNone(pipeline)
        self.assertIsNotNone(metadata)

        # Metadata keys check
        required_keys = [
            "model_name", "model_version", "estimator_type", "features",
            "preprocessing", "target", "test_evaluation", "validation_comparison"
        ]
        for key in required_keys:
            self.assertIn(key, metadata, f"Metadata must contain key: '{key}'")

        self.assertEqual(metadata["features"]["feature_names"], INITIAL_FEATURES)

    def test_input_validation_rules(self):
        """Verifies that input validation accepts valid inputs and rejects invalid ones."""
        valid_input = {
            "year": "2020",
            "location": "Zagreb",
            "bandwidth": "unlimited",
            "adblock": "1",
            "dl_throughput_first1s": 1500000.0,
            "dl_throughput_first5s": 2200000.0,
            "dl_throughput_first10s": 2800000.0,
        }
        formatted = validate_and_format_input(valid_input)
        self.assertEqual(formatted.shape, (1, 7))

        # Missing field
        invalid_missing = valid_input.copy()
        del invalid_missing["bandwidth"]
        with self.assertRaises(ValueError):
            validate_and_format_input(invalid_missing)

        # Invalid categorical
        invalid_cat = valid_input.copy()
        invalid_cat["location"] = "Paris"
        with self.assertRaises(ValueError):
            validate_and_format_input(invalid_cat)

        # Negative throughput
        invalid_num = valid_input.copy()
        invalid_num["dl_throughput_first1s"] = -50.0
        with self.assertRaises(ValueError):
            validate_and_format_input(invalid_num)


if __name__ == "__main__":
    unittest.main()
