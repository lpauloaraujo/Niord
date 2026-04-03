from ..db.database import Base
from .seguradora import Seguradora
from sqlalchemy import ForeignKey
from sqlalchemy.orm import mapped_column, Mapped
from sqlalchemy.types import Boolean
from pydantic import BaseModel, EmailStr, SecretStr
from typing import Annotated



class User(Base):
    __tablename__ = "user"
    
    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(nullable=False)
    email: Mapped[str] = mapped_column(nullable=False, unique=True)
    password: Mapped[str] = mapped_column(nullable=False)
    registration_plate: Mapped[str] = mapped_column(nullable=False)
    cpf: Mapped[str] = mapped_column(nullable=False, unique=True)
    telephone: Mapped[str] = mapped_column(nullable=False)
    blood_type: Mapped[str] = mapped_column(nullable=True)
    
    #For OTP
    is_verified: Mapped[bool] = mapped_column(nullable=False)



class UserCredentials(BaseModel):
    name: str
    email: EmailStr
    password: Annotated[str, SecretStr] #Doc formatting
    registration_plate: str
    cpf: str
    telephone: str
    blood_type: str | None = None



class User_Seguradora(Base):
    __tablename__ = "user_seguradora"

    id_user: Mapped[int] = mapped_column(ForeignKey(f"{User.__tablename__}.id"), primary_key=True)
    id_seguradora: Mapped[int] = mapped_column(ForeignKey(f"{Seguradora.__tablename__}.id"), primary_key=True)
    n_apolice: Mapped[str] = mapped_column(nullable=False, unique=True)
    cpf_cnpj: Mapped[str] = mapped_column(nullable=False)

