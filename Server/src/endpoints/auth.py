from fastapi import APIRouter
from ..db.database import SessionDep
from ..db.redis import redis_engine
from ..models.user import User
from sqlalchemy import select
from typing import Any
from random import randint

router = APIRouter(prefix="/auth")

@router.post("/register")
def register(session: SessionDep, register_data: dict[str, Any]):
    #TO-DO token generation and error handling
    user = User(**register_data, is_verified=False)
    session.add(user)
    session.commit()
    session.refresh(user)
    #TO-DO Secure code generation
    redis_engine.set(str(user.id), randint(100000, 999999))
    print(redis_engine.get(str(user.id)))
    return user 

@router.delete("/register")
def unregister(session: SessionDep):
    pass

@router.post("/verify")
def verify_account(session: SessionDep, id: int, code:int):
    #TO-DO better redis organization
    redis_code = redis_engine.get(str(id))
    if str(code) == redis_code:
        user = session.get(User, id)
        if user:
            user.is_verified = True
            session.commit()
            session.refresh(user)
        return user


@router.post("/login")
def login(session: SessionDep):
    pass

@router.delete("/login")
def logout(session: SessionDep):
    pass
