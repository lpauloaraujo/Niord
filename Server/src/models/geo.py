from pydantic import BaseModel



class GeoLocation(BaseModel):
    user_id: int | None = None
    latitude: float
    longitude: float
