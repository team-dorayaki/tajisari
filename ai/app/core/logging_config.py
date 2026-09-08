import contextvars
import logging
from logging.handlers import RotatingFileHandler
from pathlib import Path


request_id_context: contextvars.ContextVar[str] = contextvars.ContextVar(
    "request_id", default="-"
)


class RequestIdFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        record.request_id = request_id_context.get()
        return True


def _file_handler(path: Path, level: int) -> RotatingFileHandler:
    handler = RotatingFileHandler(
        path,
        maxBytes=10 * 1024 * 1024,
        backupCount=5,
        encoding="utf-8",
    )
    handler.setLevel(level)
    handler.addFilter(RequestIdFilter())
    return handler


def configure_logging() -> None:
    """터미널과 logs 폴더에 애플리케이션 로그를 기록합니다."""
    project_dir = Path(__file__).resolve().parents[2]
    log_dir = project_dir / "logs"
    log_dir.mkdir(parents=True, exist_ok=True)

    formatter = logging.Formatter(
        "%(asctime)s | %(levelname)s | %(name)s | "
        "request_id=%(request_id)s | %(message)s"
    )

    console = logging.StreamHandler()
    console.setLevel(logging.INFO)
    console.addFilter(RequestIdFilter())

    app_file = _file_handler(log_dir / "app.log", logging.INFO)
    error_file = _file_handler(log_dir / "error.log", logging.ERROR)
    access_file = _file_handler(log_dir / "access.log", logging.INFO)

    for handler in (console, app_file, error_file, access_file):
        handler.setFormatter(formatter)

    root_logger = logging.getLogger()
    root_logger.setLevel(logging.INFO)
    root_logger.handlers.clear()
    root_logger.addHandler(console)
    root_logger.addHandler(app_file)
    root_logger.addHandler(error_file)

    access_logger = logging.getLogger("app.access")
    access_logger.handlers.clear()
    access_logger.addHandler(access_file)
    access_logger.setLevel(logging.INFO)
    access_logger.propagate = False
