from pydantic import BaseModel


class ErrorType:
    conflict: str = "conflict"
    invalid: str = "invalid"

class Detail(BaseModel):
    message: str
    #invalid; conflict
    type: str
    field: str | None = None


class ErrorMessage(BaseModel):
    detail: Detail


def create_detail(message: str, type: str = ErrorType.invalid, field: str | None = None):
    return Detail(message=message, type=type, field=field).model_dump()

