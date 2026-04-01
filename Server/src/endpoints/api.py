from fastapi import APIRouter
from .user import router as user_router

api_router = APIRouter(prefix='')
#All routers
api_router.include_router(user_router)

@api_router.get('/greet')
def greet():
    return "Hello, API world!"
