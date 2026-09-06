from fastapi import APIRouter, File, HTTPException, UploadFile
from fastapi.concurrency import run_in_threadpool

from app.core.config import settings
from app.schemas.image import AnalysisResponse, UrlAnalysisRequest
from app.services.gemini_analyzer import analyze_property_url, analyze_uploaded_images


router = APIRouter(prefix="/analysis", tags=["analysis"])


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
        selected_model = settings.gemini_model
        result = await run_in_threadpool(
            analyze_uploaded_images, uploaded_images, None, selected_model
        )
        return AnalysisResponse(input_type="images", model=selected_model, result=result)
    except ValueError as error:
        raise HTTPException(status_code=400, detail=str(error)) from error
    except Exception as error:
        raise HTTPException(status_code=502, detail=f"Gemini 이미지 분석 실패: {error}") from error
    finally:
        for file in files:
            await file.close()


@router.post("/url", response_model=AnalysisResponse)
async def analyze_url_endpoint(request: UrlAnalysisRequest) -> AnalysisResponse:
    try:
        selected_model = settings.gemini_model
        result = await run_in_threadpool(
            analyze_property_url, str(request.url), None, selected_model
        )
        return AnalysisResponse(input_type="url", model=selected_model, result=result)
    except ValueError as error:
        raise HTTPException(status_code=400, detail=str(error)) from error
    except Exception as error:
        raise HTTPException(status_code=502, detail=f"Gemini URL 분석 실패: {error}") from error
