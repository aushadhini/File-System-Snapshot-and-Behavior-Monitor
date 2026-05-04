# File Behavior Monitoring & Threat Detection System

## 1. Project Overview

### Purpose of the System
The **File Behavior Monitoring & Threat Detection System** is designed to continuously observe file activity in a selected directory, detect suspicious behavior patterns, and provide near-real-time visibility through a modern dashboard. The platform combines filesystem event monitoring, deception-based detection (honeypots), behavior analytics, and risk scoring into a single integrated workflow.

### Problem Statement
Traditional endpoint monitoring in many environments either:
- focuses only on raw file change logs,
- lacks contextual risk interpretation,
- or requires heavy SIEM integration before becoming useful.

As a result, rapid destructive or malicious file operations (e.g., mass modification, delete spikes, suspicious executable drops) can go unnoticed or be triaged too late.

### Why File Monitoring Is Important
File monitoring is security-critical because file operations are often early signals of:
- ransomware encryption waves,
- destructive insider actions,
- malware dropper behavior,
- and privilege abuse against sensitive documents.

Detecting these operations at event time enables faster containment, better forensic evidence, and stronger operational resilience.

### Objective of the Project
The project objective is to deliver a practical, extensible, and user-friendly system that:
1. Watches file activity from the OS layer.
2. Detects suspicious behavior patterns.
3. Applies deterministic risk scoring.
4. Visualizes threats via an analytics dashboard.
5. Exports evidence-rich reports in JSON/PDF/Excel formats.

---

## 2. System Goals & Requirements

### Functional Requirements
1. Start/stop filesystem monitoring for a selected directory.
2. Capture file lifecycle events: create, modify, delete.
3. Auto-deploy honeypot trap artifacts in a hidden trap folder.
4. Flag interactions with honeypot files as high-risk signals.
5. Analyze event streams for suspicious patterns.
6. Calculate risk score and risk level for each event.
7. Deduplicate noisy duplicate modifications within a configured window.
8. Expose monitoring and analytics APIs via REST.
9. Render live event and risk analytics in a React dashboard.
10. Export report data as JSON, PDF, and Excel.
11. Persist report snapshots on server storage.
12. Support native folder selection from backend OS integration.

### Non-Functional Requirements
- **Performance:** Low-latency ingestion for active directories.
- **Reliability:** Safe watcher lifecycle management and cleanup.
- **Usability:** Dashboard with filters, tabs, and visual threat indicators.
- **Maintainability:** Modular package design with clear service boundaries.
- **Portability:** Java-based backend with cross-platform WatchService support.
- **Observability:** Status endpoints, monitoring state, and report summaries.

### Innovation Goals
- Introduce **deception-based security** in a lightweight local monitor.
- Correlate behavioral patterns with honeypot triggers for stronger confidence.
- Provide analyst-ready reporting without external BI tooling.

---

## 3. High-Level Architecture

The system follows a layered architecture where the React frontend controls and observes a Spring Boot backend, and the backend orchestrates WatchService-based file event capture and post-processing.

### Core Layers
- **Frontend (React):** User interaction and real-time analytics view.
- **Backend (Spring Boot):** API gateway, orchestration, and processing services.
- **Filesystem Monitoring Layer:** OS WatchService ingestion.
- **Event Processing Pipeline:** Honeypot check → behavior analysis → risk scoring.

### ASCII Architecture Diagram

```text
User
  |
  v
React UI
  |
  v
REST API (Spring Boot)
  |
  v
Event Pipeline
  |
  v
WatchService
  |
  v
File System
```

### Request/Data Flow
1. User selects a folder and starts monitoring from UI.
2. Frontend calls `/watch/start`.
3. Backend starts WatchService and deploys honeypots.
4. WatchService emits raw file events.
5. Event pipeline enriches each event (honeypot/behavior/risk).
6. Events are stored in memory and exposed via `/events`.
7. Frontend polls APIs and updates analytics components.
8. Reports are generated on demand from current in-memory state.

---

## 4. Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| Frontend | React + TypeScript | UI dashboard |
| Backend | Spring Boot | REST API |
| Language | Java 21 | Core processing |
| Build Tool | Maven | Dependency management |
| Charts | Recharts | Analytics visualization |
| PDF Export | Apache PDFBox | Reporting |
| Excel Export | Apache POI | Spreadsheet reports |

### Why These Technologies Were Selected
- **React + TypeScript:** Strong component model, safer API contracts, and responsive UX for data-heavy dashboards.
- **Spring Boot:** Rapid REST service development, dependency injection, and robust production ecosystem.
- **Java 21:** Modern LTS runtime with strong concurrency and stable filesystem APIs.
- **Maven:** Widely adopted Java build lifecycle and dependency resolution.
- **Recharts:** Fast integration for risk distributions and timeline/pattern visualizations.
- **Apache PDFBox:** Reliable server-side PDF generation for audit-ready documents.
- **Apache POI:** Standard library for enterprise Excel export and structured tabular reports.

---

## 5. System Architecture Design

### Module Responsibilities

#### 1) Watcher Module
- Starts/stops monitoring sessions.
- Registers watched directories with WatchService.
- Converts OS watch signals into normalized file events.

#### 2) Event Pipeline
- Receives normalized events.
- Performs deduplication for repeated modifications.
- Coordinates downstream enrichment stages.

#### 3) Honeypot System
- Deploys trap folder (`.sys_trap`) and decoy files.
- Maintains registry of deployed honeypot paths.
- Flags access/changes to decoys.

#### 4) Behavior Analyzer
- Maintains short sliding event window.
- Detects high-velocity and suspicious extension patterns.
- Annotates events with pattern notes.

#### 5) Risk Engine
- Assigns score based on event type and detected patterns.
- Elevates severity for honeypot interaction and critical correlations.
- Outputs LOW/MEDIUM/HIGH risk levels.

#### 6) Reporting Module
- Aggregates event/risk/pattern metrics.
- Generates JSON summary payload.
- Exports PDF and Excel artifacts.

#### 7) Analytics Dashboard
- Provides control panel + live event views + reports tab.
- Offers filtering by risk/honeypot/path search.
- Visualizes patterns, risk distribution, and timelines.

---

## 6. Backend Architecture (Detailed)

### Package Structure

```text
com.invdb.monitor
 ├── api
 ├── watcher
 ├── honeypot
 ├── behavior
 ├── risk
 ├── report
 ├── system
 └── config
```

### Modular Monolith Approach
The backend uses a **modular monolith** style:
- one deployable unit,
- multiple cohesive internal modules,
- strict responsibility boundaries through service interfaces.

This approach balances simplicity (single deployment/runtime) with maintainability (clear internal domains), making it suitable for academic and production prototypes.

### Separation of Concerns
- `api`: External HTTP contracts and request validation.
- `watcher`: Filesystem lifecycle and event capture.
- `honeypot`: Deception artifacts and trigger recognition.
- `behavior`: Pattern analysis over event windows.
- `risk`: Risk calculation and risk level mapping.
- `report`: Summary generation and export formats.
- `system`: Native/OS integrations (folder picker).
- `config`: App properties, CORS, and configuration binding.

---

## 7. Frontend Architecture

### Component Structure
The frontend is organized around feature components:
- Dashboard shell (`App.tsx`) with tab routing.
- Analytics widgets (`AnalyticsCards`, `RiskPieChart`, `EventTimelineChart`, `PatternPanel`, `HoneypotPanel`).
- API client abstraction and typed DTOs.

### Dashboard Layout
- **Control Panel Tab:** Start/stop monitor, browse folder, status controls.
- **Live Events Tab:** Filtered event feed and analytics cards/charts.
- **Reports Tab:** Generate summary and export PDF/Excel.

### Polling & Near-Real-Time Updates
The dashboard uses interval polling:
- health: every 10s,
- watch status: every 3s,
- events: every 1s (when events tab active).

This keeps UI state synchronized with backend runtime behavior without WebSocket complexity.

### Theme System (Light/Dark)
A context-based theme provider:
- stores theme in `localStorage`,
- toggles via header control,
- applies theme through a root `data-theme` attribute.

---

## 8. Event Processing Flow

### Lifecycle

```text
Start Watch
  -> WatchService detects change
  -> FileEvent created
  -> Honeypot detection
  -> Behavior analysis
  -> Risk scoring
  -> Stored in memory
  -> Displayed on dashboard
  -> Included in reports
```

### Step-by-Step Explanation
1. User starts monitoring through REST endpoint.
2. Watcher registers directory keys with WatchService.
3. OS emits create/modify/delete event.
4. Event is mapped to `FileEvent` with timestamp/path/type.
5. Pipeline checks whether target path is a honeypot artifact.
6. Dedup logic drops repeated modify noise in short windows.
7. Behavior analyzer adds pattern notes using sliding-window correlation.
8. Risk engine computes score and level.
9. Enriched event is stored in in-memory deque.
10. Frontend polls `/events` and renders live indicators.
11. Reporting module reads same event store for exports.

---

## 9. Honeypot Auto-Deployment (Innovation Section)

### What Honeypots Are
Honeypots are decoy resources intentionally placed to detect unauthorized or suspicious interaction. In this system, they are decoy files that should not be touched during normal workflows.

### Why Deception-Based Detection Is Used
Deception signals are high-value because legitimate activity rarely targets decoys. A trigger therefore raises confidence in suspicious intent and accelerates triage.

### Auto-Deployed Trap Folder (`.sys_trap`)
At watcher startup:
- system resolves trap folder name (default `.sys_trap`),
- creates trap directory,
- optionally hides it on Windows,
- deploys configured decoy files.

### Detection Mechanism
Every event path is checked against registered honeypot files/paths. Positive matches immediately raise event criticality and affect risk scoring.

### Security Value
- Reduces false confidence from benign noise.
- Improves early ransomware/intrusion visibility.
- Provides clear high-priority alerts for incident response.

> **Innovation Highlight:** Combining honeypot triggers with behavior correlation to produce `CRITICAL_INTRUSION_PATTERN` provides stronger contextual alerting than single-signal monitoring.

---

## 10. Behavior Pattern Detection

The analyzer identifies and annotates suspicious trends:

| Pattern | Meaning |
|---|---|
| `MASS_CHANGE_SUSPECTED` | Event volume exceeds threshold in short window |
| `RAPID_DELETE_SPIKE` | Delete operations spike above threshold |
| `SUSPICIOUS_EXTENSION` | New/modified file has suspicious extension (e.g., exe, dll, ps1) |
| `CRITICAL_INTRUSION_PATTERN` | Honeypot trigger + mass-change correlation |

### Correlation and Risk Amplification
- Pattern notes are additive and support multi-signal context.
- Correlated notes are consumed by risk engine to increase score.
- `CRITICAL_INTRUSION_PATTERN` forces max-severity behavior classification path.

---

## 11. Reporting & Export System

### JSON Reporting
`/report` returns a structured summary containing:
- monitoring metadata,
- risk distribution,
- honeypot trigger counts,
- detected patterns,
- full event list.

### PDF Generation
`/report/pdf` creates downloadable human-readable report documents using PDFBox.

### Excel Export
`/report/excel` creates spreadsheet output using Apache POI for analysis and archival workflows.

### Report Persistence
`/report/snapshot` persists generated PDF reports to `/reports` for later retrieval and audit continuity.

---

## 12. Real-Time Analytics Dashboard

### Features
- **Live Monitoring:** Frequent polling and continuously refreshed event feed.
- **Charts:** Risk distribution and event trend visualization.
- **Pattern Visualization:** Ranked display of detected behavioral indicators.
- **Threat Indicators:** Honeypot trigger counters and high-risk filtering.
- **Light/Dark Mode:** Persistent user theme preference.

### Operational Benefit
The dashboard converts low-level filesystem telemetry into quickly understandable threat context for operators and reviewers.

---

## 13. Native Folder Picker Integration

### Why Browser Folder Access Is Limited
Standard browser sandboxing limits direct system directory access for security reasons, especially for persistent backend monitoring use cases.

### Backend Native OS Picker Solution
The system delegates folder selection to backend-native code exposed via `/system/pick-folder`.

### Swing `JFileChooser` Integration
- Uses Java Swing `JFileChooser` in directory-only mode.
- Returns selected absolute path to frontend.
- Gracefully handles headless/permission failures by returning no content.

This approach bridges secure browser UX and native desktop directory selection.

---

## 14. Setup & Installation Guide

### Prerequisites
- **Java:** 21+
- **Node.js:** 20+ (recommended LTS)
- **npm:** bundled with Node.js
- **Maven:** via local Maven or project wrapper

### Backend Setup
```bash
cd backend
mvn clean install
mvn spring-boot:run
```

### Frontend Setup
```bash
cd frontend
npm install
npm run dev
```

### Default Runtime Endpoints
- Backend API: `http://localhost:8080`
- Frontend (Vite dev): typically `http://localhost:5173`

---

## 15. How to Use the System

1. **Select folder**
   - Enter path manually or click **Browse** for native folder picker.
2. **Start monitoring**
   - Click start; backend initializes watcher and honeypots.
3. **View live events**
   - Open **Live Events** tab to inspect incoming activity.
4. **Analyze threats**
   - Apply high-risk/honeypot/path filters and inspect pattern cards.
5. **Generate reports**
   - Open **Reports** tab, refresh summary, download PDF/Excel.
6. **Stop monitoring**
   - Stop watcher to end session and reset runtime collection behavior.

---

## 16. What Was Accomplished

- Implemented end-to-end filesystem event capture.
- Built real-time event pipeline with deduplication.
- Added automatic honeypot deployment and detection.
- Implemented behavior pattern analysis and note tagging.
- Added deterministic risk engine with level mapping.
- Built dashboard with analytics cards, charts, and filters.
- Added native folder picker integration for usability.
- Implemented report generation in JSON, PDF, and Excel.
- Enabled server-side report snapshot persistence.
- Structured backend as modular monolith with test coverage.

---

## 17. Challenges & Solutions

| Challenge | Solution |
|---|---|
| WatchService lifecycle complexity | Centralized start/stop with synchronized lifecycle lock and graceful shutdown logic |
| Headless mode issue (native UI) | Catch `HeadlessException` and return safe no-content response |
| Native picker integration with web UI | Expose backend endpoint that bridges Swing chooser and frontend input |
| Event deduplication noise | Added configurable dedup window keyed by event type + normalized path |

---

## 18. Future Improvements

1. **WebSocket streaming** for push-based event delivery.
2. **Database persistence** for long-term historical analytics.
3. **Machine learning anomaly detection** beyond rule thresholds.
4. **Multi-directory monitoring** with parallel watcher contexts.
5. **Role-based access control** for enterprise deployment.
6. **Alert integrations** (email, webhook, SIEM forwarders).

---

## 19. Conclusion

The File Behavior Monitoring & Threat Detection System demonstrates a practical and extensible approach to host-level security monitoring by combining real-time filesystem observation, deception-based sensing, behavioral analysis, and actionable risk scoring.

From an academic perspective, the project showcases layered architecture, modular backend design, and meaningful security innovation through event correlation. From a GitHub/project perspective, it provides a deployment-ready foundation that can evolve toward enterprise-grade observability and threat response workflows.
