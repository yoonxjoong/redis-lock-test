# Redis의 공식 이미지를 기반으로 합니다.
FROM redis:latest

# Redis의 기본 포트인 6379를 노출합니다.
EXPOSE 6378

# Redis 서버를 실행합니다.
CMD ["redis-server"]