import logging

from fastapi import APIRouter, File, HTTPException, UploadFile
from fastapi.concurrency import run_in_threadpool

from app.core.config import settings
from app.schemas.image import AnalysisResponse, UrlAnalysisRequest
from app.services.gemini_analyzer import analyze_property_url, analyze_uploaded_images


router = APIRouter(prefix="/analysis", tags=["analysis"])
logger = logging.getLogger(__name__)


@router.post("/images", response_model=AnalysisResponse)
async def analyze_images_endpoint(
    files: list[UploadFile] = File(...),
) -> AnalysisResponse:
    try:
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
    except ValueError as error:
        logger.warning("이미지 분석 입력 오류: %s", error)
        raise HTTPException(status_code=400, detail=str(error)) from error
    except Exception as error:
        logger.exception("Gemini 이미지 분석 실패")
        raise HTTPException(status_code=502, detail=f"Gemini 이미지 분석 실패: {error}") from error
    finally:
        for file in files:
            await file.close()


@router.post("/url", response_model=AnalysisResponse)
async def analyze_url_endpoint(request: UrlAnalysisRequest) -> AnalysisResponse:
    try:
        logger.info("URL 분석 요청 host=%s", request.url.host or "unknown")
        selected_model = settings.gemini_model
        result = await run_in_threadpool(
            analyze_property_url, str(request.url), None, selected_model
        )
        return AnalysisResponse(input_type="url", model=selected_model, result=result)
    except ValueError as error:
        logger.warning("URL 분석 입력 오류: %s", error)
        raise HTTPException(status_code=400, detail=str(error)) from error
    except Exception as error:
        logger.exception("Gemini URL 분석 실패")
        raise HTTPException(status_code=502, detail=f"Gemini URL 분석 실패: {error}") from error
