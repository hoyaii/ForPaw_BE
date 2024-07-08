# services.py
from langchain_community.llms import OpenAI
from langchain.prompts import PromptTemplate
from sqlalchemy.future import select
from sqlalchemy.orm import sessionmaker
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.decomposition import PCA
from fastapi import HTTPException
from contextlib import asynccontextmanager
from .models import Animal, Group
from pymilvus import connections, FieldSchema, CollectionSchema, DataType, Collection, utility
import numpy as np  
import redis
from .config import settings
from typing import List

# Milvus 초기화 함수
def initialize_milvus():
    connections.connect("default", host=settings.MILVUS_HOST, port=str(settings.MILVUS_PORT))

    # 1. 동물 컬렉션 초기화
    animal_fields = [
        FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=False), 
        FieldSchema(name="vector", dtype=DataType.FLOAT_VECTOR, dim=512)
    ]
    animal_collection = create_collection("animal_collection", animal_fields)

    # 2. 그룹 컬렉션 초기화
    group_fields = [
        FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=False), 
        FieldSchema(name="vector", dtype=DataType.FLOAT_VECTOR, dim=4)  # 초기 설정
    ]
    group_collection = create_collection("group_collection", group_fields)
    
    return animal_collection, group_collection

# MySQL 초기화 함수
def initialize_mysql():
    engine = create_async_engine(settings.DATABASE_URL)
    AsyncSessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine, class_=AsyncSession)
    
    return AsyncSessionLocal

# Redis 초기화 함수
def initialize_redis():
    return redis.Redis(host=settings.REDIS_HOST, port=settings.REDIS_PORT, db=settings.REDIS_DB, decode_responses=True)

# Milvus, MySQL, 백터화 객체 초기화, PCA 객체 초기화
animal_collection, group_collection = initialize_milvus()
AsyncSessionLocal = initialize_mysql()
redis_client = initialize_redis()
vectorizer = TfidfVectorizer()
animal_pca = PCA(n_components=512)
group_pca = PCA(n_components=4)

@asynccontextmanager
async def get_db_session():
    async with AsyncSessionLocal() as session:
        yield session

async def load_and_vectorize_animal_data():
    # 비동기 세션을 사용하여 데이터베이스에 연결. Animal 테이블에서 모든 데이터를 가져와서 리스트로 변환
    # 만약, DB에 Animal 데이터가 하나도 없다면 에러 발생하니 주의
    async with get_db_session() as db:
        result = await db.execute(select(Animal).filter(Animal.removed_at.is_(None)))
        animals = result.scalars().all()
        texts = [
            f"{animal.age} {animal.color} {animal.gender} {animal.kind} {animal.region} {animal.special_mark} {animal.neuter}"
            for animal in animals
        ]

    # PCA 객체를 사용하여 벡터 차원을 512로 축소 
    reduced_vectors = vectorize_and_reduce(texts, animal_pca)
    ids = [animal.id for animal in animals]
    insert_and_index_vectors(animal_collection, ids, reduced_vectors)
    
    return reduced_vectors, {animal.id: idx for idx, animal in enumerate(animals)}

async def load_and_vectorize_group_data():
    async with get_db_session() as db:
        result = await db.execute(select(Group))
        groups = result.scalars().all()
        texts = [
            f"{group.province} {group.district} {group.sub_district} {group.category} {group.description}"
            for group in groups
        ]

    # 벡터 차원을 4로 축소 
    reduced_vectors = vectorize_and_reduce(texts, group_pca)
    ids = [group.id for group in groups]
    insert_and_index_vectors(group_collection, ids, reduced_vectors)
    
    return reduced_vectors, {group.id: idx for idx, group in enumerate(groups)}

async def get_similar_animals(animal_id, animal_index, tfidf_matrix):
    # animal_id에 대한 동물이 존재여부 체크
    async with get_db_session() as db: 
        result = await db.execute(select(Animal).filter(Animal.id == animal_id))
        query_animal = result.scalars().first()
        if not query_animal:
            return []
    
    # animal_id에 대한 인덱스 존재여부 체크
    idx = animal_index.get(animal_id)
    if idx is None:
        return []
    
    # 해당 동물의 벡터를 배열로 변환하고, 전체 데이터베이스 벡터와의 코사인 유사도 계산
    query_vec = tfidf_matrix[idx].tolist() 
    similar_animal_ids = search_similar_items(query_vec, animal_collection, 5)

    return similar_animal_ids

async def get_similar_groups(group_id, group_index, tfidf_matrix):
    async with get_db_session() as db:
        result = await db.execute(select(Group).filter(Group.id == group_id))
        query_group = result.scalars().first()
        if not query_group:
            return []

    idx = group_index.get(group_id)
    if idx is None:
        return []

    query_vec = tfidf_matrix[idx].tolist()
    similar_group_ids = search_similar_items(query_vec, group_collection, 5)

    return similar_group_ids
    
# MySQL에 저장된 새로운 동물 데이터를 백터 DB에 업데이트
async def update_new_animals(animal_index, tfidf_matrix):
    async with get_db_session() as db:
        result = await db.execute(select(Animal).filter(Animal.removed_at.is_(None)))
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

    # 새로운 텍스트 데이터를 TF-IDF 벡터로 변환, 전역으로 선언한 PCA를 사용
    new_vectors = vectorizer.transform(new_texts).toarray()
    reduced_new_vectors = animal_pca.transform(new_vectors)  

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

async def update_new_groups(group_index, tfidf_matrix):
    async with get_db_session() as db:
        result = await db.execute(select(Group))
        groups = result.scalars().all()

        existing_ids = set(group_index.keys())
        new_groups = [group for group in groups if group.id not in existing_ids]

        if not new_groups:
            return

        new_texts = [
            f"{group.province} {group.district} {group.subDistrict} {group.category} {group.description}"
            for group in new_groups
        ]

    new_vectors = vectorizer.transform(new_texts).toarray()
    reduced_new_vectors = group_pca.transform(new_vectors)

    new_ids = [group.id for group in new_groups]

    group_collection.insert([new_ids, reduced_new_vectors])

    index_params = {"index_type": "IVF_FLAT", "metric_type": "L2", "params": {"nlist": 128}}
    group_collection.create_index("vector", index_params)
    group_collection.load()

    current_length = len(tfidf_matrix)
    tfidf_matrix = np.vstack([tfidf_matrix, reduced_new_vectors])

    group_index.update({group.id: current_length + idx for idx, group in enumerate(new_groups)})

async def generate_animal_introduction(animal_id):
    async with get_db_session() as db:
        result = await db.execute(select(Animal).filter(Animal.id == animal_id))
        animal = result.scalars().first()
        if not animal:
            raise HTTPException(status_code=404, detail="해당 동물을 찾을 수 없습니다.")
    
    prompt = (
        "### Persona ###\n"
        "1. Your task is to write a introduction for the animal based on the following information.\n"
        "2. Highlight the animal's positive traits and suitability as a pet to encourage potential adopters.\n"
        "### Animal Information ###\n"
        f"Name: {animal.name}\n"
        f"Species: {animal.kind}\n"
        f"Gender: {'Male' if animal.gender == 'M' else 'Female'}\n"
        f"Spayed/Neutered: {'Yes' if animal.neuter == 'Y' else 'No'}\n"
        f"Color: {animal.color}\n"
        f"Approximate Age: {animal.age}\n"
        f"Location Found: {animal.happen_place}\n"
        f"Special Characteristics: {animal.special_mark}\n"
        "### Background ###\n"
        f"{animal.name}, a {animal.kind}, was found in {animal.happen_place}. Known for its {animal.special_mark} and unique {animal.color} coat, {animal.name} has shown loving nature despite its circumstances.\n"
        "### Title ###\n"
        "1. Provide a catchy and appealing title for the introduction in Korean.\n"
        "2. The title should be 25 characters or fewer.\n"
        "3. Use a term of endearment or a metaphorical expression in the title.\n"
        "### Response Format ###\n"
        "1. The first line should be the title, prefixed with 'Title: '.\n"
        "2. After the title, there should be two newline characters (\\n\\n).\n"
        "3. The introduction should follow after the newline characters.\n"
        "### Writing Guidelines ###\n"
        "1. Provide the introduction in Korean.\n"
        "2. Write from the animal's perspective. Use a tone that conveys warmth and affection. Most sentences should end with '요' to maintain a soft and friendly tone.\n"
        "3. Start with a strong, attention-grabbing sentence that includes a metaphor or simile.\n"
        "4. Avoid repetition.\n"
        "5. Use punctuation marks and emoticons where appropriate to add enthusiasm, friendliness, and enhance the emotional appeal. Not all sentences need them.\n"
        "6. Please write a response using between 200 and 300 characters."
    )

    llm = OpenAI(api_key=settings.OPENAI_API_KEY)
    prompt_template = PromptTemplate(input_variables=["prompt"], template="{prompt}")
    formatted_prompt = prompt_template.format(prompt=prompt)
    max_response_tokens = 750

    response = llm(
        formatted_prompt,
        max_tokens=max_response_tokens,
        temperature=0.3
    )
    
    # 타이틀만 추출
    response_lines = response.strip().split('\n')
    title = response_lines[0].replace("Title: ", "").strip()
    introduction = '\n'.join(response_lines[1:]).strip()

    return {
        "title": title,
        "introduction": introduction
    }

def vectorize_and_reduce(texts: List[str], pca_model: PCA) -> np.ndarray:
    # 텍스트 데이터를 TF-IDF 벡터로 변환 후 차원 축소
    # 백터의 길이는 Milvus 초기 설정시 설정한 차원으로 나누어져야 함 => 길이를 축소해서 나누어지게 만듦
    tfidf_matrix = vectorizer.fit_transform(texts)
    vectors = tfidf_matrix.toarray()
    reduced_vectors = pca_model.fit_transform(vectors)

    return reduced_vectors

def insert_and_index_vectors(collection: Collection, ids: List[int], vectors: np.ndarray):
    # Milvus에 데이터 삽입
    collection.insert([ids, vectors.tolist()])

    # 인덱스 생성
    index_params = {"index_type": "IVF_FLAT", "metric_type": "L2", "params": {"nlist": 128}}
    collection.create_index("vector", index_params)

    collection.load()

def create_collection(collection_name: str, fields: List[FieldSchema]):
    # fastAPI를 시작할 때, 기존에 저장되 있던 컬렉션은 삭제
    if utility.has_collection(collection_name):
        collection = Collection(name=collection_name)
        collection.drop()

    # 컬렉션 생성
    schema = CollectionSchema(fields, f"{collection_name}_vectors")
    collection = Collection(collection_name, schema)
    return collection

def search_similar_items(query_vec, collection, result_num):
    search_params = {"metric_type": "L2", "params": {"nprobe": 10}}
    results = collection.search([query_vec], "vector", search_params, limit=result_num)
    similar_item_ids = [res.id for res in results[0].ids]
    return similar_item_ids