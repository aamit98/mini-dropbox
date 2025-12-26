# Mini Dropbox

A full-stack cloud storage application with a modern web interface, REST API, and TFTP protocol support.

## Features

- **Web UI**: Modern React/TypeScript interface for file management
- **REST API**: Spring Boot backend with JWT authentication
- **File Storage**: User-based file organization with versioning
- **File Sharing**: Generate shareable links with expiration and download limits
- **Version Control**: Track file versions with rollback capability
- **TFTP Server/Client**: Traditional TFTP protocol implementation
- **Admin Panel**: User management and system monitoring

## Tech Stack

- **Backend**: Java 17, Spring Boot 3.3.2, H2 Database
- **Frontend**: React 18, TypeScript, Vite
- **Security**: JWT authentication, BCrypt password hashing
- **Build Tools**: Maven, npm

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

### Default Admin Account

The application automatically creates an admin account on first run:
- Username: `admin`
- Password: `admin` (change this in production!)

## Project Structure

```
├── rest-server/      # Spring Boot REST API
├── ui/              # React frontend
├── server/          # TFTP server implementation
└── client/          # TFTP client implementation
```

## API Endpoints

- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get JWT token
- `GET /api/v2/files` - List user files
- `POST /api/v2/files` - Upload file
- `GET /api/v2/files/{filename}` - Download file
- `DELETE /api/v2/files/{filename}` - Delete file
- `POST /api/v2/files/{filename}/share` - Create share link
- `GET /api/v2/files/{filename}/versions` - List file versions

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

## License

MIT

