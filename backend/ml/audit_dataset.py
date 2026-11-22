"""
Digital Broadcast Monitoring - Reproducible Dataset Audit Script

Performs comprehensive data audit and provenance verification on the
MUEXLab video-streaming session dataset without modifying the original CSV.
"""

import os
import sys
import csv
import json
import re
from collections import Counter

# Ensure backend directory is in sys.path
CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))
BACKEND_DIR = os.path.dirname(CURRENT_DIR)
if BACKEND_DIR not in sys.path:
    sys.path.insert(0, BACKEND_DIR)

from ml.config import (
    DATASET_CSV_PATH,
    DATASET_DESC_PATH,
    AUDIT_REPORT_PATH,
    INITIAL_FEATURES,
    CATEGORICAL_FEATURES,
    NUMERIC_FEATURES,
    TARGET_COLUMN,
    GROUP_COLUMN,
    DATASET_PROVENANCE,
    FORBIDDEN_PREDICTORS,
)


def parse_description_file(desc_path):
    """
    Parses the description file and extracts column names, sections, and descriptions.
    """
    if not os.path.exists(desc_path):
        return {}

    feature_doc = {}
    current_section = "GENERAL"

    with open(desc_path, "r", encoding="utf-8") as f:
        for line in f:
            stripped = line.strip()
            if not stripped:
                continue
            if stripped.startswith("---") and stripped.endswith("---"):
                current_section = stripped.replace("-", "").strip()
                continue

            match = re.match(r"^([a-zA-Z0-9_]+)\s*:\s*(.*)$", stripped)
            if match:
                col_name, col_desc = match.group(1), match.group(2)
                feature_doc[col_name] = {
                    "section": current_section,
                    "description": col_desc.strip(),
                }

    return feature_doc


def run_data_audit(csv_path=DATASET_CSV_PATH, desc_path=DATASET_DESC_PATH, save_report=True):
    """
    Reads the CSV, validates schema against description, checks missing values,
    duplicates, feature and target distributions.
    """
    print(f"Starting Data Audit for: {csv_path}")
    if not os.path.exists(csv_path):
        raise FileNotFoundError(f"Raw dataset CSV not found at '{csv_path}'.")

    documented_features = parse_description_file(desc_path)

    total_rows = 0
    row_hashes = set()
    duplicate_count = 0

    target_counter = Counter()
    video_ids = set()
    categorical_counters = {col: Counter() for col in CATEGORICAL_FEATURES}
    numeric_stats = {
        col: {"min": float("inf"), "max": float("-inf"), "sum": 0.0, "count": 0}
        for col in NUMERIC_FEATURES
    }

    # Stream the CSV to check all columns
    with open(csv_path, "r", encoding="utf-8") as f:
        reader = csv.reader(f)
        header = next(reader)
        col_count = len(header)
        col_to_idx = {col: i for i, col in enumerate(header)}
        missing_per_col = [0] * col_count

        # Validate that required features and target are in header
        missing_required = [f for f in INITIAL_FEATURES + [TARGET_COLUMN, GROUP_COLUMN] if f not in col_to_idx]
        if missing_required:
            raise ValueError(f"Required columns missing from CSV header: {missing_required}")

        for row in reader:
            total_rows += 1
            row_tuple = tuple(row)
            if row_tuple in row_hashes:
                duplicate_count += 1
            else:
                row_hashes.add(row_tuple)

            # Check missing values per column
            for i, val in enumerate(row):
                if val == "" or val is None:
                    missing_per_col[i] += 1

            # Feature-specific stats
            for cat_col in CATEGORICAL_FEATURES:
                idx = col_to_idx[cat_col]
                categorical_counters[cat_col][row[idx]] += 1

            for num_col in NUMERIC_FEATURES:
                idx = col_to_idx[num_col]
                val_str = row[idx]
                if val_str != "":
                    try:
                        v = float(val_str)
                        stats = numeric_stats[num_col]
                        if v < stats["min"]:
                            stats["min"] = v
                        if v > stats["max"]:
                            stats["max"] = v
                        stats["sum"] += v
                        stats["count"] += 1
                    except ValueError:
                        pass

            # Target & Video group
            target_val = row[col_to_idx[TARGET_COLUMN]].strip()
            target_counter[target_val] += 1
            video_ids.add(row[col_to_idx[GROUP_COLUMN]])

    # Discrepancies between documented schema and actual CSV
    doc_cols = set(documented_features.keys())
    actual_cols = set(header)
    in_csv_not_doc = sorted(list(actual_cols - doc_cols))
    in_doc_not_csv = sorted(list(doc_cols - actual_cols))

    columns_with_missing = {
        header[i]: missing_per_col[i] for i in range(col_count) if missing_per_col[i] > 0
    }

    numeric_summary = {}
    for col, st in numeric_stats.items():
        avg = st["sum"] / st["count"] if st["count"] > 0 else 0.0
        numeric_summary[col] = {
            "min": round(st["min"], 2),
            "max": round(st["max"], 2),
            "mean": round(avg, 2),
            "valid_samples": st["count"],
            "unit": "Bytes/s",
        }

    categorical_summary = {
        col: dict(counter) for col, counter in categorical_counters.items()
    }

    report = {
        "dataset_name": DATASET_PROVENANCE["name"],
        "attribution": DATASET_PROVENANCE["attribution"],
        "csv_path": csv_path,
        "row_count": total_rows,
        "column_count": col_count,
        "duplicate_rows": duplicate_count,
        "distinct_videos": len(video_ids),
        "target_column": TARGET_COLUMN,
        "target_distribution": dict(target_counter),
        "columns_with_missing_count": len(columns_with_missing),
        "columns_with_missing_details": columns_with_missing,
        "initial_features_selected": INITIAL_FEATURES,
        "forbidden_predictors": FORBIDDEN_PREDICTORS,
        "schema_discrepancy": {
            "in_csv_not_documented": in_csv_not_doc,
            "in_documented_not_csv": in_doc_not_csv,
            "documented_column_count": len(doc_cols),
            "csv_column_count": len(actual_cols),
            "matches_exact": len(in_csv_not_doc) == 0 and len(in_doc_not_csv) == 0,
        },
        "numeric_feature_summary": numeric_summary,
        "categorical_feature_summary": categorical_summary,
        "documentation": {
            col: documented_features.get(col, {"section": "UNKNOWN", "description": "No description available"})
            for col in INITIAL_FEATURES + [TARGET_COLUMN, GROUP_COLUMN]
        },
        "provenance_disclaimer": DATASET_PROVENANCE["disclaimer"],
    }

    print("\n" + "=" * 60)
    print("DATA AUDIT SUMMARY")
    print("=" * 60)
    print(f"Total Rows:              {total_rows}")
    print(f"Total Columns:           {col_count}")
    print(f"Duplicate Rows:          {duplicate_count}")
    print(f"Distinct Video IDs:      {len(video_ids)}")
    print(f"Target Distribution:     {dict(target_counter)}")
    print(f"Columns with Missing:    {len(columns_with_missing)} (Total missing values: {sum(columns_with_missing.values())})")
    print(f"Missing in 7 Features:   {sum(columns_with_missing.get(f, 0) for f in INITIAL_FEATURES)}")
    print(f"Schema Match:            {'PERFECT (119/119)' if report['schema_discrepancy']['matches_exact'] else 'MISMATCH'}")
    print("=" * 60)

    if save_report:
        os.makedirs(os.path.dirname(AUDIT_REPORT_PATH), exist_ok=True)
        with open(AUDIT_REPORT_PATH, "w", encoding="utf-8") as f:
            json.dump(report, f, indent=2)
        print(f"Audit report saved to: {AUDIT_REPORT_PATH}")

    return report


if __name__ == "__main__":
    run_data_audit()
