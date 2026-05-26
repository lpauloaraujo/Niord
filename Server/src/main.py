from fastapi import FastAPI
from contextlib import asynccontextmanager
from .db.database import create_tables
from .db.redis import redis 
from .endpoints.api import api_router
import asyncio
from src.middle.pubsub import listen_to_redis
from src.ws.endpoints import router as ws_router

#Initialization
@asynccontextmanager
async def lifespan(app: FastAPI):
    create_tables()
     
    redis_pubsub = asyncio.create_task(listen_to_redis()) 

    yield
 
    if redis_pubsub:
        redis_pubsub.cancel()
   
    await redis.client.close()


app = FastAPI(lifespan=lifespan)
app.include_router(api_router)
app.include_router(ws_router)

@app.get("/")
def read_root():
    return {"Hello": "World"}


