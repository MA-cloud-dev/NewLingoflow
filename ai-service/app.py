from flask import Flask, request, jsonify
from flask_cors import CORS
from config import Config
from services.article_service import generate_article
from services.sentence_service import evaluate_sentence

app = Flask(__name__)
CORS(app)

@app.route('/health', methods=['GET'])
def health_check():
    """健康检查"""
    return jsonify({"status": "ok", "service": "LingoFlow AI Service"})

@app.route('/api/generate-article', methods=['POST'])
def api_generate_article():
    """
    生成包含指定单词的英文文章
    
    Request Body:
    {
        "words": [{"word": "ephemeral", "meaningCn": "短暂的"}],
        "difficulty": "medium",
        "length": "short"
    }
    """
    try:
        data = request.get_json()
        
        if not data or 'words' not in data:
            return jsonify({"code": 400, "msg": "缺少 words 参数", "data": None}), 400
        
        words = data['words']
        difficulty = data.get('difficulty', 'medium')
        length = data.get('length', 'short')
        theme = data.get('theme')
        
        if len(words) < 1:
            return jsonify({"code": 400, "msg": "至少需要 1 个单词", "data": None}), 400
        
        result = generate_article(words, difficulty, length, theme)
        
        if result['success']:
            return jsonify({"code": 200, "msg": "success", "data": result['data']})
        else:
            return jsonify({"code": 500, "msg": result['error'], "data": None}), 500
            
    except Exception as e:
        return jsonify({"code": 500, "msg": str(e), "data": None}), 500

@app.route('/api/evaluate-sentence', methods=['POST'])
def api_evaluate_sentence():
    """
    评估用户造句
    
    Request Body:
    {
        "word": "ephemeral",
        "meaning": "短暂的",
        "sentence": "The beauty of cherry blossoms is ephemeral."
    }
    """
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({"code": 400, "msg": "请求体为空", "data": None}), 400
        
        word = data.get('word')
        meaning = data.get('meaning')
        sentence = data.get('sentence')
        
        if not word or not sentence:
            return jsonify({"code": 400, "msg": "缺少必要参数", "data": None}), 400
        
        # 检查句子是否包含目标单词
        if word.lower() not in sentence.lower():
            return jsonify({
                "code": 400, 
                "msg": f"句子中未包含目标单词 '{word}'", 
                "data": None
            }), 400
        
        result = evaluate_sentence(word, meaning or "", sentence)
        
        if result['success']:
            return jsonify({"code": 200, "msg": "success", "data": result['data']})
        else:
            return jsonify({"code": 500, "msg": result['error'], "data": None}), 500
            
    except Exception as e:
        return jsonify({"code": 500, "msg": str(e), "data": None}), 500

if __name__ == '__main__':
    print(f"🚀 LingoFlow AI Service starting on port {Config.FLASK_PORT}")
    print(f"📡 Using model: {Config.SILICONFLOW_MODEL}")
    app.run(host='0.0.0.0', port=Config.FLASK_PORT, debug=True)
