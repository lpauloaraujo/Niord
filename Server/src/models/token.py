from ..db.database import Base
from .user import User
from sqlalchemy import ForeignKey
from sqlalchemy.orm import mapped_column, Mapped


class RefreshToken(Base):
    id: Mapped[int] = mapped_column(primary_key=True)
    token: Mapped[str] = mapped_column(nullable=False)
    id_user: Mapped[int] = mapped_column(ForeignKey(User.id), nullable=False)


class AccessToken:
    token: str
