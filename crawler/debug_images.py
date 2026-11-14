"""
이미지가 있어야 하는 게시글의 실제 링크/이미지 구조를 출력하는 디버그 스크립트
"""
import os
from dotenv import load_dotenv
from playwright.sync_api import sync_playwright

load_dotenv()

LOGIN_URL  = "https://portal.dankook.ac.kr/login.jsp"
BASE_URL   = "https://portal.dankook.ac.kr"
STUDENT_ID = os.getenv("DANKOOK_ID")
PASSWORD   = os.getenv("DANKOOK_PW")

# 이미지가 있어야 하는 게시글 번호 (예: 9번, 37번 등 이전에 이미지가 수집됐던 것)
TARGET_POST_NO = "50"

def main():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False)
        page = browser.new_page()

        page.goto(LOGIN_URL)
        page.wait_for_load_state("networkidle")
        page.fill("input[name='user_id']", STUDENT_ID)
        page.fill("input[name='user_password']", PASSWORD)
        page.get_by_role("button", name="로그인").click()
        page.wait_for_load_state("networkidle")
        print("로그인 완료\n")

        url = f"{BASE_URL}/p/LOST01?b=56&ls=20&ln=1&dm=r&p={TARGET_POST_NO}"
        page.goto(url)
        page.wait_for_load_state("networkidle")

        print("=== 본문 텍스트 (앞 500자) ===")
        print(page.inner_text("body")[:500])

        print("\n=== <img> 태그 src 목록 ===")
        for img in page.query_selector_all("img"):
            src = img.get_attribute("src") or ""
            print(f"  {src}")

        print("\n=== <a> 태그 href 목록 ===")
        for a in page.query_selector_all("a[href]"):
            href = a.get_attribute("href") or ""
            text = a.inner_text().strip()[:40]
            print(f"  [{text}] {href}")

        browser.close()

if __name__ == "__main__":
    main()
