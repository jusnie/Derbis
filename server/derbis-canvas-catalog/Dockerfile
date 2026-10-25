FROM node:20-alpine

WORKDIR /app
COPY package.json ./
COPY src ./src
COPY catalog ./catalog
RUN mkdir -p /data/media

ENV NODE_ENV=production \
    PORT=8789 \
    CATALOG_FILE=/data/catalog.json \
    MEDIA_DIR=/data/media
EXPOSE 8789
USER node
CMD ["node", "src/server.mjs"]
