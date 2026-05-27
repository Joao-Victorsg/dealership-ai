# ---- Base deps stage -------------------------------------------------------
FROM node:22-alpine AS deps

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

# ---- Build stage -----------------------------------------------------------
FROM node:22-alpine AS builder

WORKDIR /app
ENV NEXT_TELEMETRY_DISABLED=1
ENV BFF_URL=http://host.docker.internal:8083
ENV NEXT_PUBLIC_BFF_URL=https://app.localhost:4443
ENV NEXT_PUBLIC_APP_URL=https://app.localhost:4443
ENV NEXT_PUBLIC_KEYCLOAK_URL=https://auth.localhost:4443

COPY --from=deps /app/node_modules ./node_modules
COPY . .

RUN npm run build

# ---- Runtime stage ---------------------------------------------------------
FROM node:22-alpine AS runner

WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1

COPY package.json package-lock.json ./
RUN npm ci --omit=dev && npm cache clean --force

COPY --from=builder /app/public ./public
COPY --from=builder /app/.next ./.next
COPY --from=builder /app/next.config.ts ./next.config.ts

EXPOSE 3000

CMD ["npx", "next", "start", "-H", "0.0.0.0", "-p", "3000"]
