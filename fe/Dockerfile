FROM node:22-alpine AS build

WORKDIR /workspace

RUN apk add --no-cache git

COPY . .

RUN git init && corepack enable && pnpm install --frozen-lockfile

ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

RUN pnpm --filter @pay-with/ui build && pnpm --filter @pay-with/web build

FROM nginx:1.27-alpine

COPY apps/web/nginx/default.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/apps/web/dist /usr/share/nginx/html

EXPOSE 80
