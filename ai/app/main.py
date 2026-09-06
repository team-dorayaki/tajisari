from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.openapi.utils import get_openapi

from app.api.routes.health import router as health_router
from app.api.routes.image_analysis import router as analysis_router
from app.core.config import settings


app = FastAPI(
    title=settings.app_name,
    version="1.0.0",
    description="Gemini 기반 일본 부동산 매물 이미지 및 URL 분석 API",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health_router)
app.include_router(analysis_router, prefix=settings.api_prefix)


def custom_openapi() -> dict:
    """Swagger UI가 다중 UploadFile을 파일 선택기로 표시하도록 보정합니다."""
    if app.openapi_schema:
        return app.openapi_schema

    schema = get_openapi(
        title=app.title,
        version=app.version,
        description=app.description,
        routes=app.routes,
    )
    request_schema = schema["paths"][f"{settings.api_prefix}/analysis/images"]["post"][
        "requestBody"
    ]["content"]["multipart/form-data"]["schema"]

    if "$ref" in request_schema:
        schema_name = request_schema["$ref"].rsplit("/", 1)[-1]
        request_schema = schema["components"]["schemas"][schema_name]

    file_items = request_schema["properties"]["files"]["items"]
    file_items.pop("contentMediaType", None)
    file_items["type"] = "string"
    file_items["format"] = "binary"

    app.openapi_schema = schema
    return schema


app.openapi = custom_openapi
