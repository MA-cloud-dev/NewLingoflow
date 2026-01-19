import os
from dotenv import load_dotenv

load_dotenv()

class Config:
    SILICONFLOW_API_KEY = os.getenv('SILICONFLOW_API_KEY')
    SILICONFLOW_BASE_URL = os.getenv('SILICONFLOW_BASE_URL', 'https://api.siliconflow.cn/v1')
    SILICONFLOW_MODEL = os.getenv('SILICONFLOW_MODEL', 'Qwen/Qwen2.5-7B-Instruct')
    FLASK_PORT = int(os.getenv('FLASK_PORT', 5000))
