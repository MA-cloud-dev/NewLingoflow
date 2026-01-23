import json
import argparse
import sys
import mysql.connector
from datetime import datetime

# Database Configuration (defaults matching application.yml)
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '123456',  # Default password from application.yml
    'database': 'newlingoflow'
}

def connect_db():
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        return conn
    except mysql.connector.Error as err:
        print(f"Error connecting to database: {err}")
        sys.exit(1)

import json
import argparse
import sys
import os
import glob
import mysql.connector

# Database Configuration (defaults matching application.yml)
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '123456',  # Default password from application.yml
    'database': 'newlingoflow'
}

def connect_db():
    try:
        conn = mysql.connector.connect(**DB_CONFIG)
        return conn
    except mysql.connector.Error as err:
        print(f"Error connecting to database: {err}")
        sys.exit(1)

def process_file(file_path, cursor, dict_id):
    print(f"Processing file: {file_path}")
    try:
        with open(file_path, 'r', encoding='utf-8-sig') as f: # Use utf-8-sig to handle BOM
            words_data = json.load(f)
    except Exception as e:
        print(f"Error reading JSON file {file_path}: {e}")
        return 0, 0

    local_added = 0
    local_linked = 0

    for item in words_data:
        # Flexible parsing based on common formats
        word_text = item.get('word') or item.get('headWord')
        if not word_text:
            continue
            
        word_text = word_text.strip() # Clean whitespace
        
        # Mapping logic for Vocabulary-of-CET-4
        phonetic = item.get('phonetic') or item.get('usphone') or item.get('ukphone') or item.get('phonetic_symbol') or ''
        # Clean phonetic if it has brackets
        if phonetic and not phonetic.startswith('/'):
            phonetic = f"/{phonetic}/"
            
        meaning = item.get('trans') or item.get('meaning') or item.get('mean') or ''
        if isinstance(meaning, list):
            meaning = "; ".join(meaning)
            
        # Fallback for meaning_en if available, else empty
        meaning_en = item.get('definition') or ''
        
        # Example sentence
        example = ""
        if 'examples' in item and item['examples']:
            # Assume examples is list of dicts or strings
            ex = item['examples'][0]
            if isinstance(ex, dict):
                example = ex.get('sentence', '')
            elif isinstance(ex, str):
                example = ex
        
        # Insert Word
        try:
            cursor.execute("""
                INSERT INTO words (word, phonetic, meaning_cn, meaning_en, example_sentence)
                VALUES (%s, %s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE
                    phonetic = IF(phonetic IS NULL OR phonetic = '', VALUES(phonetic), phonetic),
                    meaning_cn = IF(meaning_cn IS NULL OR meaning_cn = '', VALUES(meaning_cn), meaning_cn),
                    meaning_en = IF(meaning_en IS NULL OR meaning_en = '', VALUES(meaning_en), meaning_en),
                    example_sentence = IF(example_sentence IS NULL OR example_sentence = '', VALUES(example_sentence), example_sentence)
            """, (word_text, phonetic, meaning, meaning_en, example))
            local_added += 1 # Rough count, includes updates
        except mysql.connector.Error as err:
            print(f"Error inserting word {word_text}: {err}")
            continue
        
        # Get Word ID
        cursor.execute("SELECT id FROM words WHERE word = %s", (word_text,))
        res = cursor.fetchone()
        if not res:
            continue
        word_id = res[0]
        
        # Link to Dictionary
        try:
            cursor.execute("""
                INSERT INTO word_dictionary_tags (word_id, dictionary_id)
                VALUES (%s, %s)
            """, (word_id, dict_id))
            local_linked += 1
        except mysql.connector.Error as err:
            if err.errno == 1062: # Duplicate entry
                pass
            else:
                print(f"Error linking word {word_text}: {err}")

    return local_added, local_linked

def import_words(input_source, dictionary_name, dictionary_description=None):
    conn = connect_db()
    cursor = conn.cursor()

    try:
        # 1. Ensure Dictionary exists
        print(f"Ensuring dictionary '{dictionary_name}' exists...")
        cursor.execute(
            "SELECT id FROM dictionaries WHERE name = %s",
            (dictionary_name,)
        )
        result = cursor.fetchone()
        
        if result:
            dict_id = result[0]
            print(f"Found existing dictionary ID: {dict_id}")
            if dictionary_description:
                 cursor.execute(
                    "UPDATE dictionaries SET description = %s WHERE id = %s",
                    (dictionary_description, dict_id)
                )
        else:
            cursor.execute(
                "INSERT INTO dictionaries (name, description, total_words) VALUES (%s, %s, 0)",
                (dictionary_name, dictionary_description or dictionary_name)
            )
            dict_id = cursor.lastrowid
            print(f"Created new dictionary ID: {dict_id}")

        # 2. Process Files
        total_added = 0
        total_linked = 0
        
        if os.path.isdir(input_source):
            # Process all JSON files in directory
            files = glob.glob(os.path.join(input_source, "*.json"))
            print(f"Found {len(files)} JSON files in directory.")
            for f in files:
                a, l = process_file(f, cursor, dict_id)
                total_added += a
                total_linked += l
        else:
            # Process single file
            a, l = process_file(input_source, cursor, dict_id)
            total_added += a
            total_linked += l

        # Update Dictionary total count
        cursor.execute("""
            UPDATE dictionaries 
            SET total_words = (SELECT COUNT(*) FROM word_dictionary_tags WHERE dictionary_id = %s)
            WHERE id = %s
        """, (dict_id, dict_id))
        
        conn.commit()
        print(f"Import process completed.")
        print(f"Total operations (insert/update): {total_added}")
        print(f"Total new associations: {total_linked}")
        
    except mysql.connector.Error as err:
        print(f"Database error: {err}")
        conn.rollback()
    finally:
        cursor.close()
        conn.close()

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Import words into LingoFlow database.')
    parser.add_argument('source', help='Path to the JSON file or directory containing JSON files')
    parser.add_argument('--name', '-n', required=True, help='Name of the dictionary (e.g., CET-4)')
    parser.add_argument('--desc', '-d', help='Description of the dictionary')
    
    args = parser.parse_args()
    
    import_words(args.source, args.name, args.desc)
