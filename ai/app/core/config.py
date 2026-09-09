import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    app_name: str = "Japanese Property Analyzer"
    api_prefix: str = "/api/v1"
    gemini_model: str = os.getenv("GEMINI_MODEL", "gemini-3.5-flash-lite")
    analysis_mode: str = os.getenv("AI_ANALYSIS_MODE", "dual36")
    fixed_model: str = os.getenv("GEMINI_FIXED_MODEL", "gemini-3.5-flash-lite")
    cost_model: str | None = os.getenv("GEMINI_COST_MODEL")
    max_image_count: int = int(os.getenv("MAX_IMAGE_COUNT", "5"))
    max_image_bytes: int = int(os.getenv("MAX_IMAGE_BYTES", str(15 * 1024 * 1024)))


settings = Settings()
