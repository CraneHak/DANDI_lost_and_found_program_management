# GitHub Actions 워크플로

`deploy.yml`: `main`에 푸시하면 테스트와 빌드 후 S3와 SSM으로 EC2에 배포하고, 헬스체크가 실패하면 이전 버전으로 되돌립니다. `.md`와 `crawler/`만 바뀐 푸시는 배포하지 않습니다.
