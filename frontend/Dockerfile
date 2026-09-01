# syntax=docker/dockerfile:1.7

FROM node:20.18.1-alpine3.20 AS build
WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27.3-alpine

COPY nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html

RUN sed -i '/^user /d; s#^pid .*;#pid /tmp/nginx.pid;#' /etc/nginx/nginx.conf \
    && mkdir -p /var/cache/nginx \
    && chown -R nginx:nginx /var/cache/nginx

EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=10s --retries=5 \
    CMD wget --quiet --output-document=- http://127.0.0.1:8080/healthz | grep -q '^ok$' || exit 1

USER nginx
