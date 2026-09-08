from pydantic import BaseModel


class ErrorDetail(BaseModel):
    code: str
    message: str
    request_id: str
    retryable: bool


class ErrorResponse(BaseModel):
    error: ErrorDetail
