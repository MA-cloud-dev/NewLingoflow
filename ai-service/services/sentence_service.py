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
    prompt = f"""你是一位专业的英语教师，正在批改学生使用单词"{word}"（意思：{meaning}）造的句子。

学生的句子："{sentence}"

请从以下三个方面评估这个句子：
1. 语法正确性
2. 目标单词"{word}"的使用是否恰当
3. 句子的表达是否清晰自然

**重要：所有评价内容必须使用中文书写**，只有在给出英文例句建议时才使用英文。

输出格式（JSON）：
{{
    "score": 0-100的分数,
    "isCorrect": true或false（80分以上为true）,
    "feedback": {{
        "grammar": "语法评价（中文）",
        "usage": "单词使用评价（中文）",
        "suggestion": "改进建议（中文，可包含英文例句）"
    }}
}}

请给出你的评价："""

    try:
        response = client.chat.completions.create(
            model=Config.SILICONFLOW_MODEL,
            messages=[
                {"role": "system", "content": "你是一位专业的英语教师，帮助学生提高英语写作能力。请始终以有效的JSON格式回复，评价内容使用中文。"},
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
