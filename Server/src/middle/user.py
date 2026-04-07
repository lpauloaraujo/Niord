from src.db.database import SessionDep
from src.models.user import User
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

def get_user_by_cpf(session: SessionDep, cpf:str) -> User | None:
    return session.query(User).filter(User.cpf == cpf).one_or_none()

def is_valid_cpf(cpf: str) -> bool:
    splitted = split_cpf(cpf)
    if not check_cpf_format(splitted):
        return False
    if not check_cpf_validity(splitted):
        return False
    return True

def split_cpf(cpf: str):
    #Split dots
    l = cpf.split(".")
    #Split hifen in last division
    v = l[-1].split("-")
    #Separate last division into two
    if len(v) == 2:
        l[-1] = v[0]
        l.append(v[1])
    return l

def check_cpf_format(cpf: list[str]):
    #Assume splitted with split_cpf
    if len(cpf) == 4:
        for i in range(0, 3):
            if len(cpf[i]) != 3:
                return False
        if len(cpf[-1]) != 2:
            return False
        return True
    
    return False

def check_cpf_validity(cpf: list[str]) -> bool:
    #Assume right format and splitted

    #Check first for repeated characters
    full_cpf = "".join(cpf)
    is_repeating = True
    for i in range(1, len(full_cpf)):
        if full_cpf[i] != full_cpf[i-1]:
            is_repeating = False
    if is_repeating:
        return False


    #First sum in algo
    s = 0
    c = 10
    for i in range(3):
        for j in range(3):
            s += int(cpf[i][j]) * c
            c -= 1
    s *= 10

    first_digit = s%11
    #Rule
    if first_digit == 10:
        first_digit = 0
    if cpf[-1][0] != str(first_digit):
        return False
    
    #Second sum in algo
    s = 0
    c = 11
    for i in range(3):
        for j in range(3):
            s += int(cpf[i][j]) * c
            c -= 1
    s += first_digit * c
    s *= 10

    second_digit = s%11
    if second_digit == 10:
        second_digit = 0

    if cpf[-1][1] != str(second_digit):
        return False

    return True

