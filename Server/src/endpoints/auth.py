from fastapi import APIRouter
from ..db.database import SessionDep
from ..db.redis import redis
from ..models.user import User
from ..middle.user import get_user_by_email, hash_password
import sqlalchemy.exc as db_exception
from typing import Any
from fastapi import HTTPException, Response

router = APIRouter(prefix="/auth")

@router.post("/register")
def register(session: SessionDep, register_data: dict[str, Any]):
    try:
        user = User(**register_data, is_verified=False)
        user.password = hash_password(user.password)
        session.add(user)
        session.commit()
    except db_exception.IntegrityError as e:
        #DEBUG
        print(e)
        raise HTTPException(status_code=409, detail="Erro de integridade dos dados")

    session.refresh(user)
    redis.create_otp(user.email)
    #DEBUG
    print(redis.client.get(f"otp:{user.email}"))
    #TO-DO send to user.email
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
