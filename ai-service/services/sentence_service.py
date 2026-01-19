from openai import OpenAI
from config import Config

client = OpenAI(
    api_key=Config.SILICONFLOW_API_KEY,
    base_url=Config.SILICONFLOW_BASE_URL
)

def evaluate_sentence(word: str, meaning: str, sentence: str) -> dict:
    """
    评估用户使用目标单词造的句子
    
    Args:
        word: 目标单词
        meaning: 单词含义
        sentence: 用户造的句子
    
    Returns:
        dict: 包含 score, isCorrect, feedback
    """
    prompt = f"""You are an English teacher evaluating a student's sentence. The student was asked to use the word "{word}" (meaning: {meaning}) in a sentence.

Student's sentence: "{sentence}"

Please evaluate the sentence based on:
1. Grammar correctness
2. Proper usage of the target word "{word}"
3. Sentence clarity and naturalness

Output format (JSON):
{{
    "score": 0-100,
    "isCorrect": true/false,
    "feedback": {{
        "grammar": "Grammar evaluation comment",
        "usage": "Word usage evaluation comment",
        "suggestion": "Improvement suggestion if any"
    }}
}}

Provide your evaluation:"""

    try:
        response = client.chat.completions.create(
            model=Config.SILICONFLOW_MODEL,
            messages=[
                {"role": "system", "content": "You are a helpful English teacher. Always respond with valid JSON."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.3,
            max_tokens=500
        )
        
        content = response.choices[0].message.content.strip()
        
        # 尝试解析 JSON
        import json
        # 处理可能的 markdown 代码块
        if content.startswith("```"):
            content = content.split("```")[1]
            if content.startswith("json"):
                content = content[4:]
        
        result = json.loads(content)
        return {
            "success": True,
            "data": result
        }
        
    except Exception as e:
        return {
            "success": False,
            "error": str(e)
        }
