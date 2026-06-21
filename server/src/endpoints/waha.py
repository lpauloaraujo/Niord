from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
import httpx

from src.models.error import ErrorMessage, create_detail, ErrorType

router = APIRouter(prefix="/whatsapp")

WAHA_BASE_URL = "http://localhost:3000"

class WhatsAppMessageRequest(BaseModel):
    phoneNumber: str
    message: str

class WhatsAppSendResponse(BaseModel):
    detail: str

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

    except Exception:
        raise HTTPException(
            status_code=500,
            detail=create_detail(
                message="Falha inesperada ao enviar mensagem",
                type=ErrorType.external
            )
        )
