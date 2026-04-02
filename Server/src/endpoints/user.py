from fastapi import APIRouter
from ..db.database import SessionDep
from ..models.user import User
from sqlalchemy import select


router = APIRouter(prefix='/user')


@router.get("/")
def get_users(session: SessionDep):
    users = session.execute(select(User.name)).scalars().all()
    return users

@router.get("/{user_id}")
def get_user(session: SessionDep, user_id: int):
    user = session.get(User, user_id)
    return user
