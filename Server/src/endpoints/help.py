from fastapi import APIRouter
from src.db.database import SessionDep
from src.models.geo import GeoSchemaHelp, GeoModel, GeoSchemaAnswer, GeoSchemaAnswerMulti
from src.models.enums import HelpType
from sqlalchemy import select
from src.models.token import TokenDecoded
from src.middle.auth import decode_token, TokenGuard
from src.db.redis import redis
from fastapi import HTTPException, Cookie, Depends, status
from typing import Annotated
from src.ws.manager import manager


router = APIRouter(prefix='/help')
allow_authenticated = TokenGuard()


@router.post("/ask", status_code=status.HTTP_200_OK)
async def get_help(
             geo_data: GeoSchemaHelp,
             access_token_decoded: TokenDecoded = Depends(allow_authenticated)):
    
    geo_publish: GeoModel = GeoModel(**geo_data.model_dump(),
                                     user_id=access_token_decoded.id)
    await redis.client.publish("alert_channel", 
                               geo_publish.model_dump_json())

    return 


@router.post("/answer", status_code=status.HTTP_200_OK)
async def answer_help( 
             geo_data: GeoSchemaAnswer,
             access_token_decoded: TokenDecoded = Depends(allow_authenticated)):
    
    geo_model: GeoModel = GeoModel(**geo_data.model_dump(),
                                     user_id=access_token_decoded.id,
                                     )

    await manager.send_to_id(geo_data.target_id, geo_model)

    return 

@router.post("/answer_multi", status_code=status.HTTP_200_OK)
async def answer_help_multi( 
             geo_data: GeoSchemaAnswerMulti,
             access_token_decoded: TokenDecoded = Depends(allow_authenticated)):
    
    geo_model: GeoModel = GeoModel(**geo_data.model_dump(),
                                     user_id=access_token_decoded.id,
                                     )
   
    for target_id in geo_data.target_ids:
        await manager.send_to_id(target_id, geo_model)

    return 

