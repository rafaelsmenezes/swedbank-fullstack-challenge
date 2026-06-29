# Stage 1: Builder
FROM node:22-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Runtime
FROM nginx:alpine

# Install curl for healthchecks (same pattern as backend)
RUN apk add --no-cache curl

# Build arg to select which nginx config to use
# Default: nginx.conf (for Railway/production)
# Override with: --build-arg NGINX_CONF=nginx.local.conf
ARG NGINX_CONF=nginx.conf

COPY --from=builder /app/dist/bank-frontend/browser /usr/share/nginx/html
COPY ${NGINX_CONF} /etc/nginx/conf.d/default.conf

EXPOSE 80

# Healthcheck using the /health endpoint defined in nginx configs
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost/health || exit 1
