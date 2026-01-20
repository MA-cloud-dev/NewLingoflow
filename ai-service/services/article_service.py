from openai import OpenAI
from config import Config

client = OpenAI(
    api_key=Config.SILICONFLOW_API_KEY,
    base_url=Config.SILICONFLOW_BASE_URL
)

def generate_article(words: list, difficulty: str = "medium", length: str = "short", theme: str = None) -> dict:
    """
    生成包含指定单词的英文文章
    
    Args:
        words: 单词列表，每个元素包含 word 和 meaningCn
        difficulty: 难度 (easy/medium/hard)
        length: 长度 (short/medium/long)
        theme: 主题 (可选)
    
    Returns:
        dict: 包含 title, content, highlightWords
    """
    word_list = ", ".join([w["word"] for w in words])
    word_details = "\n".join([f"- {w['word']}: {w['meaningCn']}" for w in words])
    
    length_guide = {
        "short": "150-200 words",
        "medium": "250-350 words",
        "long": "400-500 words"
    }
    
    difficulty_guide = {
        "easy": "simple vocabulary and short sentences, suitable for beginners",
        "medium": "moderate vocabulary with some complex sentences",
        "hard": "advanced vocabulary and sophisticated sentence structures"
    }
    
    theme_instruction = f"The article should be about {theme}." if theme else "Choose an interesting topic suitable for the context."

    prompt = f"""You are an English teacher creating a complete reading lesson. 
Generate a JSON response containing an engaging article, comprehension questions, and sentence making tasks.

Target Vocabulary:
{word_details}

Requirements:
1. Article: ~{length_guide.get(length, "200 words")}, {difficulty_guide.get(difficulty, "moderate")}. Theme: {theme_instruction}. Incorporate ALL target words naturally.
2. Comprehension Questions:
   - Generate 2 'word_comprehension' multiple-choice questions focusing on the usage of target words in context.
   - Generate 1 'main_idea' multiple-choice question about the article.
   - Each question must have 4 options (A-D), a correct answer, and an explanation.
3. Sentence Making Tasks:
   - For EACH target word, provide a 'sentence_making' task.
   - Include a specific theme/context for the sentence (related to the article or general usage).
   - Provide a Chinese example sentence as inspiration (but NOT a direct translation of the target word usage, just a context guide).

Output JSON Format:
{{
    "title": "Article Title",
    "content": "Article content with **highlighted** target words...",
    "chineseTranslation": "Full Chinese translation of the article...",
    "highlightWords": ["{word_list}"],
    "comprehensionQuestions": [
        {{
            "type": "word_comprehension",
            "word": "target_word_if_applicable", 
            "question": "What does '...' mean in the context?",
            "options": ["A. ...", "B. ...", "C. ...", "D. ..."],
            "correctAnswer": "B",
            "explanation": "..."
        }},
        {{
            "type": "main_idea",
            "question": "What is the main idea?",
            "options": ["A...", "B...", "C...", "D..."],
            "correctAnswer": "C",
            "explanation": "..."
        }}
    ],
    "sentenceMakingTasks": [
        {{
            "word": "target_word",
            "theme": "Business Context",
            "chineseExample": "这家公司..."
        }}
    ]
}}

Generate the JSON now:"""

    try:
        response = client.chat.completions.create(
            model=Config.SILICONFLOW_MODEL,
            messages=[
                {"role": "system", "content": "You are a helpful English teacher. Always respond with valid JSON."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.7,
            max_tokens=1000
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
