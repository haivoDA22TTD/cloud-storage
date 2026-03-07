# 📁 Cloud Storage - Hệ Thống Quản Lý File Đám Mây

Ứng dụng quản lý file đám mây với tính năng chia sẻ, xác thực Passkey, và lưu trữ trên Cloudinary.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue)
![Cloudinary](https://img.shields.io/badge/Cloudinary-Storage-blueviolet)

---

## ✨ Tính Năng

### 🔐 Xác Thực & Bảo Mật
- Đăng ký/Đăng nhập truyền thống (email + password)
- Xác thực Passkey (WebAuthn) - Đăng nhập không cần mật khẩu
- Hỗ trợ nhiều thiết bị Passkey
- Session-based authentication với Spring Security
- CSRF protection

### 📂 Quản Lý File
- Upload file lên Cloudinary (giới hạn 50MB/file)
- Download file từ cloud storage
- Xem trước file (ảnh, PDF, video, text)
- Tạo và quản lý thư mục
- Điều hướng thư mục với breadcrumb
- Soft delete (thùng rác)
- Khôi phục file từ thùng rác

### 🤝 Chia Sẻ File
- Chia sẻ file với người dùng khác
- Phân quyền: View (chỉ xem) hoặc Edit (xem + xóa)
- Xem danh sách file được chia sẻ
- Sao chép file được chia sẻ về thư mục của mình
- Hủy chia sẻ

### 🎨 Giao Diện
- UI hiện đại, responsive
- Hỗ trợ tiếng Việt
- Drag & drop upload
- Preview modal cho file
- Animations mượt mà

---

## 🛠️ Công Nghệ Sử Dụng

### Backend
- **Java 17** - Ngôn ngữ lập trình
- **Spring Boot 3.2.0** - Framework
- **Spring Security** - Xác thực & phân quyền
- **Spring Data JPA** - ORM
- **Hibernate** - JPA implementation
- **PostgreSQL** - Database
- **Yubico WebAuthn** - Passkey authentication
- **Cloudinary** - Cloud file storage
- **Lombok** - Giảm boilerplate code

### Frontend
- **Thymeleaf** - Template engine
- **HTML5/CSS3** - Markup & styling
- **JavaScript** - Client-side logic
- **WebAuthn API** - Passkey support

### DevOps
- **Docker** - Containerization
- **Docker Compose** - Multi-container orchestration
- **Maven** - Build tool
- **Render** - Cloud hosting
- **Aiven** - Managed PostgreSQL

---

## 🚀 Cài Đặt & Chạy

### Yêu Cầu
- Java 17+
- Docker & Docker Compose
- Maven 3.8+
- Tài khoản Cloudinary (free tier)

### 1. Clone Repository

```bash
git clone https://github.com/haivoda22ttd/cloud-storage.git
cd cloud-storage
```

### 2. Cấu Hình Environment Variables

Tạo file `.env` từ template:

```bash
cp .env.example .env
```

Cập nhật các giá trị trong `.env`:

```env
# Database (Local)
DB_HOST=localhost
DB_PORT=5432
DB_NAME=demoapp
DB_USER=postgres
DB_PASSWORD=postgres

# Cloudinary
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

### 3. Chạy Với Docker Compose

```bash
docker compose up --build
```

Ứng dụng sẽ chạy tại: **http://localhost:8080**

### 4. Hoặc Chạy Với Maven

```bash
# Build
mvn clean install

# Run
mvn spring-boot:run
```

---

## 📦 Cấu Trúc Project

```
cloud-storage/
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/
│   │   │   ├── config/          # Cấu hình (Security, Cloudinary)
│   │   │   ├── controller/      # REST Controllers
│   │   │   ├── dto/              # Data Transfer Objects
│   │   │   ├── entity/           # JPA Entities
│   │   │   ├── exception/        # Custom Exceptions
│   │   │   ├── repository/       # JPA Repositories
│   │   │   └── service/          # Business Logic
│   │   └── resources/
│   │       ├── application.yml           # Config local
│   │       ├── application-prod.yml      # Config production
│   │       ├── static/                   # Static files
│   │       └── templates/                # Thymeleaf templates
│   └── test/                     # Unit tests
├── docker-compose.yml            # Docker Compose config
├── Dockerfile                    # Docker image (local)
├── Dockerfile.render             # Docker image (production)
├── pom.xml                       # Maven dependencies
└── README.md                     # Documentation
```

---

## 🌐 Deploy Lên Render

### 1. Tạo Tài Khoản
- Đăng ký tại [Render.com](https://render.com)
- Tạo PostgreSQL database trên [Aiven](https://aiven.io)

### 2. Tạo Web Service
1. Connect GitHub repository
2. Chọn branch `main`
3. Build command: Tự động (Docker)
4. Start command: Tự động

### 3. Thêm Environment Variables

```
DB_HOST=host
DB_PORT=5540
DB_NAME=db
DB_USER=user
DB_PASSWORD=your-password

CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```


## 📖 API Documentation

### Authentication

#### Đăng Ký
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "user@example.com",
  "password": "password123",
  "fullName": "Nguyễn Văn A"
}
```

#### Đăng Nhập
```http
POST /api/auth/login
Content-Type: application/x-www-form-urlencoded

username=user@example.com&password=password123
```

### File Management

#### Upload File
```http
POST /api/files
Content-Type: multipart/form-data

file: [binary]
folderPath: "documents" (optional)
```

#### Download File
```http
GET /api/files/{id}/download
```

#### Preview File
```http
GET /api/files/{id}/preview
```

#### Delete File
```http
DELETE /api/files/{id}
```

### File Sharing

#### Chia Sẻ File
```http
POST /api/shares
Content-Type: application/json

{
  "fileId": 1,
  "sharedWithUsername": "user2@example.com",
  "permission": "view"
}
```

#### Xem File Được Chia Sẻ
```http
GET /api/shares/shared-with-me
```

#### Hủy Chia Sẻ
```http
DELETE /api/shares/{shareId}
```

---

## 🔒 Bảo Mật

- Mật khẩu được hash với BCrypt
- Session-based authentication
- CSRF protection cho tất cả POST requests
- Path traversal protection
- File size validation (max 50MB)
- Ownership verification cho mọi thao tác
- Shared access validation

---

## 🧪 Testing

### Chạy Tests

```bash
mvn test
```

### Test Coverage

```bash
mvn clean test jacoco:report
```

Report sẽ được tạo tại: `target/site/jacoco/index.html`

---

## 📊 Database Schema

### Users
- id (PK)
- username (unique)
- password (hashed)
- full_name
- created_at

### Files
- id (PK)
- file_name
- original_file_name
- file_path
- cloudinary_url
- cloudinary_public_id
- folder_path
- content_type
- file_size
- is_deleted
- deleted_at
- owner_id (FK → users)
- uploaded_at

### File Shares
- id (PK)
- file_id (FK → files)
- owner_id (FK → users)
- shared_with_id (FK → users)
- permission (view/edit)
- shared_at

### Folders
- id (PK)
- name
- path
- parent_id (FK → folders)
- owner_id (FK → users)
- created_at

### Passkey Credentials
- id (PK)
- credential_id
- public_key
- user_handle
- signature_count
- user_id (FK → users)
- created_at

---

## 🤝 Đóng Góp

Contributions are welcome! Please feel free to submit a Pull Request.

### Quy Trình
1. Fork repository
2. Tạo branch mới (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Mở Pull Request

---

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Tác Giả

**haivoDA22TTD**
- GitHub: [@your-username](https://github.com/haivoDA22TTD)
- Email: 110122068@st.tvu.edu.vn
---

## 🙏 Acknowledgments

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Cloudinary](https://cloudinary.com)
- [Yubico WebAuthn](https://github.com/Yubico/java-webauthn-server)
- [Render](https://render.com)
- [Aiven](https://aiven.io)

---

## 📞 Hỗ Trợ

Nếu bạn gặp vấn đề, vui lòng:
1. Kiểm tra [Issues](https://github.com/haivoDA22TTD/cloud-storage/issues)
2. Tạo issue mới nếu chưa có
3. Hoặc liên hệ qua email

---

**⭐ Nếu project hữu ích, hãy cho một star nhé!**
