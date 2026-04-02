from redis import Redis
from ..config import get_settings 
from secrets import SystemRandom

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
    
    def create_otp(self, email: str):
        #Expires in 5 minutes
        self.client.set(f"otp:{email}", 
                        sys_random.randint(100000, 999999), 
                        ex=get_settings().OTP_EXPIRE)
    
    def check_otp(self, email: str, code: int):
        otp_code = self.client.get(f"otp:{email}")
        if otp_code == None: return None
        if otp_code == str(code):
            self.client.delete(f"otp:{email}")
            return True
        return False

redis = RedisEngine()

