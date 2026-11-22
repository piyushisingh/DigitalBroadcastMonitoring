"""
Digital Broadcast Monitoring - ML Preprocessing and Pipeline Definition

Provides reusable scikit-learn preprocessing transformers, feature engineering
for throughput dynamics, pipeline assembly, and strict input validation for
early-session video-streaming stalling prediction.
"""

import numpy as np
from sklearn.base import BaseEstimator, TransformerMixin
from sklearn.pipeline import Pipeline
from sklearn.compose import ColumnTransformer
from sklearn.preprocessing import StandardScaler, OneHotEncoder
from sklearn.impute import SimpleImputer

from ml.config import (
    INITIAL_FEATURES,
    CATEGORICAL_FEATURES,
    NUMERIC_FEATURES,
    ALLOWED_CATEGORICAL_VALUES,
)


class EarlyThroughputDynamicsTransformer(BaseEstimator, TransformerMixin):
    """
    Computes domain-specific early-session throughput dynamics from the 7 permitted inputs.
    Input: shape (N, 7) - [year, location, bandwidth, adblock, t1, t5, t10]
    Output: shape (N, 13) - original 7 inputs + 6 derived throughput dynamics:
        - ratio_5_1: (t5 + 1) / (t1 + 1) [ramp-up acceleration in first 5s]
        - ratio_10_5: (t10 + 1) / (t5 + 1) [sustaining factor between 5s and 10s]
        - delta_10_1: t10 - t1 [net throughput difference over 10s]
        - t_min: min(t1, t5, t10) [critical throughput floor]
        - t_mean: (t1 + t5 + t10) / 3.0 [average initial throughput]
        - log_t10: log(1 + max(0, t10)) [log scale representation]
    """

    def fit(self, X, y=None):
        return self

    def transform(self, X):
        X = np.asarray(X, dtype=object)
        # Handle potential None / NaN gracefully
        t1 = np.where(X[:, 4] == None, np.nan, X[:, 4]).astype(float)
        t5 = np.where(X[:, 5] == None, np.nan, X[:, 5]).astype(float)
        t10 = np.where(X[:, 6] == None, np.nan, X[:, 6]).astype(float)

        ratio_5_1 = (t5 + 1.0) / (t1 + 1.0)
        ratio_10_5 = (t10 + 1.0) / (t5 + 1.0)
        delta_10_1 = t10 - t1
        t_min = np.minimum(np.minimum(t1, t5), t10)
        t_mean = (t1 + t5 + t10) / 3.0
        log_t10 = np.log1p(np.maximum(0.0, t10))

        derived = np.column_stack([ratio_5_1, ratio_10_5, delta_10_1, t_min, t_mean, log_t10])
        return np.hstack([X, derived])


def get_feature_indices(use_derived_features=True):
    """
    Returns column indices for numeric and categorical features within transformed array.
    """
    cat_indices = [0, 1, 2, 3]  # year, location, bandwidth, adblock
    if use_derived_features:
        # 4, 5, 6 (original) + 7, 8, 9, 10, 11, 12 (derived)
        num_indices = [4, 5, 6, 7, 8, 9, 10, 11, 12]
    else:
        num_indices = [4, 5, 6]
    return cat_indices, num_indices


def build_preprocessor(use_derived_features=True):
    """
    Constructs a ColumnTransformer that handles both numeric and categorical features.
    Imputers are included to guarantee that missing values at inference time are handled
    appropriately using statistics fitted strictly on the training set.
    """
    cat_indices, num_indices = get_feature_indices(use_derived_features=use_derived_features)

    numeric_transformer = Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="median")),
            ("scaler", StandardScaler()),
        ]
    )

    categorical_transformer = Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="most_frequent")),
            ("encoder", OneHotEncoder(handle_unknown="ignore", sparse_output=False)),
        ]
    )

    col_transformer = ColumnTransformer(
        transformers=[
            ("num", numeric_transformer, num_indices),
            ("cat", categorical_transformer, cat_indices),
        ],
        remainder="drop",
    )

    if use_derived_features:
        return Pipeline([
            ("dynamics", EarlyThroughputDynamicsTransformer()),
            ("col_transformer", col_transformer),
        ])
    else:
        return col_transformer


def build_pipeline(classifier, use_derived_features=True):
    """
    Wraps preprocessor and estimator into a complete scikit-learn Pipeline.
    Accepts the original 7 features as input.
    """
    preprocessor = build_preprocessor(use_derived_features=use_derived_features)
    pipeline = Pipeline(
        steps=[
            ("preprocessor", preprocessor),
            ("classifier", classifier),
        ]
    )
    return pipeline


def validate_and_format_input(payload):
    """
    Validates a dictionary containing the seven input features.
    Returns:
        np.ndarray: 2D array of shape (1, 7) formatted in exact feature order.
    Raises:
        ValueError: Detailed description of any missing or invalid field.
    """
    if not isinstance(payload, dict):
        raise ValueError("Input payload must be a JSON object / dictionary.")

    # 1. Check for missing fields
    missing_fields = [f for f in INITIAL_FEATURES if f not in payload or payload[f] is None]
    if missing_fields:
        raise ValueError(f"Missing required input features: {', '.join(missing_fields)}")

    formatted_row = []

    # 2. Validate Year
    raw_year = str(payload["year"]).strip()
    if raw_year not in ALLOWED_CATEGORICAL_VALUES["year"]:
        raise ValueError(
            f"Invalid value for 'year': '{raw_year}'. Allowed values: {ALLOWED_CATEGORICAL_VALUES['year']}"
        )
    formatted_row.append(raw_year)

    # 3. Validate Location
    raw_loc = str(payload["location"]).strip()
    if raw_loc not in ALLOWED_CATEGORICAL_VALUES["location"]:
        raise ValueError(
            f"Invalid value for 'location': '{raw_loc}'. Allowed values: {ALLOWED_CATEGORICAL_VALUES['location']}"
        )
    formatted_row.append(raw_loc)

    # 4. Validate Bandwidth
    raw_bw = str(payload["bandwidth"]).strip()
    if raw_bw not in ALLOWED_CATEGORICAL_VALUES["bandwidth"]:
        raise ValueError(
            f"Invalid value for 'bandwidth': '{raw_bw}'. Allowed values: {ALLOWED_CATEGORICAL_VALUES['bandwidth']}"
        )
    formatted_row.append(raw_bw)

    # 5. Validate Adblock
    raw_adblock = payload["adblock"]
    if isinstance(raw_adblock, bool):
        adblock_str = "1" if raw_adblock else "0"
    else:
        adblock_str = str(raw_adblock).strip()
    if adblock_str not in ["0", "1"]:
        raise ValueError(
            f"Invalid value for 'adblock': '{raw_adblock}'. Allowed values: '0' (no adblock) or '1' (adblock active)"
        )
    formatted_row.append(adblock_str)

    # 6. Validate Throughput metrics (Bytes/s)
    for num_col in NUMERIC_FEATURES:
        val = payload[num_col]
        try:
            val_float = float(val)
        except (ValueError, TypeError):
            raise ValueError(
                f"Feature '{num_col}' must be a numeric value representing Bytes/s, got: {val}"
            )

        if val_float < 0:
            raise ValueError(f"Feature '{num_col}' must be non-negative (>= 0), got: {val_float}")

        formatted_row.append(val_float)

    return np.array([formatted_row], dtype=object)
