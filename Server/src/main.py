from fastapi import FastAPI
from contextlib import asynccontextmanager
from .models.db import create_tables
from .endpoints.api import api_router

#Initialization
@asynccontextmanager
async def lifespan(app: FastAPI):
    create_tables()
    yield


app = FastAPI(lifespan=lifespan)
app.include_router(api_router)

@app.get("/")
def read_root():
    return {"Hello": "World"}


