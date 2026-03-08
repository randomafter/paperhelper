"""
汉/唐/宋/明 历史特色素材采集示例。
从本地或公开数据源抓取条目，输出原始 JSON 供清洗脚本使用。
实际生产可替换为具体百科/古籍站点解析逻辑。
"""
import os
import json
import random
from pathlib import Path

# 若需真实抓取，可取消注释并使用：
# import requests
# from bs4 import BeautifulSoup

from config import DYNASTIES, CATEGORIES, OUTPUT_DIR

def ensure_output_dir():
    Path(OUTPUT_DIR).mkdir(parents=True, exist_ok=True)

def fetch_dynasty_sample(dynasty: str, count: int) -> list:
    """
    示例：生成该朝代的样本条目（占位）。真实场景改为 requests + BeautifulSoup 抓取。
    """
    items = []
    for i in range(count):
        cat = random.choice(CATEGORIES)
        items.append({
            "dynasty": dynasty,
            "category": cat,
            "title": f"{dynasty}朝{cat}相关条目_{i+1}",
            "content": f"此处为{dynasty}朝{cat}类素材的正文摘要。实际爬虫应从页面提取正文。",
            "source_url": f"https://example.com/{dynasty}/{cat}/{i+1}",
        })
    return items

def main():
    ensure_output_dir()
    all_items = []
    for dynasty in DYNASTIES:
        # 每朝代约 250+ 条可凑够 1000+
        items = fetch_dynasty_sample(dynasty, 260)
        all_items.extend(items)
    out_path = os.path.join(OUTPUT_DIR, "raw_materials.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(all_items, f, ensure_ascii=False, indent=2)
    print(f"Written {len(all_items)} raw items to {out_path}")

if __name__ == "__main__":
    main()
