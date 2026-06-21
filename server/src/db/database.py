from fastapi import Depends
from typing import Annotated
from src.config import get_settings
from sqlalchemy import create_engine
from sqlalchemy.orm import DeclarativeBase 
from sqlalchemy.orm import Session
from sqlalchemy.pool import StaticPool

if get_settings().DEBUG:
    engine = create_engine(get_settings().DB_URL, echo=True, 
                       connect_args={"check_same_thread": False},
                       poolclass= StaticPool)
else:
    engine = create_engine(get_settings().DB_URL, echo=True)


class Base(DeclarativeBase):
    pass


def create_tables():
    Base.metadata.create_all(engine)

def get_session():
    with Session(engine) as session:
        yield session

SessionDep = Annotated[Session, Depends(get_session)]

#Example, classes should be in different files in most cases


