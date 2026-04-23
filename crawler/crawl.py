import os
import re
import requests
import pymysql
from dotenv import load_dotenv
from playwright.sync_api import sync_playwright

load_dotenv()

LOGIN_URL = "https://portal.dankook.ac.kr/login.jsp"
BASE_URL  = "https://portal.dankook.ac.kr"

STUDENT_ID = os.getenv("DANKOOK_ID")
PASSWORD   = os.getenv("DANKOOK_PW")

IMAGE_DIR = os.path.join(os.path.dirname(__file__))

DB_CONFIG = {
    "host":     os.getenv("DB_HOST", "localhost"),
    "port":     int(os.getenv("DB_PORT", 3306)),
    "db":       os.getenv("DB_NAME", "dandidb"),
    "user":     os.getenv("DB_USER", "dandi"),
    "password": os.getenv("DB_PASSWORD", "dandi1234"),
    "charset":  "utf8mb4",
}


def login(page):
    page.goto(LOGIN_URL)
    page.wait_for_load_state("networkidle")

    page.fill("input[name='user_id']", STUDENT_ID)
    page.fill("input[name='user_password']", PASSWORD)
    page.get_by_role("button", name="로그인").click()
    page.wait_for_load_state("networkidle")
    print("로그인 완료")


def get_posts(page):
    """목록 페이지를 순회하며 (post_no, url, list_title) 수집 - 게시글 번호 오름차순(오래된 순) 정렬"""
    posts = []
    seen = set()
    for ln in range(1, 6):  # 5페이지
        page.goto(f"{BASE_URL}/p/LOST01?b=56&ls=20&ln={ln}&dm=m")
        page.wait_for_load_state("networkidle")

        rows = page.query_selector_all("table tbody tr")
        if not rows:
            break

        for row in rows:
            cells = row.query_selector_all("td")
            if len(cells) < 3:
                continue
            post_no = cells[0].inner_text().strip()
            if post_no.isdigit() and post_no not in seen:
                seen.add(post_no)
                list_title = cells[2].inner_text().strip()
                url = f"{BASE_URL}/p/LOST01?b=56&ls=20&ln={ln}&dm=r&p={post_no}"
                posts.append((post_no, url, list_title))

    # 게시글 번호 오름차순 정렬 (오래된 것 먼저)
    posts.sort(key=lambda x: int(x[0]))
    print(f"총 {len(posts)}개 게시물 수집")
    return posts


LOCATION_PATTERN = r"\d+호|\d+층|도서관|체육관|학생식당|강의실|행정팀|센터$|관$|버스|광장|정류장|화장실|주차장|복도|계단|강당|극장|라운지|매점|편의점|공학관|사무실|열람실|행정실"
NOISE_WORDS = {"습득", "보관", "습득물", "습득물명", "물품명", "없음", ""}


def is_location(text):
    return bool(text and re.search(LOCATION_PATTERN, text))


def extract_field(text, label):
    """본문 텍스트에서 '레이블 : 값' 패턴 추출"""
    pattern = rf"{label}\s*:\s*(.+)"
    match = re.search(pattern, text)
    return match.group(1).strip() if match else None


def normalize_date(raw):
    """다양한 날짜 형식을 YYYY-MM-DD로 통일
    지원 형식:
      26.04.08          → 2026-04-08
      2026. 4. 9(목)    → 2026-04-09
      2026. 4. 8. 점심  → 2026-04-08
      2026.04.08        → 2026-04-08
    """
    if not raw:
        return None
    # 요일, 시간대 등 불필요한 텍스트 제거
    cleaned = re.sub(r"[（(][^)）]*[)）]", "", raw)   # 괄호 안 요일 제거
    cleaned = re.sub(r"(오전|오후|아침|점심|저녁|낮|밤).*", "", cleaned)
    cleaned = cleaned.strip(" .")

    # 패턴 매칭
    patterns = [
        (r"(\d{4})[.\s]+(\d{1,2})[.\s]+(\d{1,2})", "%Y %m %d"),  # 2026. 4. 9
        (r"(\d{2})[.](\d{2})[.](\d{2})",             "yy.mm.dd"),  # 26.04.08
    ]

    for pattern, fmt in patterns:
        m = re.search(pattern, cleaned)
        if m:
            if fmt == "yy.mm.dd":
                return f"20{m.group(1)}-{m.group(2)}-{m.group(3)}"
            else:
                return f"{m.group(1)}-{int(m.group(2)):02d}-{int(m.group(3)):02d}"
    return None


def item_name_from_title(list_title):
    """목록 제목에서 물품명 추출
    예: '[혜당관 학생극장] 무선 이어폰' → '무선 이어폰'
        '[다과세트] 3층 도산라운지(학술정보지원팀)' → '다과세트'
    """
    # 브래킷 이후 텍스트에서 부서명(괄호) 제거
    after = re.sub(r"\(.+?\)\s*$", "", re.sub(r"^\[.+?\]\s*", "", list_title)).strip()
    bracket = re.search(r"\[(.+?)\]", list_title)
    bracket_val = bracket.group(1).strip() if bracket else None

    # after를 먼저 clean 처리 후 유효성 검사
    after_cleaned = clean_item_name(after) if after else None
    if after_cleaned and after_cleaned not in NOISE_WORDS and not is_location(after_cleaned):
        return after_cleaned

    # 브래킷 내용이 유효한 물품명이면 사용
    if bracket_val and bracket_val not in NOISE_WORDS and not is_location(bracket_val):
        return bracket_val
    return None


def download_images(page, seq, item_name=None):
    """게시글 상세 페이지에서 이미지를 다운로드하여 crawler/{seq}/ 에 저장"""
    save_dir = os.path.join(IMAGE_DIR, str(seq))
    os.makedirs(save_dir, exist_ok=True)

    cookies = page.context.cookies()
    cookie_str = "; ".join([f"{c['name']}={c['value']}" for c in cookies])

    # 첨부파일 링크에서 이미지 수집 (<a> 태그)
    img_urls = []
    for a in page.query_selector_all("a[href]"):
        href = a.get_attribute("href") or ""
        link_text = a.inner_text().strip()
        if not href.startswith("http"):
            href = BASE_URL + href
        # 단국대 포털 파일 다운로드 패턴: a=fd
        if "/ctt/bb/bulletin" in href and "a=fd" in href:
            # 링크 텍스트에 이미지 확장자가 포함된 경우만 (이미지 파일인지 확인)
            if any(ext in link_text.lower() for ext in [".jpg", ".jpeg", ".png", ".gif", ".webp"]):
                # 링크 텍스트에서 원본 파일명 추출 (예: "드래곤볼 키링.jpg(3781 KB)" → "드래곤볼 키링.jpg")
                original_name = re.sub(r"\(\d+\s*KB\).*$", "", link_text).strip()
                # 날짜 접두사 제거 (예: "2026.03.19 카드지갑.jpg", "260401 갤럭시.jpg")
                original_name = re.sub(r"^\d{4}[.\-]\d{2}[.\-]\d{2}\s*", "", original_name).strip()
                original_name = re.sub(r"^\d{6}\s*", "", original_name).strip()
                # 분실물/습득물 등 노이즈 단어 제거
                original_name = re.sub(r"\s*(분실물|습득물)\s*", " ", original_name).strip()
                original_name = re.sub(r"\s+", " ", original_name).strip()
                # KakaoTalk 등 무의미한 파일명은 None 처리 → item_name으로 대체
                meaningless = ("KakaoTalk_", "IMG_", "DSC", "image", "photo", "Screenshot")
                if any(original_name.upper().startswith(p.upper()) for p in meaningless):
                    original_name = None
                # 정리 후 확장자만 남으면 None 처리
                if original_name and not re.search(r"[가-힣a-zA-Z0-9]", os.path.splitext(original_name)[0]):
                    original_name = None
                img_urls.append((href, original_name))
        elif any(ext in href.lower() for ext in [".jpg", ".jpeg", ".png", ".gif", ".webp"]):
            img_urls.append((href, None))

    # 페이지 끝까지 스크롤하여 lazy load 이미지 강제 로드
    page.evaluate("window.scrollTo(0, document.body.scrollHeight)")
    page.wait_for_timeout(1000)

    # <img> 태그에서 수집 (src, data-src 모두 확인)
    for img in page.query_selector_all("img"):
        for attr in ("src", "data-src"):
            src = img.get_attribute(attr) or ""
            if not src:
                continue
            if not src.startswith("http"):
                src = BASE_URL + src
            if any(skip in src for skip in ["/img/", "/icon", "/btn", "/logo", "emoticon", "common", "eXPortal"]):
                continue
            # 단국대 게시글 첨부 이미지 패턴
            if "/ctt/bb/bulletin" in src or any(ext in src.lower() for ext in [".jpg", ".jpeg", ".png", ".gif", ".webp"]):
                img_urls.append((src, None))

    saved = []
    idx = 1
    seen_urls = set()
    for src, original_name in img_urls:
        if src in seen_urls:
            continue
        seen_urls.add(src)

        try:
            resp = requests.get(src, headers={"Cookie": cookie_str}, timeout=10)
            content_type = resp.headers.get("Content-Type", "")
            if resp.status_code == 200 and "image" in content_type:
                # 30KB 미만은 UI 이미지로 판단하고 건너뜀
                if len(resp.content) < 30 * 1024:
                    continue
                ext = content_type.split("/")[-1].split(";")[0]
                if ext not in ("jpeg", "jpg", "png", "gif", "webp"):
                    ext = "jpg"
                if original_name:
                    # 원본 파일명 사용 (첨부파일 링크에서 추출)
                    filename = re.sub(r'[\\/*?:"<>|]', "", original_name).strip()
                else:
                    base_name = re.sub(r'[\\/*?:"<>|]', "", item_name or "image").strip() or "image"
                    filename = f"{base_name}_{idx}.{ext}"
                with open(os.path.join(save_dir, filename), "wb") as f:
                    f.write(resp.content)
                saved.append(filename)
                idx += 1
        except Exception as e:
            print(f"  이미지 다운로드 실패 ({src}): {e}")

    return saved


def parse_detail(page, seq, url, list_title=""):
    page.goto(url)
    page.wait_for_load_state("networkidle")

    body_text = page.inner_text("body")

    # 물품명 추출 시도 순서:
    item_name = None
    # 날짜 브래킷: [2026.04.08], [2026.02.25(수)], [2026.03.18] 등
    DATE_BR = r"\[[\d.(월화수목금토일)\s]+\]"

    # 1) "(에서는|께서) [날짜] [물품명]을 습득/보관" (브래킷 있음)
    item_match = re.search(rf"(?:에서는|께서)\s*{DATE_BR}\s*\[(.+?)\][을를]\s*(습득|보관)", body_text)
    if item_match:
        candidate = clean_item_name(item_match.group(1))
        if candidate and not is_location(candidate):
            item_name = candidate

    # 2) "(에서는|께서) [날짜] 물품명을 습득/보관" (브래킷 없음, 30자 이내)
    if not item_name:
        item_match2 = re.search(rf"(?:에서는|께서)\s*{DATE_BR}\s*(.{{1,30}}?)[을를]\s*(습득|보관)", body_text)
        if item_match2:
            candidate = clean_item_name(item_match2.group(1))
            if candidate and not is_location(candidate):
                item_name = candidate

    # 3) fallback: 목록 페이지 제목에서 추출
    if not item_name and list_title:
        item_name = item_name_from_title(list_title)

    # 4) 제목 대괄호 내용이 더 구체적이면 우선 적용 (분실물/습득물 포함 시 건너뜀)
    if list_title:
        bracket = re.search(r"\[(.+?)\]", list_title)
        if bracket:
            bracket_val = bracket.group(1).strip()
            if (bracket_val
                    and not is_location(bracket_val)
                    and bracket_val not in NOISE_WORDS
                    and "분실물" not in bracket_val
                    and "습득물" not in bracket_val):
                item_name = bracket_val

    found_location  = normalize_location(extract_field(body_text, "습득장소"))
    stored_location = normalize_location(extract_field(body_text, "보관장소"))
    stored_date     = normalize_date(extract_field(body_text, "습득일시") or extract_field(body_text, "습득일"))
    contact         = extract_field(body_text, "보관장소 연락처")
    special_note    = extract_field(body_text, "특이사항")

    color     = extract_color((item_name or "") + " " + (special_note or ""))
    item_type = item_name  # 물품명이 곧 종류

    # 이미지 다운로드
    images = download_images(page, seq, item_name)
    if images:
        print(f"  이미지 {len(images)}개 저장: crawler/{seq}/")

    return {
        "item_name":       item_name,
        "found_location":  found_location,
        "stored_location": stored_location,
        "stored_date":     stored_date,
        "contact":         contact,
        "color":           color,
        "item_type":       item_type,
    }


def normalize_location(loc):
    """호실 번호에서 층을 추론하여 삽입
    예: '퇴계기념중앙도서관 401호 자연과학실' → '퇴계기념중앙도서관 4층 401호 자연과학실'
    이미 'N층'이 있으면 그대로 유지
    """
    if not loc:
        return loc

    def insert_floor(m):
        room = m.group(0)          # ex) '401호'
        number = m.group(1)        # ex) '401'
        # 지하(B로 시작)는 건드리지 않음
        if number.startswith("B") or number.startswith("b"):
            return room
        floor = int(number[0]) if number[0].isdigit() else None
        if floor is None:
            return room
        return f"{floor}층 {room}"

    # 이미 '층' 표현이 근처에 있으면 패턴 치환 건너뜀
    # '(\d+)호' 앞에 '\d+층\s*' 없을 때만 삽입
    result = re.sub(
        r"(?<!\d층 )(?<!\d층)(\d{3,4})호",
        insert_floor,
        loc
    )
    return result


def clean_item_name(name):
    """추출된 물품명에서 노이즈 제거"""
    if not name:
        return None
    # 불필요한 후행 문구 제거
    name = re.sub(r'\s*(분실물|습득물|들어와|에서 보관.*|에 보관.*|을 보관.*|를 보관.*).*$', '', name, flags=re.DOTALL).strip()
    name = re.sub(r'\s+습득$|\s+보관$', '', name).strip()
    # 앞뒤 공백/특수문자/브래킷 정리
    name = name.strip(" \t\n\r.,·[]")
    # 의미 없는 단어 단독으로만 남은 경우 None 처리
    if name in ("습득", "보관", "습득물", "습득물명", "물품명", "명", ""):
        return None
    # 한글/영문/숫자가 하나도 없으면 None
    if not re.search(r"[가-힣a-zA-Z0-9]", name):
        return None
    return name


def extract_color(text):
    colors = ["검정", "검은", "흰색", "하얀색", "파란색", "빨간색", "초록색", "노란색",
              "회색", "갈색", "베이지", "블랙", "화이트", "블루", "레드", "그린",
              "옐로우", "그레이", "브라운", "핑크", "보라색", "은색", "금색"]
    for color in colors:
        if color in text:
            return color
    return None


def extract_type(title):
    after_bracket = re.sub(r"\[.+?\]", "", title).strip()
    after_bracket = re.sub(r"\(.+?\)", "", after_bracket).strip()
    return after_bracket if after_bracket else None


def init_db(conn):
    """테이블이 없을 때만 생성 (기존 데이터 유지)"""
    with conn.cursor() as cur:
        cur.execute("""
            CREATE TABLE IF NOT EXISTS lost_item (
                id              INT AUTO_INCREMENT PRIMARY KEY,
                post_no         INT UNIQUE,
                item_name       VARCHAR(255),
                found_location  VARCHAR(255),
                stored_location VARCHAR(255),
                stored_date     DATE,
                contact         VARCHAR(100),
                color           VARCHAR(50),
                item_type       VARCHAR(255),
                image_url       VARCHAR(500)
            ) CHARACTER SET utf8mb4
        """)
    conn.commit()


def get_max_post_no(conn):
    """DB에 저장된 가장 최신 게시글 번호 반환 (없으면 0)"""
    with conn.cursor() as cur:
        cur.execute("SELECT COALESCE(MAX(post_no), 0) FROM lost_item")
        return cur.fetchone()[0]


def save_to_db(items):
    conn = pymysql.connect(**DB_CONFIG)
    try:
        init_db(conn)
        with conn.cursor() as cur:
            for item in items:
                cur.execute("""
                    INSERT INTO lost_item
                        (post_no, item_name, found_location, stored_location, stored_date,
                         contact, color, item_type, image_url)
                    VALUES
                        (%(post_no)s, %(item_name)s, %(found_location)s, %(stored_location)s,
                         %(stored_date)s, %(contact)s, %(color)s, %(item_type)s, %(image_url)s)
                """, {**item, "image_url": None})
        conn.commit()
        print(f"{len(items)}개 저장 완료")
    finally:
        conn.close()


def main():
    # DB 연결해서 마지막 post_no 확인
    conn = pymysql.connect(**DB_CONFIG)
    init_db(conn)
    max_post_no = get_max_post_no(conn)
    conn.close()

    if max_post_no > 0:
        print(f"마지막 저장된 게시글 번호: #{max_post_no} → 이후 게시글만 크롤링")
    else:
        print("첫 실행: 전체 게시글 크롤링")

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)
        page = browser.new_page()

        login(page)
        all_posts = get_posts(page)

        # 이미 저장된 게시글 제외
        new_posts = [(pno, url, title) for pno, url, title in all_posts if int(pno) > max_post_no]
        print(f"새 게시글 {len(new_posts)}개 크롤링 시작")

        # seq는 기존 DB 마지막 id 이후부터 시작
        start_seq = max_post_no + 1
        items = []
        for seq, (post_no, url, list_title) in enumerate(new_posts, start_seq):
            try:
                item = parse_detail(page, seq, url, list_title)
                item["post_no"] = int(post_no)
                items.append(item)
                print(f"[{seq}] 파싱 완료: {item['item_name']} (게시글 #{post_no} → 폴더 {seq})")
            except Exception as e:
                print(f"[{seq}] 파싱 실패 ({url}): {e}")

        browser.close()

    if items:
        save_to_db(items)
    else:
        print("새 게시글이 없습니다.")


if __name__ == "__main__":
    main()
