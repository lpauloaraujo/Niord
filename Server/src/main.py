from fastapi import FastAPI
from contextlib import asynccontextmanager
from .db.database import create_tables
from .db.redis import redis 
from .endpoints.api import api_router

#Initialization
@asynccontextmanager
async def lifespan(app: FastAPI):
    create_tables()
    yield
    redis.client.close()


app = FastAPI(lifespan=lifespan)
app.include_router(api_router)

@app.get("/")
def read_root():
    return {"Hello": "World"}


