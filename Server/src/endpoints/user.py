from fastapi import APIRouter
from ..db.database import SessionDep
from ..models.user import User
from sqlalchemy import select


router = APIRouter(prefix='/user')


@router.get("/{user_id}")
def get_user(session: SessionDep, user_id: int):
    #DEBUG
    user = session.get(User, user_id)
    return user
