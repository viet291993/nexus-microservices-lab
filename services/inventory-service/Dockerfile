# Stage 1: Build
FROM node:22.13-alpine3.21 AS build
RUN apk update && apk upgrade --no-cache
WORKDIR /app

# Copy package files
COPY services/inventory-service/package*.json ./
RUN npm install

# Copy source and build
COPY services/inventory-service/ .
RUN npm run build

# Stage 2: Runtime
FROM gcr.io/distroless/nodejs22-debian12:latest
WORKDIR /app
COPY --from=build /app/dist ./dist
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/package*.json ./

EXPOSE 3000
# Distroless Node.js runs the file directly
CMD ["dist/main.js"]
