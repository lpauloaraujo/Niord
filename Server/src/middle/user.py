from ..db.database import SessionDep
from ..models.user import User
from pwdlib import PasswordHash


algo_hash = PasswordHash.recommended()

def hash_password(password: str) -> str:
    return algo_hash.hash(password)

def check_hash(password: str, hashed_password: str) -> bool:
    return algo_hash.verify(password, hashed_password)

def get_user_by_id(session: SessionDep, user_id: int):
    return session.get(User, user_id)

def get_user_by_email(session: SessionDep, email:str) -> User | None:
    return session.query(User).filter(User.email == email).one_or_none()


