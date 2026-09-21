# syntax=docker/dockerfile:1

FROM node:22-alpine AS build
WORKDIR /workspace

COPY package.json package-lock.json ./
RUN npm ci

COPY . .

ARG VITE_API_BASE_URL=/api
ARG VITE_KAKAO_MAP_APP_KEY=
ARG VITE_TOSS_PAYMENTS_CLIENT_KEY=
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_KAKAO_MAP_APP_KEY=${VITE_KAKAO_MAP_APP_KEY}
ENV VITE_TOSS_PAYMENTS_CLIENT_KEY=${VITE_TOSS_PAYMENTS_CLIENT_KEY}

RUN npm run build

FROM nginx:alpine AS runtime
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://127.0.0.1/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
