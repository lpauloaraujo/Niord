from fastapi import WebSocket
from typing import Tuple
from src.db.redis import redis
from src.models.geo import GeoModel
from src.models.enums import HelpType

class RedisConnectionManager:
    def __init__(self):
        self.local_connections: dict[int, WebSocket] = {}

    async def connect(self, websocket: WebSocket, user_id: int):
        await websocket.accept()
        self.local_connections[user_id] = websocket

    def disconnect(self, websocket: WebSocket, user_id: int):
        self.local_connections.pop(user_id)

    async def broadcast_locally(self, location: GeoModel):
        radius = 500 if location.type is HelpType.ACCIDENT else 3000
        #Radius limit
        nearby_users = await redis.client.geosearch(
                name="user_locations",
                latitude=location.latitude,
                longitude=location.longitude,
                radius=radius,
                unit="m"
                )
        for u in nearby_users:
            if u != location.user_id:
                try:
                    conn = self.local_connections.get(int(u))
                    if conn:
                        await conn.send_text(location.model_dump_json())
                except Exception:
                    pass

    async def send_to_id(self, target_id: int, geo_data: GeoModel):
        #type should be HelpType.NONE to identify as an acception
        try:
            await (
                    self.local_connections[target_id].
                    send_text(geo_data.model_dump_json())
                   )
        except Exception:
            pass

manager = RedisConnectionManager()
