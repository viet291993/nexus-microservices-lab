# Stage 1: Build
FROM node:22-bookworm-slim AS build
RUN apt-get update && apt-get upgrade -y && apt-get clean
WORKDIR /app

# Copy package files
COPY services/inventory-service/package*.json ./
RUN npm install

# Copy source and build
COPY services/inventory-service/ .
# Build và thực hiện prune --omit=dev để giảm kích thước image, chỉ giữ lại production dependencies
RUN npm run build && npm prune --omit=dev

# Stage 2: Runtime - Sử dụng gcr.io/distroless/nodejs22 để tối thiểu hóa bề mặt tấn công
FROM gcr.io/distroless/nodejs22-debian12:latest
WORKDIR /app

# Tuyệt đối không copy mã nguồn (.ts, .java), chỉ copy file đã biên dịch (.js, .class)
COPY --from=build /app/dist ./dist
COPY --from=build /app/node_modules ./node_modules
COPY --from=build /app/package*.json ./

EXPOSE 3000

# Chạy container dưới quyền user không đặc quyền (nonroot - UID 65532 của Distroless)
# Tránh các rủi ro leo thang đặc quyền (Privilege Escalation)
USER 65532

# Sử dụng CMD trực tiếp giúp image khởi động nhanh và nhận tín hiệu SIGTERM đúng cách
CMD ["dist/main.js"]
