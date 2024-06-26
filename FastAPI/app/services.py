# services.py
from sqlalchemy.future import select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.decomposition import PCA
from fastapi import HTTPException
from contextlib import asynccontextmanager
from .models import Animal
from pymilvus import connections, FieldSchema, CollectionSchema, DataType, Collection, utility
import numpy as np  
import redis
from .config import settings

# Milvus 초기화 함수
def initialize_milvus():
    connections.connect("default", host=settings.MILVUS_HOST, port=str(settings.MILVUS_PORT))

    # 컬렉션 이름은 animal_collection
    collection_name = "animal_collection"

    # fastAPI를 시작할 때, 기존에 저장된 데이터는 삭제
    if utility.has_collection(collection_name):
        collection = Collection(name=collection_name)
        collection.drop()

    # ID 필드와 벡터 필드 정의. 차원은 512 
    fields = [
        FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=False), 
        FieldSchema(name="vector", dtype=DataType.FLOAT_VECTOR, dim=512)  
    ]

    # 컬렉션 생성
    animal_schema = CollectionSchema(fields, "animal_vectors")
    animal_collection = Collection(collection_name, animal_schema)
    
    return animal_collection

# MySQL 초기화 함수
def initialize_mysql():
    engine = create_async_engine(settings.DATABASE_URL)
    AsyncSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine, class_=AsyncSession)
    
    return AsyncSessionLocal

# Redis 초기화 함수
def initialize_redis():
    return redis.Redis(host=settings.REDIS_HOST, port=settings.REDIS_PORT, db=settings.REDIS_DB, decode_responses=True)

# Milvus, MySQL, 백터화 객체 초기화
animal_collection = initialize_milvus()
AsyncSessionLocal = initialize_mysql()
redis_client = initialize_redis()
vectorizer = TfidfVectorizer()
vector_dim = 512

@asynccontextmanager
async def get_db_session():
    async with AsyncSessionLocal() as session:
        yield session

async def load_and_vectorize_data():
    # 비동기 세션을 사용하여 데이터베이스에 연결. Animal 테이블에서 모든 데이터를 가져와서 리스트로 변환
    async with get_db_session() as db:
        result = await db.execute(select(Animal))
        animals = result.scalars().all()
        texts = [
            f"{animal.shelter_id} {animal.age} {animal.color} {animal.gender} {animal.kind} {animal.region} {animal.special_mark} {animal.happen_place}"
            for animal in animals
        ]

    # 텍스트 데이터를 TF-IDF 벡터로 변환 후 밀집 배열로 변환
    tfidf_matrix = vectorizer.fit_transform(texts)
    vectors = tfidf_matrix.toarray()

    # PCA를 사용하여 벡터 차원을 512로 축소
    pca = PCA(n_components=vector_dim)
    reduced_vectors = pca.fit_transform(vectors)

    # 동물 ID 리스트 생성
    ids = [animal.id for animal in animals]

    # Milvus에 데이터 삽입
    animal_collection.insert([ids, reduced_vectors])

    # 인덱스 생성
    index_params = {"index_type": "IVF_FLAT", "metric_type": "L2", "params": {"nlist": 128}}
    animal_collection.create_index("vector", index_params)

    # 컬렉션 로드
    animal_collection.load()
    
    return reduced_vectors, {animal.id: idx for idx, animal in enumerate(animals)}

async def get_similar_animals(animal_id, animal_index, tfidf_matrix, num_results=5):
    async with get_db_session() as db:
        # 주어진 ID의 동물 선택해서 
        result = await db.execute(select(Animal).filter(Animal.id == animal_id))
        query_animal = result.scalars().first()
        if not query_animal:
            raise HTTPException(status_code=404, detail="해당 동물을 찾을 수 없습니다.")
    
        # 조회된 동물의 인덱스 찾기
        idx = animal_index.get(animal_id)
        if idx is None:
            raise HTTPException(status_code=404, detail="해당 동물에 대한 인덱스가 존재하지 않습니다.")
    
        # 해당 동물의 벡터를 배열로 변환하고, 전체 데이터베이스 벡터와의 코사인 유사도 계산
        query_vec = tfidf_matrix[idx].tolist() 
        search_params = {"metric_type": "L2", "params": {"nprobe": 10}}
        results = animal_collection.search([query_vec], "vector", search_params, limit=num_results)
        
        similar_animal_ids = [res for res in results[0].ids]

        return similar_animal_ids
    
# MySQL에 저장된 새로운 동물 데이터를 백터 DB에 업데이트
async def update_new_animals(animal_index, tfidf_matrix):
    async with get_db_session() as db:
        result = await db.execute(select(Animal))
        animals = result.scalars().all()

        # 현재 animal_index에서 기존의 모든 동물 ID를 가져옴
        existing_ids = set(animal_index.keys())
        new_animals = [animal for animal in animals if animal.id not in existing_ids]

        # 새로운 동물이 없는 경우, 함수 종료
        if not new_animals:
            return

        # 새로운 동물 데이터로 텍스트 리스트를 생성
        new_texts = [
            f"{animal.shelter_id} {animal.age} {animal.color} {animal.gender} {animal.kind} {animal.region} {animal.special_mark} {animal.happen_place}"
            for animal in new_animals
        ]

    # 새로운 텍스트 데이터를 TF-IDF 벡터로 변환
    new_vectors = vectorizer.transform(new_texts).toarray()
    pca = PCA(n_components=vector_dim)
    reduced_new_vectors = pca.fit_transform(new_vectors)

    # 새로운 동물들의 ID 리스트를 생성
    new_ids = [animal.id for animal in new_animals]

    # Milvus에 새로운 데이터 삽입
    animal_collection.insert([new_ids, reduced_new_vectors])

    # 인덱스 생성
    index_params = {"index_type": "IVF_FLAT", "metric_type": "L2", "params": {"nlist": 128}}
    animal_collection.create_index("vector", index_params)

    # 컬렉션 로드
    animal_collection.load()
    
    # 기존 tfidf_matrix에 새로운 벡터 추가
    current_length = len(tfidf_matrix)
    tfidf_matrix = np.vstack([tfidf_matrix, reduced_new_vectors])

    # animal_index를 업데이트하여 새로운 동물들의 ID와 인덱스를 추가
    animal_index.update({animal.id: current_length + idx for idx, animal in enumerate(new_animals)})