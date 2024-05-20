# main.py
from fastapi import FastAPI
from pydantic import BaseModel
import redis
import random
from .services import load_and_vectorize_data, get_similar_animals  # 같은 패키지 내에서 상대 경로 사용
from contextlib import asynccontextmanager

app = FastAPI()

# Redis 설정
r = redis.Redis(host='localhost', port=6379, db=0, decode_responses=True)

class RecommendRequest(BaseModel):
    user_id: int

@asynccontextmanager
async def lifespan(app: FastAPI):
    global tfidf_matrix, animal_index
    tfidf_matrix, animal_index = await load_and_vectorize_data()
    yield

@app.post("/recommend/animal")
async def recommend(request: RecommendRequest):
    key = f"animalSearch:{request.user_id}"
    animal_ids_str = r.lrange(key, 0, -1)

    animal_ids = list(map(int, animal_ids_str))
    unique_ids = set()

    for animal_id in animal_ids:
        recommended_animals = await get_similar_animals(animal_id, animal_index, tfidf_matrix)
        unique_ids.update(recommended_animals)

    unique_list = list(unique_ids)

    # 5개가 넘어가면, 5개의 동물을 랜덤으로 골라서 추천해준다
    if len(unique_list) > 5:
        recommended_animals = random.sample(unique_list, 5)
    else:
        recommended_animals = unique_list  

    return {"recommendedAnimals": recommended_animals}