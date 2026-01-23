import json
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

    prompt = f"""You are a professional English writer and educator comprising a native speaker's intuition with pedagogical expertise.
Create a high-quality, engaging English reading lesson based on the provided target vocabulary.

Target Vocabulary:
{word_details}

Requirements:
1. Article Generation:
   - Identify a coherent and engaging theme that naturally connects ALL target words.
   - Title: Creative and relevant to the theme.
   - Length: ~{length_guide.get(length, "200 words")}.
   - Difficulty: {difficulty_guide.get(difficulty, "moderate")}.
   - Writing Style: Authentic, fluid, and non-robotic. Avoid forced insertion of words; they must fit the context naturally. The article should be interesting to read on its own.
   - Content: Incorporate ALL target words. Highlight target words in the content using **bold** markdown (e.g., **word**).

2. Chinese Translation:
   - Provide a fluent, natural Chinese translation of the article.
   - Do NOT translate word-for-word; translate the meaning and nuance.

3. Comprehension Questions (3 questions):
   - Q1 & Q2: 'word_comprehension' type. Focus on the subtle meaning or usage of specific target words in the article's context.
   - Q3: 'main_idea' type. Test understanding of the overall theme or a key detail.
   - Options: 4 distinct choices (A-D).
   - Explanation: Clear reasoning for the correct answer.

4. Sentence Making Tasks (One for EACH target word):
   - Create a task to practice using the word in a NEW context (different from the article).
   - Theme: meaningful context (e.g., Business, Daily Life, Academic).
   - Chinese Example: A sentence in Chinese that implies the target word's usage, but is NOT a direct translation. It should serve as a prompt for the user to write the English sentence.

Output JSON Format:
{{
    "title": "Article Title",
    "content": "Article content with **highlighted** target words...",
    "chineseTranslation": "Fluent Chinese translation...",
    "highlightWords": ["{word_list}"],
    "comprehensionQuestions": [
        {{
            "type": "word_comprehension",
            "word": "target_word_if_applicable", 
            "question": "Question text...",
            "options": ["A. ...", "B. ...", "C. ...", "D. ..."],
            "correctAnswer": "B",
            "explanation": "Explanation..."
        }}
    ],
    "sentenceMakingTasks": [
        {{
            "word": "target_word",
            "theme": "Context Theme",
            "chineseExample": "Chinese prompt sentence..."
        }}
    ]
}}

Ensure the response is strictly valid JSON."""

    try:
        response = client.chat.completions.create(
            model=Config.SILICONFLOW_MODEL,
            messages=[
                {"role": "system", "content": "You are a professional English content creator and educator. Response must be valid JSON."},
                {"role": "user", "content": prompt}
            ],
            temperature=0.7,
            max_tokens=2000
        )
        
        content = response.choices[0].message.content.strip()
        
        # Robust JSON extraction
        try:
            # Find the first '{' and last '}'
            start_idx = content.find('{')
            end_idx = content.rfind('}')
            
            if start_idx != -1 and end_idx != -1:
                json_str = content[start_idx : end_idx + 1]
                result = json.loads(json_str)
            else:
                # Fallback to direct parsing if no braces found (unlikely to work but consistent)
                result = json.loads(content)
                
            return {
                "success": True,
                "data": result
            }
        except json.JSONDecodeError as e:
            return {
                "success": False,
                "error": f"JSON Parse Error: {str(e)}. Content: {content[:200]}..."
            }
        
    except Exception as e:
        return {
            "success": False,
            "error": str(e)
        }
