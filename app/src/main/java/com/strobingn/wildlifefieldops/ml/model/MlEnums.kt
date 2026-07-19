package com.strobingn.wildlifefieldops.ml.model

/** Backend that produced a vision or fused prediction. */
enum class ModelBackend {
    ML_KIT_LABELING,
    ML_KIT_OBJECT,
    TFLITE_SPECIES_DAMAGE_V1,
    CLOUD_VLM,
    FUSION
}

/** What a prediction or training label is about. */
enum class PredictionTarget {
    SPECIES,
    DAMAGE,
    SEVERITY,
    SERVICE_TYPE,
    PRIORITY
}

/** Last writer for a draft field (user always wins on commit). */
enum class FieldProvenance {
    USER,
    VOICE_NLU,
    VISION,
    FUSION,
    LLM,
    GPS,
    SYSTEM_DEFAULT
}

enum class CaptureSessionStatus {
    DRAFT,
    REVIEW,
    COMMITTED,
    DISCARDED
}

enum class LabelSource {
    /** Tech accepted the model suggestion as-is. */
    MODEL_ACCEPTED,
    /** Tech changed the model suggestion. */
    MODEL_CORRECTED,
    /** Tech set a label with no model suggestion. */
    USER_ONLY,
    IMPORTED
}

/** Confidence gates for auto-fill / review / cloud escalate. */
object MlThresholds {
    const val AUTO_ACCEPT = 0.82f
    const val SHOW_SUGGESTION = 0.55f
    const val CLOUD_ESCALATE = 0.55f
    const val HARD_REJECT = 0.35f
}
