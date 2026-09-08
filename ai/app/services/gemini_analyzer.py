import json
import tempfile
from pathlib import Path

from app.core.config import settings
from gemini_analyze_image import DEFAULT_PROMPT, analyze_images, analyze_webpage


ALLOWED_IMAGE_TYPES = {
    "image/jpeg": ".jpg",
    "image/png": ".png",
    "image/webp": ".webp",
    "image/bmp": ".bmp",
}


def analyze_uploaded_images(
    uploaded_images: list[tuple[str, str, bytes]],
    prompt: str | None = None,
    model: str | None = None,
) -> dict:
    """업로드된 이미지 바이트를 임시 파일로 전달하고 즉시 정리한다."""
    if not uploaded_images:
        raise ValueError("분석할 이미지를 한 개 이상 업로드해 주세요.")
    if len(uploaded_images) > settings.max_image_count:
        raise ValueError(f"이미지는 최대 {settings.max_image_count}개까지 업로드할 수 있습니다.")

    selected_model = model or settings.gemini_model
    selected_prompt = prompt or DEFAULT_PROMPT
    with tempfile.TemporaryDirectory(prefix="gemini-property-") as temp_dir:
        image_paths = []
        for index, (filename, content_type, content) in enumerate(uploaded_images, start=1):
            if content_type not in ALLOWED_IMAGE_TYPES:
                raise ValueError(f"지원하지 않는 이미지 형식입니다: {filename} ({content_type})")
            if not content:
                raise ValueError(f"빈 이미지 파일입니다: {filename}")
            if len(content) > settings.max_image_bytes:
                limit_mb = settings.max_image_bytes // (1024 * 1024)
                raise ValueError(f"이미지 한 개의 크기는 {limit_mb}MB 이하여야 합니다: {filename}")
            path = Path(temp_dir) / f"image_{index}{ALLOWED_IMAGE_TYPES[content_type]}"
            path.write_bytes(content)
            image_paths.append(path)

        result_text = analyze_images(image_paths, selected_prompt, selected_model)
    return json.loads(result_text)


def analyze_property_url(
    url: str,
    prompt: str | None = None,
    model: str | None = None,
) -> dict:
    """Gemini URL Context로 공개 웹페이지 한 개를 분석한다."""
    selected_model = model or settings.gemini_model
    selected_prompt = prompt or DEFAULT_PROMPT
    return json.loads(analyze_webpage(url, selected_prompt, selected_model))
