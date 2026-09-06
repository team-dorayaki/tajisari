import logging
from dataclasses import dataclass
from typing import Any

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException as StarletteHTTPException

from app.core.logging_config import request_id_context


logger = logging.getLogger(__name__)


@dataclass
class AppError(Exception):
    code: str
    message: str
    status_code: int = 400
    retryable: bool = False

    def __str__(self) -> str:
        return self.message


def error_response(error: AppError) -> JSONResponse:
    return JSONResponse(
        status_code=error.status_code,
        content={
            "error": {
                "code": error.code,
                "message": error.message,
                "request_id": request_id_context.get(),
                "retryable": error.retryable,
            }
        },
    )


def _status_code(error: Exception) -> int | None:
    for value in (
        getattr(error, "status_code", None),
        getattr(error, "code", None),
        getattr(getattr(error, "response", None), "status_code", None),
    ):
        if isinstance(value, int):
            return value
    return None


def classify_gemini_error(error: Exception, input_type: str) -> AppError:
    """SDK 버전과 무관하게 Gemini 오류를 프론트엔드용 코드로 변환합니다."""
    if isinstance(error, AppError):
        return error

    message = str(error)
    lowered = message.lower()
    status = _status_code(error)

    if "gemini_response_truncated" in lowered:
        return AppError(
            "GEMINI_RESPONSE_TRUNCATED",
            "Gemini 응답이 출력 한도에 도달해 완성되지 않았습니다. 다시 시도해 주세요.",
            502,
            True,
        )
    if "url_context_paywall" in lowered:
        return AppError(
            "URL_CONTEXT_PAYWALL",
            "유료 구독이 필요한 웹페이지는 분석할 수 없습니다.",
            403,
            False,
        )
    if "url_context_unsafe" in lowered:
        return AppError(
            "URL_CONTEXT_UNSAFE",
            "안전 정책으로 인해 해당 웹페이지에 접근할 수 없습니다.",
            403,
            False,
        )
    if "url_context_failed" in lowered:
        return AppError(
            "URL_CONTEXT_FAILED",
            "웹페이지 내용을 가져오지 못했습니다. 공개 URL인지 확인해 주세요.",
            502,
            True,
        )
    if "gemini_api_key" in lowered and "환경 변수" in message:
        return AppError(
            "API_KEY_MISSING",
            "서버에 Gemini API 키가 설정되지 않았습니다.",
            500,
            False,
        )
    if status in {401, 403} or any(
        keyword in lowered for keyword in ("api key not valid", "unauthorized", "forbidden")
    ):
        return AppError(
            "GEMINI_AUTH_ERROR",
            "Gemini 인증에 실패했습니다. 서버 설정을 확인해 주세요.",
            502,
            False,
        )
    if status == 429 or any(
        keyword in lowered for keyword in ("rate limit", "resource_exhausted", "quota")
    ):
        return AppError(
            "GEMINI_RATE_LIMIT",
            "Gemini 요청 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.",
            429,
            True,
        )
    if status in {408, 504} or "timeout" in lowered or "timed out" in lowered:
        return AppError(
            "GEMINI_TIMEOUT",
            "Gemini 응답 시간이 초과됐습니다. 잠시 후 다시 시도해 주세요.",
            504,
            True,
        )
    if any(
        keyword in message
        for keyword in (
            "JSON으로 해석할 수 없습니다",
            "필수 영역이 없습니다",
            "필수 필드가 없습니다",
            "분석 결과가 없습니다",
        )
    ):
        return AppError(
            "INVALID_AI_RESPONSE",
            "Gemini 응답 형식이 올바르지 않습니다. 다시 시도해 주세요.",
            502,
            True,
        )
    if input_type == "url" and any(
        keyword in lowered for keyword in ("url context", "url_context", "retrieve")
    ):
        return AppError(
            "URL_CONTEXT_FAILED",
            "웹페이지 내용을 가져오지 못했습니다. 공개 URL인지 확인해 주세요.",
            502,
            True,
        )
    return AppError(
        "GEMINI_API_ERROR",
        "Gemini 분석 중 오류가 발생했습니다.",
        502,
        True,
    )


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(AppError)
    async def handle_app_error(_: Request, error: AppError) -> JSONResponse:
        log = logger.error if error.status_code >= 500 else logger.warning
        log("요청 처리 오류 code=%s message=%s", error.code, error.message)
        return error_response(error)

    @app.exception_handler(RequestValidationError)
    async def handle_validation_error(
        _: Request, error: RequestValidationError
    ) -> JSONResponse:
        fields = [".".join(str(item) for item in detail["loc"]) for detail in error.errors()]
        if "body.url" in fields:
            app_error = AppError(
                "INVALID_URL",
                "분석할 웹페이지 URL 형식이 올바르지 않습니다.",
                422,
                False,
            )
        elif "body.files" in fields:
            app_error = AppError(
                "IMAGE_REQUIRED",
                "분석할 이미지를 한 개 이상 업로드해 주세요.",
                422,
                False,
            )
        else:
            app_error = AppError(
                "INVALID_REQUEST",
                "요청 형식이 올바르지 않습니다. 확인할 필드: " + ", ".join(fields),
                422,
                False,
            )
        logger.warning("요청 검증 실패 fields=%s", fields)
        return error_response(app_error)

    @app.exception_handler(StarletteHTTPException)
    async def handle_http_error(_: Request, error: StarletteHTTPException) -> JSONResponse:
        code = "NOT_FOUND" if error.status_code == 404 else "HTTP_ERROR"
        message = "요청한 API를 찾을 수 없습니다." if error.status_code == 404 else str(error.detail)
        return error_response(AppError(code, message, error.status_code, False))

    @app.exception_handler(Exception)
    async def handle_unexpected_error(_: Request, error: Exception) -> JSONResponse:
        logger.exception("처리되지 않은 서버 오류", exc_info=error)
        return error_response(
            AppError(
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다.",
                500,
                True,
            )
        )
