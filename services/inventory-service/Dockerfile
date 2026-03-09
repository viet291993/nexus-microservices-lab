# Stage 1: Build
FROM node:24-alpine AS build
WORKDIR /app

# Copy package files
COPY services/inventory-service/package*.json ./
RUN npm install

# Copy source and build
COPY services/inventory-service/ .
RUN npm run build

# Stage 2: Runtime
FROM node:24-alpine
WORKDIR /app
COPY --from=build /app/dist ./dist
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/package*.json ./

EXPOSE 3000
CMD ["npm", "run", "start:prod"]
