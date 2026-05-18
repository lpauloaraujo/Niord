from fastapi import APIRouter
from src.db.database import SessionDep
from src.db.redis import redis
from src.models.user import User, UserPublic, UserUpdate
from src.models.token import TokenDecoded
from src.middle.auth import TokenGuard
from src.middle.user import check_hash, get_user_by_email, hash_password, is_valid_plate
from fastapi import HTTPException, Depends
from pydantic import EmailStr
from src.models.error import ErrorMessage, create_detail, ErrorType


router = APIRouter(prefix='/user')
allow_authenticated = TokenGuard()


@router.get("/", response_model=UserPublic)
def get_user(session: SessionDep, access_token_decoded: TokenDecoded = Depends(allow_authenticated)):
    user = session.get(User, access_token_decoded.id)
    if not user:
        raise HTTPException(404, create_detail(message="Usuário não encontrado"))
    return user


@router.post("/email-otp", status_code=200, responses={401: {"model": ErrorMessage}})
def request_email_update_otp(
    session: SessionDep,
    email: EmailStr,
    access_token_decoded: TokenDecoded = Depends(allow_authenticated)
):
    user = session.get(User, access_token_decoded.id)
    if not user:
        raise HTTPException(404, create_detail(message="Usuário não encontrado"))

    new_email = str(email)
    existing = get_user_by_email(session, new_email)
    if existing and existing.id != user.id:
        raise HTTPException(
            401,
            create_detail(message="Email já em uso", type=ErrorType.conflict, field="email")
        )

    otp_code = redis.create_otp(new_email)
    print(otp_code)
    return {"detail": "Código enviado"}


@router.patch("/", response_model=UserPublic, responses={401: {"model": ErrorMessage}})
def update_user(
    user_update: UserUpdate,
    session: SessionDep,
    access_token_decoded: TokenDecoded = Depends(allow_authenticated)
):
    user = session.get(User, access_token_decoded.id)
    if not user:
        raise HTTPException(404, create_detail(message="Usuário não encontrado"))

    if user_update.name is not None:
        name = user_update.name.strip()
        if len(name) < 2:
            raise HTTPException(401, create_detail(message="Nome inválido", field="name"))
        user.name = name

    if user_update.registration_plate is not None:
        plate = user_update.registration_plate.strip().upper().replace("-", "")
        if not is_valid_plate(plate):
            raise HTTPException(401, create_detail(message="Placa inválida", field="registration_plate"))
        user.registration_plate = plate

    if user_update.blood_type is not None:
        user.blood_type = user_update.blood_type.strip() or None

    email_changed = user_update.email is not None and str(user_update.email) != user.email
    if email_changed:
        new_email = str(user_update.email)
        existing = get_user_by_email(session, new_email)
        if existing and existing.id != user.id:
            raise HTTPException(
                401,
                create_detail(message="Email já em uso", type=ErrorType.conflict, field="email")
            )

        if user_update.email_otp_code is None:
            raise HTTPException(401, create_detail(message="Código OTP obrigatório", field="email_otp_code"))

        otp_check = redis.check_otp(new_email, user_update.email_otp_code)
        if otp_check is None:
            raise HTTPException(404, create_detail(message="Email não aguarda OTP ou expirou", field="email_otp_code"))
        if not otp_check:
            raise HTTPException(401, create_detail(message="Código incorreto", field="email_otp_code"))

        user.email = new_email

    phone_changed = user_update.telephone is not None and user_update.telephone.strip() != user.telephone
    password_changed = user_update.new_password is not None and user_update.new_password != ""
    if phone_changed or password_changed:
        if not user_update.current_password:
            raise HTTPException(401, create_detail(message="Senha atual obrigatória", field="current_password"))
        if not check_hash(user_update.current_password, user.password):
            raise HTTPException(401, create_detail(message="Senha atual inválida", field="current_password"))

    if phone_changed:
        user.telephone = user_update.telephone.strip()

    if password_changed:
        if len(user_update.new_password) < 8:
            raise HTTPException(401, create_detail(message="Senha deve ter ao menos 8 caracteres", field="new_password"))
        user.password = hash_password(user_update.new_password)

    session.commit()
    session.refresh(user)
    return user
