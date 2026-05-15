from src.db.database import Base
from src.models.user import User
from sqlalchemy import ForeignKey
from sqlalchemy.orm import mapped_column, Mapped
from pydantic import BaseModel
from datetime import datetime
from typing import Annotated
from fastapi import Cookie


class RefreshToken(Base):
    __tablename__ = "refresh_token"

    id_user: Mapped[int] = mapped_column(ForeignKey(User.id), primary_key=True, nullable=False,
                                         autoincrement=False)
    token: Mapped[str] = mapped_column(nullable=False)


class TokenDecoded(BaseModel):
    id: int
    exp: datetime
    fresh: bool | None = None


class TokenLoginSchema(BaseModel):
    refresh_token: str
    access_token: str

class RefreshSchema(BaseModel):
    refresh: str | None = None
    access: str


class TokenCookies(BaseModel):
    refresh_token: Annotated[str, Cookie]
    access_token: Annotated[str, Cookie]

def get_token_cookies(
    access_token: Annotated[str, Cookie()],
    refresh_token: Annotated[str, Cookie()],
) -> TokenCookies:
    return TokenCookies(access_token=access_token, refresh_token=refresh_token)
