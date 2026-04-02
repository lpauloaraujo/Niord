from redis import Redis
from ..config import get_settings 

def setup_redis_client():
    s = get_settings()
    redis_client = Redis(
            host= s.REDIS_HOST,
            port= s.REDIS_PORT,
            password= s.REDIS_PASSWORD,
            decode_responses=True
            )
    try:
        redis_client.ping()
        print("Redis connected")
    except Exception as e:
        print("Redis not initialized")
        print(e)
    return redis_client

redis_engine = setup_redis_client()

