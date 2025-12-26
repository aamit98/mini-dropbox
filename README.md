# 📦 Mini Dropbox

 full-stack cloud storage application featuring a modern React UI, secure REST API, file versioning, and shareable links. Built with enterprise security best practices and comprehensive input validation.


## ✨ Key Features

### Core Functionality
- 🎨 **Modern Web UI**: Responsive React/TypeScript interface with real-time updates
- 🔐 **Secure Authentication**: JWT-based auth with BCrypt password hashing and session management
- 📁 **File Management**: Upload, download, delete, and organize files with user-based isolation
- 🔄 **Version Control**: Automatic file versioning with rollback capability
- 🔗 **Secure File Sharing**: Generate time-limited, download-restricted shareable links
- 👁️ **File Preview**: Image thumbnails and preview support
- 🗑️ **Soft Delete**: Trash/restore functionality for deleted files
- 👨‍💼 **Admin Panel**: User management and system monitoring dashboard

### Security Features (Production-Ready)
- ✅ **Cryptographically secure** share link generation (SecureRandom)
- ✅ **Path traversal protection** with comprehensive validation
- ✅ **Input validation** on all endpoints (usernames, passwords, parameters)
- ✅ **Proper CORS configuration** (no wildcard origins)
- ✅ **Secure JWT secret** with environment variable support
- ✅ **SLF4J logging** throughout (no debug code in production)
- ✅ **RFC 5987 compliant** filename encoding in HTTP headers

## 🛠️ Tech Stack

### Backend
- **Java 17** - Modern Java with records, pattern matching
- **Spring Boot 3.3.2** - Enterprise-grade framework
- **Spring Security** - JWT authentication & authorization
- **Spring Data JPA** - ORM with Hibernate
- **H2 Database** - Embedded SQL database
- **SLF4J/Logback** - Professional logging framework

### Frontend
- **React 18** - Modern hooks-based architecture
- **TypeScript** - Type-safe development
- **Vite** - Fast build tool and dev server
- **CSS3** - Responsive, modern styling

### DevOps & Tools
- **Maven** - Dependency management and build
- **Git** - Version control
- **Docker** - Containerization support (.devcontainer)

## 🔒 Security Highlights

This project demonstrates **security-first development**:

1. **Comprehensive Security Audit**: Identified and fixed 7+ critical vulnerabilities
2. **Input Validation**: All user inputs validated (see `SECURITY_IMPROVEMENTS.md`)
3. **Secure Defaults**: H2 console disabled, proper CORS, secure random generators
4. **Production Best Practices**: Environment-based configuration, proper logging

See [`SECURITY_IMPROVEMENTS.md`](./SECURITY_IMPROVEMENTS.md) for detailed security improvements.

## Quick Start

### Prerequisites

- Java 17+ (JDK)
- Maven 3.6+
- Node.js 16+ and npm

### Starting the Application

**Important**: There are two different servers:
- **`rest-server/`** - Spring Boot REST API (for the web UI) - Use `mvn spring-boot:run`
- **`server/`** - TFTP Server (traditional protocol) - Use `mvn exec:java`

1. **Start REST Server** (port 8080) - **This is what you need for the web UI**:
   ```powershell
   cd rest-server
   .\run-server.ps1
   ```

2. **Start UI** (port 5173):
   ```powershell
   cd ui
   npm install
   .\run-ui.ps1
   ```

3. **Or start everything at once**:
   ```powershell
   .\start-all.ps1
   ```

### Access the Application

- Web UI: http://localhost:5173
- REST API: http://localhost:8080/api
- H2 Console: http://localhost:8080/h2-console (credentials in application.properties)

### Login Instructions

**Default Admin Account** (automatically created on first run):
- **Username**: `admin`
- **Password**: `admin`

**Or create your own account:**
1. Enter any username and password
2. Click "Register" to create a new account
3. Then click "Sign In" to login

> **Note**: The admin account is automatically created when the REST server starts for the first time. After that, you can use it or create new accounts.

## Project Structure

```
├── rest-server/      # Spring Boot REST API
├── ui/              # React frontend
├── server/          # TFTP server implementation
└── client/          # TFTP client implementation
```

## 📡 API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user (with validation)
- `POST /api/auth/login` - Login and get JWT token
- `GET /api/auth/me` - Get current user info

### File Management (v2 API)
- `GET /api/v2/files` - List user's files
- `POST /api/v2/files/upload` - Upload file (multipart/form-data)
- `GET /api/v2/files/{filename}` - Download file
- `DELETE /api/v2/files/{filename}` - Soft delete file
- `POST /api/v2/files/{filename}/undelete` - Restore deleted file

### Versioning
- `GET /api/v2/files/{filename}/versions` - List all versions
- `POST /api/v2/files/{filename}/restore?version={n}` - Restore specific version

### Sharing
- `POST /api/v2/files/{filename}/share` - Create shareable link
  - Body: `{ "hours": 24, "maxDownloads": 10 }`
  - Returns: `{ "code": "abc123", "url": "/d/abc123" }`
- `GET /d/{code}` - Download via share link (public)

## Development

### Building

```powershell
# Build all Maven modules
mvn clean install

# Build UI
cd ui
npm install
npm run build
```

### Running TFTP Server/Client

```powershell
# Start TFTP server (port 7777)
cd server
.\run-server.ps1

# Run TFTP client
cd client\tftp-client
.\run-client.ps1
```

## Configuration

Storage directory can be configured via environment variable:
```powershell
$env:STORAGE_DIR = "C:\path\to\storage"
```

Or edit `rest-server/src/main/resources/application.properties`

## 🎯 What This Project Demonstrates


## 📚 Learning Outcomes

From building this project, I learned:

- How to design RESTful APIs with proper HTTP semantics
- Spring Security architecture and JWT authentication flow
- React state management and component architecture
- Secure file handling and path traversal prevention
- Professional logging practices and error handling
- Database relationships and JPA entity modeling
- Input validation strategies at multiple layers

## 🚀 Future Enhancements

Potential improvements for production deployment:

- [ ] Migrate to PostgreSQL for production database
- [ ] Add rate limiting on authentication endpoints
- [ ] Implement file encryption at rest
- [ ] Add user storage quotas
- [ ] WebSocket support for real-time file sync
- [ ] File search and tagging system
- [ ] Email notifications for shared links
- [ ] OAuth integration (Google, GitHub)
- [ ] Docker Compose for easy deployment
- [ ] CI/CD pipeline with GitHub Actions


See [`SECURITY_IMPROVEMENTS.md`](./SECURITY_IMPROVEMENTS.md) for a detailed breakdown of the security audit and improvements.

## 📄 License

MIT

---

**Author**: Amit Asher
**Contact**: asherproc@gmail.com
**LinkedIn**: https://www.linkedin.com/in/amit-asher-555757382/
**GitHub**: https://github.com/aamit98

