from enum import Enum

class HelpType(str, Enum):
    ACCIDENT = "accident"
    ROBBERY = "robbery"
    NONE = "none"
