from fastapi_mail import ConnectionConfig, FastMail, MessageSchema, MessageType
from ..config import get_settings 
from pydantic import NameEmail


settings = get_settings()
conf = ConnectionConfig(
        MAIL_FROM_NAME=settings.MAIL_FROM_NAME,
        MAIL_USERNAME= settings.MAIL_USERNAME,
        MAIL_PASSWORD=settings.MAIL_PASSWORD,
        MAIL_PORT=settings.MAIL_PORT,
        MAIL_FROM=settings.MAIL_FROM,
        MAIL_SERVER=settings.MAIL_SERVER,
        MAIL_STARTTLS = settings.MAIL_STARTTLS,
        MAIL_SSL_TLS = settings.MAIL_SSL_TLS,
        USE_CREDENTIALS = settings.USE_CREDENTIALS,
        VALIDATE_CERTS = settings.VALIDATE_CERTS
)

fast_mail = FastMail(conf)

async def send_mail_code(email_str: str, code: int):
    email = NameEmail._validate(email_str)
    
    #TO-DO email body html
    message = MessageSchema(
            subject="Código de confirmação",
            recipients=[email],
            body=str(code),
            subtype=MessageType.html
            )
    await fast_mail.send_message(message)
    return True
