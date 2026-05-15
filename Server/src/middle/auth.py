import httpx
from src.config import get_settings 
from pydantic import NameEmail, ValidationError
from src.models.token import RefreshToken, TokenDecoded, RefreshSchema, TokenCookies, get_token_cookies
from src.models.user import User, UserCredentials
from src.models.error import create_detail
from src.middle.user import get_user_by_id
from src.db.database import SessionDep
import jwt
from datetime import timedelta, timezone, datetime
from fastapi import Response, HTTPException, status, Cookie, Depends
from typing import Annotated

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


def add_user_db(session: SessionDep, userData: UserCredentials, verified: bool = False):
    data = userData.model_dump()
    user = User(**data, is_verified=verified)
    session.add(user)
    return user


def create_access_token(user: User, fresh: bool = False) -> str:
    encoded = encode_token(user.id, datetime.now(timezone.utc) + 
                           timedelta(seconds=get_settings().TOKEN_EXPIRE_SECONDS), 
                           fresh)
    return encoded


def create_refresh_token(session: SessionDep, user: User) -> RefreshToken:
    delete_refresh_by_id(session, user.id)
    encoded = encode_token(user.id, 
                           datetime.now(timezone.utc) + timedelta(days=get_settings().REFRESH_EXPIRE_DAYS), 
                           None)
    refresh_token = RefreshToken(id_user=user.id, token=encoded)
    session.add(refresh_token)
    return refresh_token
    
def delete_refresh_token(session: SessionDep, token_instance: RefreshToken | None):
    if token_instance:
        session.delete(token_instance)

def delete_refresh_by_id(session: SessionDep, user_id: int):
    refresh_token = session.get(RefreshToken, user_id)
    delete_refresh_token(session, refresh_token)

def refresh_access_token(session: SessionDep, refresh_token: str) -> str | None:
    decoded = decode_token(refresh_token)
    if decoded and verify_refresh(session, decoded, refresh_token):
        user = get_user_by_id(session, decoded.id)
        if user:
            return create_access_token(user, False)
    return None

def is_refresh_update_age(refresh_token: str):

    decoded = decode_token(refresh_token)
    if decoded:
        delta = decoded.exp - datetime.now(timezone.utc)
        if delta.seconds < 0:
            return False
        if delta.days < get_settings().REFRESH_UPDATE_DAYS:
            return True
    return False


def encode_token(id: int, exp: datetime, fresh: bool | None):
    data = {"id": id, "exp": exp}
    if fresh:
        data["fresh"] = fresh

    return jwt.encode(data, get_settings().SECRET_KEY, algorithm=get_settings().ALGO)

def decode_token(token: str) -> TokenDecoded | None:
    try:
        decoded = TokenDecoded(
                **jwt.decode(
                    token, 
                    get_settings().SECRET_KEY, 
                    algorithms=[get_settings().ALGO]
                    )
                )
        return decoded
    except jwt.InvalidTokenError as e:
        print(e)
        return None


def verify_refresh(session: SessionDep, decoded_refresh: TokenDecoded, encoded: str) -> bool:
    db_token = session.get(RefreshToken,  decoded_refresh.id)
    if db_token and db_token.token == encoded:
        return True
    return False


def refresh(session: SessionDep, refresh_token: str) -> RefreshSchema:

    access_token = refresh_access_token(session, refresh_token)
    refresh_should_update = is_refresh_update_age(refresh_token)
    if access_token:
        data = dict()
        data["access"] = access_token
        
        if refresh_should_update:
            #Returns new refresh token if it's near expiring
            decoded = decode_token(refresh_token)
            if decoded:
                user = get_user_by_id(session, decoded.id)
                if not verify_refresh(session, decoded, refresh_token):
                    raise ValidationError(create_detail("Token Inválido", field="refresh_token"))
                if user:
                    new_refresh = create_refresh_token(session, user)
                    session.commit()
                    session.refresh(new_refresh)
                    data["refresh"] = new_refresh.token
            raise ValidationError(create_detail("Token Inválido", field="refresh_token"))

        return RefreshSchema(**data)
    raise Exception(create_detail("Token inválido", field="refresh_token"))

class TokenGuard:

    def __call__(self, session: SessionDep, response: Response, tokens: TokenCookies = Depends(get_token_cookies)) -> TokenDecoded:
        access_token = tokens.access_token  
        
        try:
            decoded = decode_token(access_token)
            if decoded is None:
                refresh_data = refresh(session, tokens.refresh_token)
                if refresh_data.refresh is not None:
                    response.set_cookie("refresh_token", refresh_data.refresh)
                response.set_cookie("access_token", refresh_data.access)
                new_decoded = decode_token(refresh_data.access)
                if new_decoded is not None:
                    return new_decoded
                raise Exception(create_detail("Token inválido", field="access_token"))
            else:
                return decoded
              
                

        except Exception as e:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail = e.args
                )



