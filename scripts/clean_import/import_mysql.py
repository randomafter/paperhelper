"""
将清洗后的 materials_cleaned.json 导入 MySQL。
需先执行 docs/schema.sql 建表，并配置下方数据库连接。
"""
import os
import json
import pymysql

# 数据库配置（与 backend application.yml 一致）
DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "root",
    "database": "history_creation",
    "charset": "utf8mb4",
}

def main():
    output_dir = os.path.join(os.path.dirname(__file__), "output")
    path = os.path.join(output_dir, "materials_cleaned.json")
    if not os.path.isfile(path):
        print(f"Run clean.py first to generate {path}")
        return
    with open(path, "r", encoding="utf-8") as f:
        materials = json.load(f)
    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor() as cur:
            for m in materials:
                cur.execute(
                    "INSERT INTO material (dynasty, category, title, content, source_url) VALUES (%s, %s, %s, %s, %s)",
                    (
                        m.get("dynasty") or "汉",
                        m.get("category") or "文化",
                        m.get("title") or "未命名",
                        m.get("content") or "",
                        m.get("source_url"),
                    ),
                )
        conn.commit()
        print(f"Imported {len(materials)} materials into MySQL.")
    finally:
        conn.close()

if __name__ == "__main__":
    main()
