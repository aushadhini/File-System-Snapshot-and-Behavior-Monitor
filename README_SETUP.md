# File Behavior Monitoring System – Setup Guide

## Requirements

Install before running:

- Java JDK 21+
- Node.js 18+
- Maven (latest)
- Windows OS (required for native folder picker)

---

## Backend Setup

1. Open terminal
2. Navigate to backend folder

cd backend

3. Run application

mvn spring-boot:run -Dspring-boot.run.jvmArguments="-Djava.awt.headless=false"

Backend runs at:
http://localhost:8080

---

## Frontend Setup

1. Open new terminal
2. Navigate to frontend

cd frontend

3. Install dependencies

npm install

4. Start frontend

npm run dev

Frontend runs at:
http://localhost:5173

---

## How to Use

1. Open dashboard in browser
2. Click "Browse (Native)"
3. Select folder to monitor
4. Click Start Watch
5. Perform file operations
6. View analytics & reports

---

## Generate Reports

Go to Reports tab and download:

- PDF Report
- Excel Report
