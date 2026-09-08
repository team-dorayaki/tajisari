import logging
import time
import uuid

from fastapi import FastAPI
from fastapi import Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.openapi.utils import get_openapi

from app.api.routes.health import router as health_router
from app.api.routes.image_analysis import router as analysis_router
from app.core.config import settings
from app.core.logging_config import configure_logging, request_id_context


configure_logging()
logger = logging.getLogger(__name__)
access_logger = logging.getLogger("app.access")


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


@app.middleware("http")
async def log_request(request: Request, call_next):
    request_id = request.headers.get("X-Request-ID") or uuid.uuid4().hex[:12]
    token = request_id_context.set(request_id)
    started = time.perf_counter()
    status_code = 500

    try:
        response = await call_next(request)
        status_code = response.status_code
        response.headers["X-Request-ID"] = request_id
        return response
    except Exception:
        logger.exception("처리되지 않은 HTTP 요청 오류")
        raise
    finally:
        elapsed = time.perf_counter() - started
        access_logger.info(
            "%s %s status=%s duration=%.3fs client=%s",
            request.method,
            request.url.path,
            status_code,
            elapsed,
            request.client.host if request.client else "unknown",
        )
        request_id_context.reset(token)


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
