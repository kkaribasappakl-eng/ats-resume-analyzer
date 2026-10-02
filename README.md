# Enterprise AI ATS Resume Analyzer & Candidate Ranking Platform

An enterprise-grade, AI-powered Applicant Tracking System (ATS) platform built with **Java 21, Spring Boot 3.3, MySQL, and Google Gemini AI API**. It offers recruiters a 15-second candidate summary, dynamic candidate ranker dashboard, multi-strategy resume parser, transparent explainable scoring, STAR project metrics rewrites, soft skills matrix, and an inline AI Recruiter Chat assistant.

---

## 🚀 Key Features

* **Multi-Strategy Resume Parsing:** Extracts structured content and page-level annotation hyperlinks from PDF and DOCX files generated via MS Word, Google Docs, LaTeX, Overleaf, Canva, or modern ATS templates.
* **Weighted Placement Readiness Index:** Transparent, non-cached scoring algorithm calculating candidate readiness across 8 key dimensions:
  - Technical Skills (30%)
  - Projects Quality (25%)
  - ATS Compatibility & Structure (15%)
  - Resume Quality & Grammar (10%)
  - Soft Skills Matrix (10%)
  - Industry Certifications (5%)
  - Internship & Experience (5%)
* **Recruiter 15-Second Summary & Leaderboard:** Recruiter console to batch screen and rank multiple candidates simultaneously, outputting candidate fit badges (`Strong Hire`, `Shortlist`, `Maybe`, `Reject`).
* **Transparent Explainable Scoring Auditor:** Interactive modal details exact calculation formulas, evidence extracted from the resume, deduction reasons, and projected ATS score improvements.
* **Projects STAR Metrics Engine:** Evaluates project statements for Situation, Task, Action, Result, and missing measurable achievements, generating high-impact STAR rewrites.
* **Soft Skills Audit Matrix:** Scans candidate content for evidence across 12 core competencies (Leadership, Communication, Teamwork, Problem Solving, Analytical Thinking, Ownership, Presentation, Adaptability, Learning Ability, Decision Making, Critical Thinking, Time Management).
* **AI Recruiter Chat Assistant:** Floating, context-aware chatbot on report pages that answers questions about candidate skills, experience, or qualifications using only parsed resume data.
* **Version Comparison & Dynamic Roadmap:** Visual side-by-side version comparison with score change tracking and prioritized improvement action cards.

---

## 🛠️ Technology Stack

* **Backend Framework:** Java 21, Spring Boot 3.3.0, Spring Data JPA, Spring Security
* **Database & Persistence:** MySQL 8+, Hibernate ORM, HikariCP Connection Pool
* **Parsing Engine:** Apache PDFBox, Apache Tika, Apache POI
* **AI Integration:** Google Gemini REST API via Java `HttpClient` + Jackson `ObjectMapper` (with local rule-based fallback handlers)
* **Frontend UI:** HTML5, CSS3 Glassmorphism UI, Bootstrap 5.3, Thymeleaf Templates, JavaScript (Async Fetch API)
* **Build Tool:** Apache Maven 3.9+

---

## 📁 Project Structure

```
c:/Users/Nayana k l/OneDrive/Documents/ResumeAnalyzer
├── src/main/java/com/resumeanalyzer/
│   ├── config/             # Security, Auth, & Database Seeders
│   ├── controller/         # Spring MVC & REST Controllers
│   ├── dto/                # Data Transfer Objects
│   ├── model/              # JPA Entities (Resume, Job, User)
│   ├── repository/         # Spring Data JPA Repositories
│   └── service/            # Parser, ATS Scoring, & Gemini AI Assistant Services
├── src/main/resources/
│   ├── static/             # CSS & JavaScript assets
│   ├── templates/          # Thymeleaf HTML Templates
│   └── application.properties # Application Configuration
├── schema.sql              # Database Schema Script
└── pom.xml                 # Maven Project Configuration
```

---

## ⚙️ Prerequisites & Requirements

1. **Java Development Kit (JDK):** JDK 21 or higher.
2. **Database:** MySQL Server 8.0 or higher.
3. **Build Tool:** Apache Maven 3.8+ (or included Maven wrapper `mvnw.cmd`).
4. **Google Gemini API Key (Optional):** Set `GEMINI_API_KEY` as an environment variable to enable live Gemini AI analysis. (Fallback rule-based engine operates automatically if unconfigured).

---

## 🔧 Database Setup & Configuration

1. **Create MySQL Database:**
   ```sql
   CREATE DATABASE resume_analyzer_db;
   ```

2. **Configure Credentials (`src/main/resources/application.properties`):**
   ```properties
   server.port=8080
   spring.datasource.url=jdbc:mysql://localhost:3306/resume_analyzer_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   spring.datasource.username=root
   spring.datasource.password=YOUR_MYSQL_PASSWORD
   ```

---

## 🏃 How to Run the Project

### Using Maven Commands

1. **Build & Package:**
   ```bash
   mvn clean install -DskipTests
   ```

2. **Run Spring Boot Application:**
   ```bash
   mvn spring-boot:run
   ```

3. **Access Application:**
   Open browser at **`http://localhost:8080`**

### Seeded Credentials (Auto-Generated on First Boot)

* **Administrator / Recruiter:** `admin` / `admin123`
* **Applicant User:** `user` / `user123`

---

## 📡 API Endpoints Summary

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/` | Home / Welcome Landing Page |
| `GET` | `/resume/analyze` | Resume Upload & Screening Form |
| `POST` | `/resume/analyze` | Processes uploaded file against target Job Description |
| `GET` | `/resume/report/{id}` | Detailed ATS Analysis Report Page |
| `POST` | `/resume/report/{id}/chat` | REST endpoint for AI Recruiter Chat assistant |
| `GET` | `/admin/dashboard` | Recruiter Candidate Leaderboard & Screening Panel |
| `POST` | `/admin/ranking/upload` | Recruiter Bulk Screening upload endpoint |
