from fastapi import FastAPI
from contextlib import asynccontextmanager
from .models.db import create_tables, User, SessionDep
from .endpoints.api import api_router
from sqlalchemy import select

#Initialization
@asynccontextmanager
async def lifespan(app: FastAPI):
    create_tables()
    yield


app = FastAPI(lifespan=lifespan)
app.include_router(api_router)

@app.get("/")
def read_root():
    return {"Hello": "World"}

#Add to db demonstration
@app.get("/create_user/{name}")
def create_test(name: str, session: SessionDep):
    user = User(user_name=name)
    session.add(user)
    session.commit()
    session.refresh(user)
    return user

@app.get("/user")
def get_users(session: SessionDep):
    users = session.execute(select(User.user_name)).scalars().all()
    return users
