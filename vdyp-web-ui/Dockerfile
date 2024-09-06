# Build static files
# Node Bullseye has npm
FROM node:20.17.0-bullseye-slim AS build

# Install packages, build and keep only prod packages
WORKDIR /app
COPY *.json *.ts index.html ./
COPY public ./public
COPY ./src ./src
RUN npm ci --ignore-scripts --no-update-notifier --omit=dev && \
    npm run build-only

# Deploy using Caddy to host static files
FROM caddy:2.8.4-alpine

# Install packages and ensure permissions in one RUN command
RUN apk add --no-cache ca-certificates && \
    mkdir -p /config/caddy /data/caddy && \
    chown -R 1001:1001 /config/caddy /data/caddy

# Copy static files and run formatting
COPY --from=build /app/dist /srv
COPY Caddyfile /etc/caddy/Caddyfile
RUN caddy fmt --overwrite /etc/caddy/Caddyfile

RUN chmod 755 /usr/bin/caddy

# Ports, health check and non-root user
EXPOSE 3000 3001
HEALTHCHECK --interval=30s --timeout=3s CMD ["curl", "-f", "http://localhost:3001/health"]
USER 1001
