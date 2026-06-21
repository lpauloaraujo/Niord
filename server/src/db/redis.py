from redis.asyncio import Redis
from src.config import get_settings 
from secrets import SystemRandom
from src.models.user import UserCredentials
from pydantic import BaseModel
import json
from typing import Any

class RedisEngine:
    def __init__(self):
        s = get_settings()
        self.client: Redis = Redis(
            host=s.REDIS_HOST,
            port=s.REDIS_PORT,
            password=s.REDIS_PASSWORD,
            decode_responses=True
        )
        self.pubsub = self.client.pubsub()

    async def ping_connection(self) -> bool:
        """Explicit async ping to verify the connection on startup."""
        try:
            self.client.ping()
            print("Redis connected")
            return True
        except Exception as e:
            print("Redis not initialized")
            print(e)
            return False

    async def add_to_verify_user(self, user: UserCredentials) -> None:
        await self.client.set(
            f"unverified:{user.email}", 
            user.model_dump_json(),
            ex=get_settings().UNVERIFIED_EXPIRE
        )

    async def get_to_verify_user(self, email: str) -> UserCredentials | None:
        query = await self.client.get(f"unverified:{email}")
        if query is None:
            return None
        
        entry: dict[str, Any] = json.loads(query)
        return UserCredentials.model_validate(entry)

    async def create_otp(self, email: str) -> int:
        code = SystemRandom().randint(100000, 999999)
        
        await self.client.set(
            f"otp:{email}", 
            code, 
            ex=get_settings().OTP_EXPIRE
        )
        return code
    
    async def check_otp(self, email: str, code: int) -> bool | None:
        otp_code = await self.client.get(f"otp:{email}")
        if otp_code is None: 
            return None
            
        if otp_code == str(code):
            await self.client.delete(f"otp:{email}")
            return True
        return False


redis = RedisEngine()
