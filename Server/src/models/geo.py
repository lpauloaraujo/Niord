from pydantic import BaseModel


class GeoSchema(BaseModel):
    latitude: float
    longitude: float

class GeoModel(BaseModel):
    user_id: int
    latitude: float
    longitude: float
