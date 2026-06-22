from fastapi import APIRouter
from src.endpoints.user import router as user_router
from src.endpoints.auth import router as auth_router
from src.endpoints.help import router as help_router
from src.endpoints.waha import router as whatsapp_router

api_router = APIRouter(prefix='')
#All routers
api_router.include_router(user_router)
api_router.include_router(auth_router)
api_router.include_router(help_router)
api_router.include_router(whatsapp_router)


@api_router.get('/greet')
def greet():
    return "Hello, API world!"



