"""
Digital Broadcast Monitoring - ML Data Loader and Leak-Free Group Splitting

Loads session-dataset.csv and performs group-aware splitting based on video identifiers,
guaranteeing zero data leakage across training, validation, and test sets.
"""

import os
import csv
import numpy as np

from ml.config import (
    DATASET_CSV_PATH,
    INITIAL_FEATURES,
    TARGET_COLUMN,
    GROUP_COLUMN,
    TARGET_MAPPING,
    RANDOM_SEED,
    TRAIN_RATIO,
    VAL_RATIO,
    TEST_RATIO,
)


def load_raw_dataset(csv_path=DATASET_CSV_PATH):
    """
    Reads the raw CSV and extracts:
        - X: 2D numpy array of shape (N, 7) containing INITIAL_FEATURES
        - y: 1D numpy array of shape (N,) with binary targets (0: no_stalling, 1: stalling)
        - groups: 1D numpy array of shape (N,) with video IDs
    """
    if not os.path.exists(csv_path):
        raise FileNotFoundError(f"Session dataset not found at '{csv_path}'")

    videos = []
    X_rows = []
    y_rows = []

    with open(csv_path, "r", encoding="utf-8") as f:
        reader = csv.DictReader(f)
        for row in reader:
            videos.append(int(row[GROUP_COLUMN]))

            # Format the 7 features
            year_val = str(row["year"]).strip()
            loc_val = str(row["location"]).strip()
            bw_val = str(row["bandwidth"]).strip()
            adblock_val = str(row["adblock"]).strip()

            t1 = float(row["dl_throughput_first1s"])
            t5 = float(row["dl_throughput_first5s"])
            t10 = float(row["dl_throughput_first10s"])

            X_rows.append([year_val, loc_val, bw_val, adblock_val, t1, t5, t10])

            # Target mapping
            raw_target = row[TARGET_COLUMN].strip().lower()
            if raw_target not in TARGET_MAPPING:
                raise ValueError(f"Unknown target value: '{row[TARGET_COLUMN]}'")
            y_rows.append(TARGET_MAPPING[raw_target])

    X = np.array(X_rows, dtype=object)
    y = np.array(y_rows, dtype=np.int32)
    groups = np.array(videos, dtype=np.int32)

    return X, y, groups


def split_dataset_by_video(
    X,
    y,
    groups,
    train_ratio=TRAIN_RATIO,
    val_ratio=VAL_RATIO,
    test_ratio=TEST_RATIO,
    random_seed=RANDOM_SEED,
):
    """
    Splits dataset into Train, Validation, and Test sets grouped by video identifier.
    Guarantees that no video ID appears in more than one partition.

    Returns:
        (X_train, y_train, train_videos),
        (X_val, y_val, val_videos),
        (X_test, y_test, test_videos)
    """
    assert abs(train_ratio + val_ratio + test_ratio - 1.0) < 1e-5, "Ratios must sum to 1.0"

    rng = np.random.RandomState(random_seed)
    unique_videos = np.unique(groups)
    rng.shuffle(unique_videos)

    n_total = len(unique_videos)
    n_train = int(n_total * train_ratio)
    n_val = int(n_total * val_ratio)

    train_videos = set(unique_videos[:n_train])
    val_videos = set(unique_videos[n_train : n_train + n_val])
    test_videos = set(unique_videos[n_train + n_val :])

    # Assert strict disjointness
    assert len(train_videos & val_videos) == 0, "Train and Val videos overlap!"
    assert len(train_videos & test_videos) == 0, "Train and Test videos overlap!"
    assert len(val_videos & test_videos) == 0, "Val and Test videos overlap!"

    train_mask = np.isin(groups, list(train_videos))
    val_mask = np.isin(groups, list(val_videos))
    test_mask = np.isin(groups, list(test_videos))

    return (
        (X[train_mask], y[train_mask], train_videos),
        (X[val_mask], y[val_mask], val_videos),
        (X[test_mask], y[test_mask], test_videos),
    )


def get_dataset_splits(csv_path=DATASET_CSV_PATH, random_seed=RANDOM_SEED):
    """
    Convenience function returning the three leak-free splits.
    """
    X, y, groups = load_raw_dataset(csv_path)
    return split_dataset_by_video(X, y, groups, random_seed=random_seed)
