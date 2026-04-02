from ..db.database import Base
from sqlalchemy.orm import mapped_column, Mapped



class Seguradora(Base):
    __tablename__ = "seguradora"

    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(nullable=False)


