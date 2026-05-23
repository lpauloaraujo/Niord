from fastapi import APIRouter, WebSocket, WebSocketDisconnect
from src.models.geo import GeoLocation
from src.ws.manager import manager
from src.db.redis import redis

router = APIRouter(prefix="/ws")



@router.websocket("/{client_id}")
async def websocket_endpoint(websocket: WebSocket, client_id: int):
    await manager.connect(websocket)
    
    try:
        while True:
            data = await websocket.receive_text()
            print(GeoLocation.model_validate_json(data))
            
            payload = f"Client #{client_id} says: {data}"
            print(payload, flush=True)
            #TO-DO save location data

            #await redis.client.publish("alert_channel", payload)
            
    except WebSocketDisconnect:
        manager.disconnect(websocket)
        #await redis.client.publish("alert_channel", f"Client #{client_id} left the room")
