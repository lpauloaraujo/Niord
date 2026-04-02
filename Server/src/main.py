from fastapi import FastAPI
from contextlib import asynccontextmanager
from .db.database import create_tables
from .db.redis import redis_engine 
from .endpoints.api import api_router

#Initialization
@asynccontextmanager
async def lifespan(app: FastAPI):
    create_tables()
    yield
    redis_engine.close()


app = FastAPI(lifespan=lifespan)
app.include_router(api_router)

@app.get("/")
def read_root():
    return {"Hello": "World"}

@app.get("/redis/{foo}")
async def redis_test_get(foo: str):
    return redis_engine.get(foo)


@app.get("/redis/{foo}/{bar}")
async def redis_test(foo: str, bar: str):
    redis_engine.set(foo, bar)
    return redis_engine.get(foo)


