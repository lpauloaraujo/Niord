from fastapi import Depends
from typing import Annotated, List, Optional
from ..config import get_settings
from sqlalchemy import create_engine, ForeignKey
from sqlalchemy.orm import DeclarativeBase, mapped_column, Mapped, relationship
from sqlalchemy.orm import Session


engine = create_engine(get_settings().DB_URL, echo=True, connect_args={"check_same_thread": False})

class Base(DeclarativeBase):
    pass


def create_tables():
    Base.metadata.create_all(engine)

def get_session():
    with Session(engine) as session:
        yield session

SessionDep = Annotated[Session, Depends(get_session)]

#Example, classes should be in different files in most cases
class User(Base):
    __tablename__ = "user"
    
    id: Mapped[int] = mapped_column(primary_key=True)
    user_name: Mapped[str] = mapped_column(nullable=False)
    

    #relationship for ORM operations
    #One To Many
    items: Mapped[List["Item"]] = relationship(back_populates="user")



class Item(Base):
    __tablename__ = "item"
       
    id: Mapped[int] = mapped_column(primary_key=True)
    item_name: Mapped[str] = mapped_column(nullable=False)
    user_id: Mapped[int] = mapped_column(ForeignKey("user.id"))
   
    #continuation
    user: Mapped[User] = relationship(back_populates="items")


