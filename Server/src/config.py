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
    
    MAIL_FROM_NAME: str = ''
    MAIL_USERNAME: str = ''
    MAIL_PASSWORD: SecretStr = SecretStr('')
    MAIL_FROM: EmailStr = ''
    MAIL_PORT: int = -1
    MAIL_SERVER: str = ''
    MAIL_STARTTLS: bool = False 
    MAIL_SSL_TLS: bool = False
    USE_CREDENTIALS: bool = False 
    VALIDATE_CERTS: bool = False 

    model_config = SettingsConfigDict(env_file='./.env')

@lru_cache
def get_settings() -> Settings:
    return Settings()

