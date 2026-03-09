# 🚀 Nexus CI/CD Workflows

Thư mục này chứa các cấu hình GitHub Actions dùng để tự động hóa quy trình kiểm tra (CI) và triển khai (CD) cho hệ thống Nexus Microservices.

## 📋 Danh sách Workflow

### 1. Nexus CI (`ci.yml`)
**Mục đích:** Đảm bảo mã nguồn luôn ổn định sau mỗi commit hoặc Pull Request.
- **Trigger:** Tự động chạy khi có `push` hoặc `pull_request` vào nhánh `main`.
- **Các tác vụ chính:**
    - **Build Maven:** Kiểm tra tính đúng đắn của các service Java (API Gateway, Config, Eureka, Order, Product).
    - **Build Node.js:** Kiểm tra và biên dịch `inventory-service` (NestJS).
    - **Validate Docker:** Kiểm tra cú pháp của file `docker-compose.yml` trong thư mục `infra/`.

### 2. Nexus CD (`cd.yml`)
**Mục đích:** Đóng gói Docker Images và triển khai lên hệ thống thực tế.
- **Trigger:** 
    - Khi có `push` vào nhánh `main`.
    - Khi đánh tag phiên bản mới (`v*`).
    - Chạy thủ công qua nút **Run workflow** trên giao diện GitHub.
- **Các tác vụ chính:**
    - **Build and Push:** Sử dụng Docker Buildx để đóng gói images cho 6 microservices và đẩy lên **GitHub Container Registry (GHCR)**.
    - **Deploy Swarm:** Kết nối SSH tới Server và cập nhật hệ thống sử dụng Docker Swarm.

---

## 🔐 Cấu hình GitHub Secrets

Để workflow **CD** hoạt động, bạn cần cấu hình các biến bảo mật sau trong **Settings > Secrets and variables > Actions**:

| Biến | Ý nghĩa | Ví dụ |
| :--- | :--- | :--- |
| `SWARM_HOST` | IP hoặc Domain của Server | `1.2.3.4` |
| `SWARM_USER` | Tài khoản SSH | `root` hoặc `ubuntu` |
| `SWARM_SSH_KEY` | Nội dung file Private Key | `-----BEGIN RSA PRIVATE KEY----- ...` |

---

## ⚡ Các tối ưu hóa đã áp dụng

- **Security Hardening:** Các action đều được gán commit SHA cố định thay vì tag để tránh tấn công supply-chain.
- **Docker Buildx:** Sử dụng driver `docker-container` để hỗ trợ xuất bộ nhớ đệm (Cache export) lên GitHub Actions một cách ổn định.
- **Cache Management:** Sử dụng `mode=min` trong Docker cache để duy trì tổng dung lượng cache dưới giới hạn 10GB của GitHub.
- **Zero Downtime:** Cấu hình Rolling Update trong Docker Swarm giúp hệ thống không bị gián đoạn khi cập nhật phiên bản mới.

---

## 🛠️ Cách khắc phục lỗi thường gặp

- **Lỗi Cache limit:** Nếu nhận được thông báo đầy bộ nhớ 10GB, hãy vào tab **Actions > Caches** và xóa các cache cũ của những nhánh không còn sử dụng.
- **Lỗi SSH:** Kiểm tra lại format của `SWARM_SSH_KEY` (phải bao gồm cả dòng đầu và cuối của file key). Đảm bảo firewall của server đã mở cổng 22.
