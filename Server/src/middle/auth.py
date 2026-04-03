import httpx
from ..config import get_settings 
from pydantic import NameEmail


async def send_mail_code(email_str: str, code: int):
    recipient_mail = NameEmail._validate(email_str).email
    response = await send_mail([recipient_mail], str(code))
    return response 



async def send_mail(email_recipients: list[str], message: str):
    async with httpx.AsyncClient() as client:
        response = await client.post(
                get_settings().EMAIL_URL,
                headers={"Authorization": f"Bearer {get_settings().EMAIL_API_KEY}"},
                json={
                    "from": {"email":get_settings().EMAIL_FROM, 
                             "name": get_settings().EMAIL_NAME},
                    "to": [{"email": e for e in email_recipients}],
                    "subject": "Código de confirmação",
                    "text": message
                    }
                ) 
    return response 

