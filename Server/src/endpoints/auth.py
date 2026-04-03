from fastapi import APIRouter, BackgroundTasks
from src.db.database import SessionDep
from src.db.redis import redis
from src.models.user import User, UserCredentials
from src.middle.user import get_user_by_email, hash_password
from src.middle.auth import send_mail_code
import sqlalchemy.exc as db_exception
from typing import Any
from fastapi import HTTPException, Response

router = APIRouter(prefix="/auth")

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

@router.post("/verify")
def verify_account(session: SessionDep, email: str, code:int):
    otp_check = redis.check_otp(email, code)
    if otp_check:
        user = get_user_by_email(session, email)
        if user:
            user.is_verified = True
            session.commit()
            return Response(status_code=200)
        #Should happen only if the email exists on Redis but not on DB
        raise HTTPException(404, "Usuário não encontrado")
    if otp_check == None:
        raise HTTPException(404, "Email não aguarda OTP ou expirou")
    raise HTTPException(400, "Código incorreto")



@router.post("/login")
def login(session: SessionDep):
    pass

@router.delete("/login")
def logout(session: SessionDep):
    pass
