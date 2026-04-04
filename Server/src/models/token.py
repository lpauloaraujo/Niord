from src.db.database import Base
from src.models.user import User
from sqlalchemy import ForeignKey
from sqlalchemy.orm import mapped_column, Mapped
from pydantic import BaseModel
from datetime import datetime


class RefreshToken(Base):
    __tablename__ = "refresh_token"

    id_user: Mapped[int] = mapped_column(ForeignKey(User.id), primary_key=True, nullable=False,
                                         autoincrement=False)
    token: Mapped[str] = mapped_column(nullable=False)


class TokenDecoded(BaseModel):
    id: int
    exp: datetime
    fresh: bool | None = None
