#!/usr/bin/env bash
set -e
cd "$(dirname "${BASH_SOURCE[0]}")/../deploy"
docker compose up -d
echo "等待数据库就绪..."
until docker exec eeip-postgres pg_isready -U eeip -d eeip >/dev/null 2>&1; do sleep 1; done
echo "PostgreSQL / Redis 已就绪"
echo "  PG      : localhost:5432  (eeip / eeip123)"
echo "  Redis   : localhost:6379  (password: eeip123)"
echo "  Adminer : http://localhost:8088"
