from pydantic_settings import BaseSettings, SettingsConfigDict
from functools import lru_cache
from pydantic import SecretStr, EmailStr

#Server variables, supports default values and enviroment variables
class Settings(BaseSettings):
    DEBUG: bool = False
    SERVER_NAME: str = 'Niord'
    ORIGINS: list[str] = []
    DB_URL: str = ''
    SECRET_KEY: str = ''
    REDIS_HOST: str = ''
    REDIS_PORT: int = -1
    REDIS_PASSWORD: str = ''
    OTP_EXPIRE: int = -1
   
    EMAIL_URL: str = ""
    EMAIL_FROM: str = ""
    EMAIL_API_KEY: str = ""
    EMAIL_NAME: str = ""

    REFRESH_EXPIRE_DAYS: int = 0
    REFRESH_UPDATE_DAYS: int = 0
    TOKEN_EXPIRE_SECONDS: int = 0
    ALGO:str = ""

    model_config = SettingsConfigDict(env_file='./.env')

@lru_cache
def get_settings() -> Settings:
    return Settings()

