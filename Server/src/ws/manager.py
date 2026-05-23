from fastapi import WebSocket

class RedisConnectionManager:
    def __init__(self):
        self.local_connections: list[WebSocket] = []

    async def connect(self, websocket: WebSocket):
        await websocket.accept()
        self.local_connections.append(websocket)

    def disconnect(self, websocket: WebSocket):
        self.local_connections.remove(websocket)

    async def broadcast_locally(self, message: str):
        for connection in self.local_connections:
            try:
                await connection.send_text(message)
            except Exception:
                pass

manager = RedisConnectionManager()
