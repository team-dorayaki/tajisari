from typing import Any

from pydantic import BaseModel, HttpUrl


class UrlAnalysisRequest(BaseModel):
    url: HttpUrl


class AnalysisResponse(BaseModel):
    input_type: str
    model: str
    result: dict[str, Any]
