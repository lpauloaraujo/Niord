from redis import Redis
from src.config import get_settings 
from secrets import SystemRandom
from src.models.user import UserCredentials
from pydantic import BaseModel
import json
from typing import Any

sys_random = SystemRandom()

class RedisEngine:
    def __init__(self):
        s = get_settings()
        self.client: Redis = Redis(
                host= s.REDIS_HOST,
                port= s.REDIS_PORT,
                password= s.REDIS_PASSWORD,
                decode_responses=True
                )
        try:
            self.client.ping()
            print("Redis connected")
        except Exception as e:
            print("Redis not initialized")
            print(e)

    def add_to_verify_user(self, user: UserCredentials):
        self.client.set(f"unverified:{user.email}", 
                        user.model_dump_json(),
                        ex=get_settings().UNVERIFIED_EXPIRE)

    def get_to_verify_user(self, email: str) -> UserCredentials | None:
        query = self.client.get(f"unverified:{email}")
        if query is None:
            return None
        entry: dict[str, Any] = json.loads(str(query))
        return UserCredentials.model_validate(entry)

    def create_otp(self, email: str) -> int:
        code = sys_random.randint(100000, 999999)
        #Expires in 5 minutes
        self.client.set(f"otp:{email}", 
                        code, 
                        ex=get_settings().OTP_EXPIRE)
        return code
    
    def check_otp(self, email: str, code: int):
        otp_code = self.client.get(f"otp:{email}")
        if otp_code == None: return None
        if otp_code == str(code):
            self.client.delete(f"otp:{email}")
            return True
        return False

redis = RedisEngine()

