# 🚀 UptimeWatch — Website Health Monitoring

UptimeWatch is a web-based website health monitoring application that allows users to check website availability, view HTTP response status, measure response time, and review recent monitoring history.

## ✨ Features

- **Website Health Checks** — Check a website using its URL.
- **HTTP Status Monitoring** — View HTTP response codes and UP/DOWN status.
- **Response Time Measurement** — Measure how long a website takes to respond.
- **Monitoring History** — Store and retrieve recent checks from MySQL.
- **Interactive Dashboard** — View monitoring results in a responsive dashboard.

## 🛠️ Tech Stack

| Component | Technology |
|---|---|
| Frontend | React, Vite, CSS |
| Backend | Java 21, Spring Boot |
| Database | MySQL 8 |
| Data Access | Spring Data JPA / Hibernate |
| Version Control | Git and GitHub |

## 🏗️ Architecture

1. The user enters a website URL in the React dashboard.
2. The frontend sends a request to the Spring Boot API.
3. The backend checks the website and measures its response time.
4. The result is saved in the MySQL database.
5. The dashboard retrieves and displays recent monitoring history.

## ⚙️ Local Setup

### Prerequisites

- Java 21
- Node.js and npm
- MySQL 8
- Git

### 1. Clone the repository

```bash
git clone https://github.com/muruganchandhru2024-pixel/UptimeWatch.git
cd UptimeWatch
```

### 2. Configure MySQL

Create the database:

```sql
CREATE DATABASE uptimewatch_db;
```

Configure the database URL and username in `src/main/resources/application.properties`. Supply the database password through the `DB_PASSWORD` environment variable.

Never commit real passwords or other secrets to GitHub.

### 3. Start the backend

In PowerShell, from the project root:

```powershell
$env:DB_PASSWORD = "YOUR_LOCAL_DATABASE_PASSWORD"
.\mvnw.cmd spring-boot:run
```

The backend runs on `http://localhost:8080` by default.

### 4. Start the frontend

Open another terminal:

```powershell
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173` in your browser.

## 🔌 API Endpoints

| Method | Endpoint | Purpose |
|---|---|---|
| GET | `/api/check?url=https://example.com` | Check a website |
| GET | `/api/history` | Retrieve recent monitoring records |

## 🔐 Security Note

This project is currently intended for local development and learning. Before public deployment, implement stronger URL validation, block private and internal network destinations, and add appropriate security controls and request limits.

## 👨‍💻 Author

**Chandhru M.**

Computer Science and Engineering

GitHub: [muruganchandhru2024-pixel](https://github.com/muruganchandhru2024-pixel)

---

*Built as a hands-on project to learn full-stack development, REST APIs, database integration, and cloud deployment concepts.*