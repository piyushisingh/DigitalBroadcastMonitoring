import os

# Base directory for backend
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

# Dataset paths
DATA_DIR = os.path.join(BASE_DIR, "ml", "data")
RAW_DATA_DIR = os.path.join(DATA_DIR, "raw")
DATASET_CSV_PATH = os.path.join(RAW_DATA_DIR, "session-dataset.csv")
DATASET_DESC_PATH = os.path.join(RAW_DATA_DIR, "session-dataset-description.txt")
AUDIT_REPORT_PATH = os.path.join(DATA_DIR, "audit_report.json")

# Model persistence paths
MODEL_DIR = os.path.join(BASE_DIR, "ml", "models")
MODEL_FILE_PATH = os.path.join(MODEL_DIR, "stalling_pipeline.joblib")
METADATA_FILE_PATH = os.path.join(MODEL_DIR, "stalling_metadata.json")

# 7 Primary Early-Session Predictors
INITIAL_FEATURES = [
    "year",
    "location",
    "bandwidth",
    "adblock",
    "dl_throughput_first1s",
    "dl_throughput_first5s",
    "dl_throughput_first10s",
]

CATEGORICAL_FEATURES = ["year", "location", "bandwidth", "adblock"]
NUMERIC_FEATURES = [
    "dl_throughput_first1s",
    "dl_throughput_first5s",
    "dl_throughput_first10s",
]

# Allowed categorical values for input validation
ALLOWED_CATEGORICAL_VALUES = {
    "year": ["2020", "2021"],
    "location": ["Wuerzburg", "Zagreb"],
    "bandwidth": ["unlimited", "1Mbps", "stochastic"],
    "adblock": ["0", "1", 0, 1],
}

# Group identifier for leak-free splitting
GROUP_COLUMN = "video"

# Target configuration
TARGET_COLUMN = "stalling_class"
TARGET_MAPPING = {
    "false": 0,
    "true": 1,
    False: 0,
    True: 1,
}
CLASS_NAMES = {
    0: "no_stalling",
    1: "stalling",
}

# Explicitly forbidden predictor columns (targets or group identifier)
FORBIDDEN_PREDICTORS = [
    "video",
    "stalling_class",
    "MOS_p1203",
    "startup_delay_class",
    "avg_resolution_class",
    "avg_bitrate_class",
]

# Reproducibility settings
RANDOM_SEED = 42
TRAIN_RATIO = 0.70
VAL_RATIO = 0.15
TEST_RATIO = 0.15

# Provenance metadata
DATASET_PROVENANCE = {
    "name": "MUEXLab YouTube QoE Session Dataset",
    "attribution": "Multimedia Experience Laboratory (MUEXLab), University of Zagreb & University of Würzburg (2020-2021)",
    "description": (
        "Measurement sessions capturing encrypted YouTube video streaming traffic under "
        "various network bandwidth constraints, adblock configurations, and measurement locations. "
        "Estimated QoE target is session stalling."
    ),
    "disclaimer": (
        "This model is trained on early-session video streaming QoE research data (initial 10s traffic). "
        "It predicts whether an encrypted video session will experience playback stalling. "
        "It does NOT detect physical television broadcast faults (e.g., RF transmission loss, black frames, "
        "or EBU R128 audio clipping) and the Android application does not automatically acquire live broadcast telemetry."
    ),
}
