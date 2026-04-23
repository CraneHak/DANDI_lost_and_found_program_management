import os
import re
import pymysql
import boto3
from dotenv import load_dotenv

load_dotenv()

BUCKET     = os.getenv("S3_BUCKET_NAME")
REGION     = os.getenv("AWS_REGION", "ap-northeast-2")
ACCESS_KEY = os.getenv("AWS_ACCESS_KEY")
SECRET_KEY = os.getenv("AWS_SECRET_KEY")
IMAGE_DIR  = os.path.dirname(__file__)

DB_CONFIG = {
    "host":     os.getenv("DB_HOST"),
    "port":     int(os.getenv("DB_PORT", 3306)),
    "db":       os.getenv("DB_NAME"),
    "user":     os.getenv("DB_USER"),
    "password": os.getenv("DB_PASSWORD"),
    "charset":  "utf8mb4",
}

IMAGE_EXTS = (".jpg", ".jpeg", ".png", ".gif", ".webp")


def clean_filenames(folder_path):
    """기존 폴더의 _ 시작 파일명 및 _1 suffix 정리"""
    if not os.path.isdir(folder_path):
        return
    for filename in sorted(os.listdir(folder_path)):
        if not filename.lower().endswith(IMAGE_EXTS):
            continue
        name, ext = os.path.splitext(filename)
        new_name = re.sub(r"^[_\-\s]+", "", name).strip()   # 선행 _ - 제거
        new_name = re.sub(r"_1$", "", new_name).strip()      # 말미 _1 제거
        new_name = new_name or "image"
        new_filename = new_name + ext
        if new_filename != filename:
            old_path = os.path.join(folder_path, filename)
            new_path = os.path.join(folder_path, new_filename)
            if not os.path.exists(new_path):
                os.rename(old_path, new_path)
                print(f"  파일명 정리: {filename} → {new_filename}")


def get_first_image(folder_path):
    """폴더에서 첫 번째 이미지 파일 경로 반환"""
    if not os.path.isdir(folder_path):
        return None
    for f in sorted(os.listdir(folder_path)):
        if f.lower().endswith(IMAGE_EXTS):
            return os.path.join(folder_path, f)
    return None


def upload_to_s3(s3, file_path, item_id):
    """S3에 이미지 업로드 후 URL 반환"""
    ext = os.path.splitext(file_path)[1].lower()
    key = f"lost-items/{item_id}{ext}"
    content_type = {
        ".jpg": "image/jpeg", ".jpeg": "image/jpeg",
        ".png": "image/png", ".gif": "image/gif", ".webp": "image/webp"
    }.get(ext, "image/jpeg")

    s3.upload_file(
        file_path, BUCKET, key,
        ExtraArgs={"ContentType": content_type}
    )
    return f"https://{BUCKET}.s3.{REGION}.amazonaws.com/{key}"


def main():
    s3 = boto3.client(
        "s3",
        region_name=REGION,
        aws_access_key_id=ACCESS_KEY,
        aws_secret_access_key=SECRET_KEY,
    )

    conn = pymysql.connect(**DB_CONFIG)
    try:
        with conn.cursor() as cur:
            cur.execute("SELECT id FROM lost_item WHERE image_url IS NULL ORDER BY id")
            rows = cur.fetchall()

        print(f"image_url이 없는 항목: {len(rows)}개")

        updated = 0
        for (item_id,) in rows:
            folder = os.path.join(IMAGE_DIR, str(item_id))
            clean_filenames(folder)
            image_path = get_first_image(folder)

            if not image_path:
                continue

            try:
                url = upload_to_s3(s3, image_path, item_id)
                with conn.cursor() as cur:
                    cur.execute(
                        "UPDATE lost_item SET image_url = %s WHERE id = %s",
                        (url, item_id)
                    )
                conn.commit()
                updated += 1
                print(f"[{item_id}] 업로드 완료: {url}")
            except Exception as e:
                print(f"[{item_id}] 업로드 실패: {e}")

        print(f"\n총 {updated}개 image_url 업데이트 완료")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
