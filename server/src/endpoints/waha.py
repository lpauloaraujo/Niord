from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
import httpx

from src.models.error import ErrorMessage, create_detail, ErrorType
from src.config import get_settings

router = APIRouter(prefix="/whatsapp")
WAHA_BASE_URL = get_settings().WAHA_BASE_URL


class WhatsAppMessageRequest(BaseModel):
    phoneNumber: str
    message: str

class WhatsAppSendResponse(BaseModel):
    detail: str

class WhatsAppLocRequest(WhatsAppMessageRequest):
    title: str
    latitude: float
    longitude: float

@router.post(
    "/send-text",
    status_code=200,
    responses={
        500: {"model": ErrorMessage}
    }
)
async def send_text(payload: WhatsAppMessageRequest):

    chat_id = f"{payload.phoneNumber}@c.us"

    try:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{WAHA_BASE_URL}/api/sendText",
                json={
                    "session": "default",
                    "chatId": chat_id,
                    "text": payload.message
                },
                timeout=10
            )

        if response.status_code >= 400:
            raise HTTPException(
                status_code=500,
                detail=create_detail(
                    message="Erro ao enviar mensagem via WAHA",
                    type=ErrorType.external
                )
            )

        return {"detail": "Mensagem enviada com sucesso"}

    except Exception as e:
        print(e)
        raise HTTPException(
            status_code=500,
            detail=create_detail(
                message="Falha inesperada ao enviar mensagem",
                type=ErrorType.external
            )
        )


@router.post(
    "/sendLoc",
    status_code=200,
    responses={
        500: {"model": ErrorMessage}
    }
)
async def send_loc(payload: WhatsAppLocRequest):

    chat_id = f"{payload.phoneNumber}@c.us"

    try:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{WAHA_BASE_URL}/api/sendLocation",
                json={
                    "session": "default",
                    "chatId": chat_id,
                    "title": payload.title + '\n' + payload.message,
                    "latitude": payload.latitude,
                    "longitude": payload.longitude
                },
                timeout=10
            )

        if response.status_code >= 400:
            raise HTTPException(
                status_code=500,
                detail=create_detail(
                    message="Erro ao enviar mensagem via WAHA",
                    type=ErrorType.external
                )
            )

        return {"detail": "Mensagem enviada com sucesso"}

    except Exception:
        raise HTTPException(
            status_code=500,
            detail=create_detail(
                message="Falha inesperada ao enviar mensagem",
                type=ErrorType.external
            )
        )
