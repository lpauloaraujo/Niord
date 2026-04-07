from fastapi import APIRouter, BackgroundTasks
from src.db.database import SessionDep
from src.db.redis import redis
from src.models.user import User, UserCredentials, UserLogin
from src.middle.user import get_user_by_email, hash_password, check_hash
from src.middle.auth import send_mail_code
from src.middle.auth import create_refresh_token, create_access_token, refresh_access_token, decode_token, delete_refresh_by_id, is_refresh_update_age, get_user_by_id, verify_refresh
import sqlalchemy.exc as db_exception
from typing import Annotated
from fastapi import HTTPException, Response, Depends
from fastapi.security import OAuth2PasswordBearer 
from pydantic import SecretStr

router = APIRouter(prefix="/auth")

oauth2_bearer = OAuth2PasswordBearer("auth/login", "auth/refresh")

@router.post("/register")
async def register(session: SessionDep, background: BackgroundTasks, userData: UserCredentials):
    try:
        data = userData.model_dump()
        user = User(**data, is_verified=False)
        user.password = hash_password(user.password)
        session.add(user)
        session.commit()
    except db_exception.IntegrityError as e:
        #DEBUG
        print(e)
        raise HTTPException(status_code=409, detail="Erro de integridade dos dados")

    session.refresh(user)
    
    otp_code = redis.create_otp(user.email)
    #Avoid response delay from sending the email
    background.add_task(send_mail_code, user.email, otp_code)

    return user 

@router.delete("/register")
def unregister(session: SessionDep):
    pass

@router.post("/resend", status_code=200)
def resend_otp(session: SessionDep, background: BackgroundTasks, email: str, password: Annotated[str, SecretStr]):
    user: User|None = get_user_by_email(session, email)
    if user and not user.is_verified and check_hash(password, user.password):
        otp_code = redis.create_otp(email)
        #Avoid response delay from sending the email
        background.add_task(send_mail_code, email, otp_code)
        return {"detail": "Código reenviado"}
    else:
        raise HTTPException(401, "Dados incorretos ou email não aguarda OTP")


@router.post("/verify", status_code=200)
def verify_account(session: SessionDep, email: str, code:int):
    otp_check = redis.check_otp(email, code)
    if otp_check:
        user = get_user_by_email(session, email)
        if user:
            user.is_verified = True
            session.commit()
            return {"detail": "Conta verificada com sucesso"}
        #Should happen only if the email exists on Redis but not on DB
        raise HTTPException(404, "Usuário não encontrado")
    if otp_check == None:
        raise HTTPException(404, "Email não aguarda OTP ou expirou")
    raise HTTPException(400, "Código incorreto")


@router.post("/login")
def login(session: SessionDep, user_form: Annotated[UserLogin, Depends()]):
    credential_exception = HTTPException(401, "Dados incorretos")
    user = get_user_by_email(session, user_form.email)
    if not user:
        raise credential_exception
    if not user.is_verified:
        raise HTTPException(401, "Verificação de conta pendente")
    if not check_hash(user_form.password, user.password):
        raise credential_exception

    refresh_token = create_refresh_token(session, user)
    access_token = create_access_token(user, fresh=True)

    session.commit()
    session.refresh(refresh_token)
    
    return {"refresh": refresh_token.token, "access": access_token}

@router.delete("/login", status_code=200)
def logout(session: SessionDep, refresh_token: str):
    decoded = decode_token(refresh_token)
    if decoded and verify_refresh(session, decoded, refresh_token):
        delete_refresh_by_id(session, decoded.id)
        session.commit()
        return {"detail":"Refresh token apagado com sucesso"}
    raise HTTPException(401, "Token inválido")
    

@router.post("/refresh")
def refresh(session: SessionDep, refresh_token: str, 
            user_form: Annotated[UserLogin, Depends()] | None = None):
    is_fresh = False
    if user_form is not None:
        user = get_user_by_email(session, user_form.email)
        if user and check_hash(user_form.password, user.password):
            is_fresh=True
            #DEBUG
            print("Creating fresh access token")
        else:
            raise HTTPException(401, "Credenciais incorretas")


    access_token = refresh_access_token(session, refresh_token, is_fresh)
    refresh_should_update = is_refresh_update_age(refresh_token)
    if access_token:
        data = dict()
        data["access"] = access_token
        
        if refresh_should_update:
            #Returns new refresh token if it's near expiring
            decoded = decode_token(refresh_token)
            if decoded:
                user = get_user_by_id(session, decoded.id)
                if user:
                    new_refresh = create_refresh_token(session, user)
                    session.commit()
                    session.refresh(new_refresh)
                    data["refresh"] = new_refresh.token

        return data
    raise HTTPException(401, "Token inválido")






