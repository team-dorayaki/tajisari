import logging

from fastapi import APIRouter, File, UploadFile
from fastapi.concurrency import run_in_threadpool

from app.core.config import settings
from app.core.errors import AppError, classify_gemini_error
from app.schemas.error import ErrorResponse
from app.schemas.image import AnalysisResponse, UrlAnalysisRequest
from app.services.gemini_analyzer import analyze_property_url, analyze_uploaded_images


router = APIRouter(prefix="/analysis", tags=["analysis"])
logger = logging.getLogger(__name__)
ERROR_RESPONSES = {
    400: {"model": ErrorResponse, "description": "잘못된 입력"},
    413: {"model": ErrorResponse, "description": "이미지 제한 초과"},
    422: {"model": ErrorResponse, "description": "요청 형식 오류"},
    429: {"model": ErrorResponse, "description": "Gemini 요청 한도 초과"},
    500: {"model": ErrorResponse, "description": "서버 설정 또는 내부 오류"},
    502: {"model": ErrorResponse, "description": "Gemini 또는 URL Context 오류"},
    504: {"model": ErrorResponse, "description": "Gemini 응답 시간 초과"},
}


@router.post("/images", response_model=AnalysisResponse, responses=ERROR_RESPONSES)
async def analyze_images_endpoint(
    files: list[UploadFile] = File(...),
) -> AnalysisResponse:
    try:
        if len(files) > settings.max_image_count:
            raise AppError(
                "TOO_MANY_IMAGES",
                f"이미지는 최대 {settings.max_image_count}개까지 업로드할 수 있습니다.",
                413,
                False,
            )
        uploaded_images = []
        for file in files:
            uploaded_images.append(
                (file.filename or "image", file.content_type or "", await file.read())
            )
        logger.info(
            "이미지 분석 요청 image_count=%d total_bytes=%d",
            len(uploaded_images),
            sum(len(image[2]) for image in uploaded_images),
        )
        selected_model = settings.gemini_model
        result = await run_in_threadpool(
            analyze_uploaded_images, uploaded_images, None, selected_model
        )
        return AnalysisResponse(input_type="images", model=selected_model, result=result)
    except AppError:
        raise
    except Exception as error:
        logger.exception("Gemini 이미지 분석 실패")
        raise classify_gemini_error(error, "images") from error
    finally:
        for file in files:
            await file.close()


@router.post("/url", response_model=AnalysisResponse, responses=ERROR_RESPONSES)
async def analyze_url_endpoint(request: UrlAnalysisRequest) -> AnalysisResponse:
    try:
        logger.info("URL 분석 요청 host=%s", request.url.host or "unknown")
        selected_model = settings.gemini_model
        result = await run_in_threadpool(
            analyze_property_url, str(request.url), None, selected_model
        )
        return AnalysisResponse(input_type="url", model=selected_model, result=result)
    except AppError:
        raise
    except Exception as error:
        logger.exception("Gemini URL 분석 실패")
        raise classify_gemini_error(error, "url") from error
