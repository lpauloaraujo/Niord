from pydantic_settings import BaseSettings, SettingsConfigDict
from functools import lru_cache

#Server variables, supports default values and enviroment variables
class Settings(BaseSettings):
    SERVER_NAME: str = 'Niord'
    ORIGINS: list[str] = []
    DB_URL: str = ''
    SECRET_KEY: str = ''
    model_config = SettingsConfigDict(env_file='./.env')

@lru_cache
def get_settings() -> Settings:
    return Settings()

