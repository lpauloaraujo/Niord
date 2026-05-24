from fastapi import APIRouter
from src.db.database import SessionDep
from src.models.geo import GeoSchemaHelp, GeoModel
from sqlalchemy import select
from src.models.token import TokenDecoded
from src.middle.auth import decode_token, TokenGuard
from src.db.redis import redis
from fastapi import HTTPException, Cookie, Depends, status
from typing import Annotated


router = APIRouter(prefix='/help')
allow_authenticated = TokenGuard()


@router.post("/", status_code=status.HTTP_200_OK)
async def get_user(session: SessionDep, 
             geo_data: GeoSchemaHelp,
             access_token_decoded: TokenDecoded = Depends(allow_authenticated)):
    
    geo_publish: GeoModel = GeoModel(**geo_data.model_dump(),
                                     user_id=access_token_decoded.id)
    await redis.client.publish("alert_channel", 
                               geo_publish.model_dump_json())

    return 
