# models.py
from sqlalchemy import Column, Integer, String
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()

class Animal(Base):
    __tablename__ = "animal_tb"

    id = Column(Integer, primary_key=True, index=True)
    shelter_id = Column(Integer)  
    age = Column(String(255))
    color = Column(String(255))
    gender = Column(String(255))
    kind = Column(String(255))
    region = Column(String(255))
    special_mark = Column(String(255))
    happen_place = Column(String(255))