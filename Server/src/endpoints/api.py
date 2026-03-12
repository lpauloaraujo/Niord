from fastapi import APIRouter

api_router = APIRouter(prefix='/test')

@api_router.get('/')
def test():
    return "Hello, API world!"
