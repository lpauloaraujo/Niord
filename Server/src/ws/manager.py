from fastapi import WebSocket
from typing import Tuple
from src.db.redis import redis
from src.models.geo import GeoModel

class RedisConnectionManager:
    def __init__(self):
        self.local_connections: dict[int, WebSocket] = {}

    async def connect(self, websocket: WebSocket, user_id: int):
        await websocket.accept()
        self.local_connections[user_id] = websocket

    def disconnect(self, websocket: WebSocket, user_id: int):
        self.local_connections.pop(user_id)

    async def broadcast_locally(self, location: GeoModel):
        #Radius limit
        nearby_users = await redis.client.geosearch(
                name="user_locations",
                latitude=location.latitude,
                longitude=location.longitude,
                radius=500,
                unit="m"
                )
        for u in nearby_users:
            if u != location.user_id:
                try:
                    conn = self.local_connections.get(int(u))
                    if conn:
                        await conn.send_text(location.model_dump_json())
                    pass
                except Exception:
                    pass

manager = RedisConnectionManager()
