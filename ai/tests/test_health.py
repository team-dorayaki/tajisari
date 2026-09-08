from fastapi.testclient import TestClient

from app.core.config import settings
from app.main import app


client = TestClient(app)


def test_health() -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {"status": "ok"}
    assert response.headers["X-Request-ID"]


def test_image_upload_openapi_uses_binary_files() -> None:
    schema = client.get("/openapi.json").json()
    request_schema = schema["paths"]["/api/v1/analysis/images"]["post"]["requestBody"][
        "content"
    ]["multipart/form-data"]["schema"]
    schema_name = request_schema["$ref"].rsplit("/", 1)[-1]
    file_schema = schema["components"]["schemas"][schema_name]["properties"]["files"]
    properties = schema["components"]["schemas"][schema_name]["properties"]

    assert file_schema["type"] == "array"
    assert file_schema["items"] == {"type": "string", "format": "binary"}
    assert set(properties) == {"files"}


def test_url_analysis_request_only_accepts_url() -> None:
    schema = client.get("/openapi.json").json()
    request_schema = schema["paths"]["/api/v1/analysis/url"]["post"]["requestBody"][
        "content"
    ]["application/json"]["schema"]
    schema_name = request_schema["$ref"].rsplit("/", 1)[-1]
    url_schema = schema["components"]["schemas"][schema_name]

    assert set(url_schema["properties"]) == {"url"}
    assert url_schema["required"] == ["url"]


def test_invalid_url_returns_standard_error() -> None:
    response = client.post("/api/v1/analysis/url", json={"url": "not-a-url"})

    assert response.status_code == 422
    assert response.json()["error"]["code"] == "INVALID_URL"
    assert response.json()["error"]["request_id"] == response.headers["X-Request-ID"]
    assert response.json()["error"]["retryable"] is False


def test_invalid_image_type_returns_standard_error() -> None:
    response = client.post(
        "/api/v1/analysis/images",
        files=[("files", ("property.txt", b"not an image", "text/plain"))],
    )

    assert response.status_code == 400
    assert response.json()["error"]["code"] == "INVALID_IMAGE_TYPE"
    assert response.json()["error"]["request_id"] == response.headers["X-Request-ID"]


def test_too_many_images_returns_standard_error() -> None:
    files = [
        ("files", (f"property-{index}.png", b"image", "image/png"))
        for index in range(settings.max_image_count + 1)
    ]
    response = client.post("/api/v1/analysis/images", files=files)

    assert response.status_code == 413
    assert response.json()["error"]["code"] == "TOO_MANY_IMAGES"
    assert response.json()["error"]["retryable"] is False
