# services.py
from sqlalchemy.future import select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity
import numpy as np
import random
from fastapi import HTTPException
from contextlib import asynccontextmanager
from .models import Animal

# 데이터베이스 설정
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
        texts = [f"{animal.shelter_id} {animal.age} {animal.color} {animal.gender} {animal.kind} {animal.region} {animal.special_mark} {animal.happen_place}"
                for animal in animals]

    # 문자열 리스트를 TF-IDF 벡터로 변환
    tfidf_matrix = vectorizer.fit_transform(texts)

    return tfidf_matrix, {animal.id: idx for idx, animal in enumerate(animals)}

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
        query_vec = tfidf_matrix[idx].toarray()
        cosine_sim = cosine_similarity(query_vec, tfidf_matrix.toarray())
        indices = np.argsort(cosine_sim[0])[::-1][1:num_results+1]
    
    # 유사한 동물의 ID를 리스트로 반환
    return [list(animal_index.keys())[i] for i in indices]