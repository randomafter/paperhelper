"""
清洗脚本：去重、统一字段、输出结构化 JSON/CSV。
"""
import os
import json
import hashlib
from pathlib import Path

# 原始数据目录（爬虫输出）
RAW_DIR = os.path.join(os.path.dirname(__file__), "..", "crawler", "output")
OUTPUT_DIR = "output"

def ensure_output_dir():
    Path(OUTPUT_DIR).mkdir(parents=True, exist_ok=True)

def content_hash(item: dict) -> str:
    s = f"{item.get('dynasty','')}|{item.get('category','')}|{item.get('title','')}|{item.get('content','')[:500]}"
    return hashlib.md5(s.encode("utf-8")).hexdigest()

def clean_item(item: dict) -> dict:
    title = (item.get("title") or "").strip() or "未命名"
    content = (item.get("content") or "").strip() or ""
    if len(content) > 10000:
        content = content[:10000] + "…"
    return {
        "dynasty": (item.get("dynasty") or "").strip() or "汉",
        "category": (item.get("category") or "").strip() or "文化",
        "title": title[:200],
        "content": content,
        "source_url": (item.get("source_url") or "").strip()[:512] or None,
    }

def main():
    ensure_output_dir()
    raw_path = os.path.join(RAW_DIR, "raw_materials.json")
    if not os.path.isfile(raw_path):
        print(f"Raw file not found: {raw_path}. Run crawler first.")
        return
    with open(raw_path, "r", encoding="utf-8") as f:
        raw = json.load(f)
    seen = set()
    cleaned = []
    for item in raw:
        c = clean_item(item)
        h = content_hash(c)
        if h in seen:
            continue
        seen.add(h)
        cleaned.append(c)
    out_json = os.path.join(OUTPUT_DIR, "materials_cleaned.json")
    with open(out_json, "w", encoding="utf-8") as f:
        json.dump(cleaned, f, ensure_ascii=False, indent=2)
    out_csv = os.path.join(OUTPUT_DIR, "materials_cleaned.csv")
    with open(out_csv, "w", encoding="utf-8-sig", newline="") as f:
        f.write("dynasty,category,title,content,source_url\n")
        for c in cleaned:
            content_esc = (c["content"] or "").replace('"', '""')
            f.write(f'"{c["dynasty"]}","{c["category"]}","{(c["title"] or "").replace(chr(34), chr(34)+chr(34))}","{content_esc}","{c.get("source_url") or ""}"\n')
    print(f"Cleaned {len(cleaned)} items -> {out_json}, {out_csv}")

if __name__ == "__main__":
    main()
