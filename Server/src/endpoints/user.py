from fastapi import APIRouter
from src.db.database import SessionDep
from src.models.user import User
from sqlalchemy import select
from src.models.token import TokenDecoded
from src.middle.auth import decode_token, TokenGuard
from fastapi import HTTPException, Cookie, Depends
from typing import Annotated


router = APIRouter(prefix='/user')
allow_authenticated = TokenGuard()


@router.get("/")
def get_user(session: SessionDep, access_token_decoded: TokenDecoded = Depends(allow_authenticated)):
    user = session.get(User, access_token_decoded.id)
    return user
