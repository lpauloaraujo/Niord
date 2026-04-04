from fastapi import APIRouter
from src.db.database import SessionDep
from src.models.user import User
from sqlalchemy import select
from src.models.token import TokenDecoded
from src.middle.auth import decode_token
from fastapi import HTTPException


router = APIRouter(prefix='/user')


@router.get("/{user_id}")
def get_user(session: SessionDep, access_token: str):
    decoded = decode_token(access_token)
    if decoded:
        #TO-DO return Model and not entire database entry
        user = session.get(User, decoded.id)
        return user
    raise HTTPException(401, "Token inválido")
