from fastapi import APIRouter
from ..models.db import SessionDep
from ..models.user import User
from sqlalchemy import select


router = APIRouter(prefix='/user')


#Add to db demonstration
@router.get("/create_user/{name}")
def create_test(name: str, session: SessionDep):
    #user = User(name=name)
    #session.add(user)
    #session.commit()
    #session.refresh(user)
    #return user
    pass

@router.get("/")
def get_users(session: SessionDep):
    users = session.execute(select(User.name)).scalars().all()
    return users

@router.get("/{user_id}")
def get_user(session: SessionDep, user_id: int):
    user = session.execute(select(User.id).where(User.id == user_id)).one_or_none()
    return user
