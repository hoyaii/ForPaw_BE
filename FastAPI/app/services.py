# services.py
from sqlalchemy.future import select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sklearn.feature_extraction.text import TfidfVectorizer
from fastapi import HTTPException
from contextlib import asynccontextmanager
from .models import Animal
from pymilvus import connections, FieldSchema, CollectionSchema, DataType, Collection

# Milvus에 연결
connections.connect("default", host="localhost", port="19530")

# 컬렉션 스키마 정의
fields = [
    FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=False),
    FieldSchema(name="vector", dtype=DataType.FLOAT_VECTOR, dim=512)  # 벡터의 차원 설정
]

animal_schema = CollectionSchema(fields, "animal_vectors")

# 컬렉션 생성
animal_collection = Collection("animal_collection", animal_schema)

# MySQL스 설정
DATABASE_URL = "mysql+aiomysql://root:0623@localhost/ForPaw"
engine = create_async_engine(DATABASE_URL)
AsyncSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine, class_=AsyncSession)

@asynccontextmanager
async def get_db_session():
    async with AsyncSessionLocal() as session:
        yield session

# 벡터화 객체 초기화
vectorizer = TfidfVectorizer()

async def load_and_vectorize_data():
    async with get_db_session() as db:
        result = await db.execute(select(Animal))
        animals = result.scalars().all()
        texts = [
            f"{animal.shelter_id} {animal.age} {animal.color} {animal.gender} {animal.kind} {animal.region} {animal.special_mark} {animal.happen_place}"
            for animal in animals
        ]

    tfidf_matrix = vectorizer.fit_transform(texts)
    vectors = tfidf_matrix.toarray()

    ids = [animal.id for animal in animals]

    # Milvus에 데이터 삽입
    animal_collection.insert([ids, vectors])
    
    return vectors, {animal.id: idx for idx, animal in enumerate(animals)}

async def get_similar_animals(animal_id, animal_index, tfidf_matrix, num_results=5):
    async with get_db_session() as db:
        result = await db.execute(select(Animal).filter(Animal.id == animal_id))
        query_animal = result.scalars().first()
        if not query_animal:
            raise HTTPException(status_code=404, detail="Animal not found")
    
        # 조회된 동물의 인덱스 찾기
        idx = animal_index.get(animal_id)
        if idx is None:
            raise HTTPException(status_code=404, detail="Index not found for the given animal ID")
    
        # 해당 동물의 벡터를 배열로 변환하고, 전체 데이터베이스 벡터와의 코사인 유사도 계산
        query_vec = tfidf_matrix[idx].toarray().tolist()
        search_params = {"metric_type": "L2", "params": {"nprobe": 10}}
        results = animal_collection.search([query_vec], "vector", search_params, limit=num_results)
        
        similar_animal_ids = [res.id for res in results[0].ids]

        return similar_animal_ids