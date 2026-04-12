import os
import re
import pymysql
from dotenv import load_dotenv
from playwright.sync_api import sync_playwright

load_dotenv()

LOGIN_URL = "https://portal.dankook.ac.kr/login.jsp"
BASE_URL  = "https://portal.dankook.ac.kr"

STUDENT_ID = os.getenv("DANKOOK_ID")
PASSWORD   = os.getenv("DANKOOK_PW")

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
    """목록 페이지를 순회하며 (url, list_title) 수집"""
    posts = []
    for ln in range(1, 4):  # 3페이지 (48건 / 20건)
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
            if post_no.isdigit():
                # 제목 셀(index 2)에서 텍스트 추출
                list_title = cells[2].inner_text().strip()
                url = f"{BASE_URL}/p/LOST01?b=56&ls=20&ln={ln}&dm=r&p={post_no}"
                posts.append((url, list_title))

    print(f"총 {len(posts)}개 게시물 수집")
    return posts


LOCATION_PATTERN = r"\d+호|\d+층|도서관|체육관|학생식당|강의실|행정팀|센터$|관$|버스|광장|정류장"
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


def parse_detail(page, url, list_title=""):
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

    found_location  = normalize_location(extract_field(body_text, "습득장소"))
    stored_location = normalize_location(extract_field(body_text, "보관장소"))
    stored_date     = normalize_date(extract_field(body_text, "습득일시") or extract_field(body_text, "습득일"))
    contact         = extract_field(body_text, "보관장소 연락처")
    special_note    = extract_field(body_text, "특이사항")

    color     = extract_color((item_name or "") + " " + (special_note or ""))
    item_type = item_name  # 물품명이 곧 종류

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
    with conn.cursor() as cur:
        cur.execute("DROP TABLE IF EXISTS lost_item")
        cur.execute("""
            CREATE TABLE lost_item (
                id              INT AUTO_INCREMENT PRIMARY KEY,
                item_name       VARCHAR(255),
                found_location  VARCHAR(255),
                stored_location VARCHAR(255),
                stored_date     DATE,
                contact         VARCHAR(100),
                color           VARCHAR(50),
                item_type       VARCHAR(255)
            ) CHARACTER SET utf8mb4
        """)
    conn.commit()


def save_to_db(items):
    conn = pymysql.connect(**DB_CONFIG)
    try:
        init_db(conn)
        with conn.cursor() as cur:
            for item in items:
                cur.execute("""
                    INSERT INTO lost_item
                        (item_name, found_location, stored_location, stored_date,
                         contact, color, item_type)
                    VALUES
                        (%(item_name)s, %(found_location)s, %(stored_location)s,
                         %(stored_date)s, %(contact)s, %(color)s, %(item_type)s)
                """, item)
        conn.commit()
        print(f"{len(items)}개 저장 완료")
    finally:
        conn.close()


def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)
        page = browser.new_page()

        login(page)
        posts = get_posts(page)

        items = []
        for i, (url, list_title) in enumerate(posts, 1):
            try:
                item = parse_detail(page, url, list_title)
                items.append(item)
                print(f"[{i}/{len(posts)}] 파싱 완료: {item['item_name']}")
            except Exception as e:
                print(f"[{i}/{len(posts)}] 파싱 실패 ({url}): {e}")

        browser.close()

    save_to_db(items)


if __name__ == "__main__":
    main()
