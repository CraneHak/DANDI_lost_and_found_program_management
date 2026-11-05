#!/bin/bash
# GitHub Actions가 SSM으로 EC2(root)에서 실행한다. 사용법: server-deploy.sh <버킷> <jar 키> <커밋 태그>
set -euo pipefail

BUCKET="$1"
KEY="$2"
TAG="$3"
DIR=/home/ec2-user/dandi
HEALTH=http://127.0.0.1:8080/api/public/health
KEEP_BACKUPS=5

aws s3 cp "s3://$BUCKET/$KEY" "$DIR/app.jar.new" --only-show-errors --region ap-northeast-2
chown ec2-user:ec2-user "$DIR/app.jar.new"

BACKUP="$DIR/app.jar.bak-$(date +%Y%m%d-%H%M)-pre-$TAG"
cp -p "$DIR/app.jar" "$BACKUP"
mv "$DIR/app.jar.new" "$DIR/app.jar"
systemctl restart dandi

# 기동 ~21초. 최대 90초 기다린다.
for _ in $(seq 1 30); do
  sleep 3
  if curl -fsS "$HEALTH" >/dev/null 2>&1; then
    echo "배포 성공: $TAG"
    # 디스크(8GB) 보호: 최신 백업 $KEEP_BACKUPS개만 남긴다
    ls -1t "$DIR"/app.jar.bak-* 2>/dev/null | tail -n +$((KEEP_BACKUPS + 1)) | xargs -r rm -f || true
    exit 0
  fi
done

echo "헬스체크 실패 → 이전 jar로 되돌립니다. (DB 표/칸 변경이 RDS에 먼저 반영됐는지 확인하세요)"
journalctl -u dandi -n 60 --no-pager || true
cp -p "$BACKUP" "$DIR/app.jar"
systemctl restart dandi
exit 1
