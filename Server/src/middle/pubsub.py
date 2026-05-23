from src.db.redis import redis
from src.ws.manager import manager
import asyncio

async def listen_to_redis():
    """Background task running on every server instance, listening to Redis"""
    try:
        await redis.pubsub.subscribe("alert_channel")
        print("Successfully subscribed to Redis channel.")
        
        async for message in redis.pubsub.listen():
            if message["type"] == "message":
                data = message["data"]
                await manager.broadcast_locally(data)
    except asyncio.CancelledError:
        print("Redis listener task cancelled.")
    except Exception as e:
        print(f"Error in Redis listener: {e}")
