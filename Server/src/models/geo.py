from pydantic import BaseModel

from src.models.enums import HelpType

class GeoSchema(BaseModel):
    latitude: float
    longitude: float

class GeoSchemaHelp(GeoSchema):
    type: HelpType


class GeoSchemaAnswer(GeoSchema):
    target_id: int
    type: HelpType

class GeoModel(GeoSchemaHelp):
    user_id: int
