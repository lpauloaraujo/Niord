from fastapi import APIRouter, WebSocket, WebSocketDisconnect, Depends
from src.models.geo import GeoModel, GeoSchema
from src.ws.manager import manager
from src.db.redis import redis
from src.middle.auth import TokenGuard
from src.models.token import TokenDecoded
from pydantic import ValidationError

router = APIRouter(prefix="/ws")
is_auth = TokenGuard()


@router.websocket("/")
async def websocket_endpoint(websocket: WebSocket, 
                             access_token: TokenDecoded = Depends(is_auth)
                             ):
    user_id: int = access_token.id
    print(user_id, flush=True)

    await manager.connect(websocket, user_id)
    
    try:
        while True:
            try:
                data: GeoSchema = GeoSchema.model_validate_json(await websocket.receive_text())
                #Adds user id
                location: GeoModel = GeoModel(**data.model_dump(), user_id=user_id)
            except ValidationError as e:
                await websocket.send_text("Bad format")
                raise WebSocketDisconnect

            await redis.client.geoadd("user_locations",
                                        (
                                        location.longitude,
                                        location.latitude,
                                        user_id 
                                        )
                                      )

    except WebSocketDisconnect:
        manager.disconnect(websocket, user_id)
