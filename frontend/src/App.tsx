import { useCallback, useEffect, useMemo, useState } from "react";
import {
  clearEvents,
  downloadExcel,
  downloadPdf,
  getEvents,
  getHealth,
  pickFolder,
  getReport,
  getWatchStatus,
  startWatch,
  stopWatch,
} from "./api/client";
import type { FileEvent, HealthResponse, ReportSummary, RiskLevel, WatchStatus } from "./api/types";
import { usePolling } from "./hooks/usePolling";
import { AnalyticsCards } from "./components/analytics/AnalyticsCards";
import { RiskPieChart } from "./components/analytics/RiskPieChart";
import { EventTimelineChart } from "./components/analytics/EventTimelineChart";
import { PatternPanel } from "./components/analytics/PatternPanel";
import { HoneypotPanel } from "./components/analytics/HoneypotPanel";
import { useTheme } from "./theme/ThemeProvider";
import "./App.css";

type TabKey = "control" | "events" | "reports";

const TAB_LABELS: Record<TabKey, string> = {
  control: "Control Panel",
  events: "Live Events",
  reports: "Reports",
};

function App() {
  const [activeTab, setActiveTab] = useState<TabKey>("control");
  const [directoryInput, setDirectoryInput] = useState("D:\\Temp\\watchme");
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [watchStatus, setWatchStatus] = useState<WatchStatus | null>(null);
  const [events, setEvents] = useState<FileEvent[]>([]);
  const [report, setReport] = useState<ReportSummary | null>(null);
  const [highRiskOnly, setHighRiskOnly] = useState(false);
  const [honeypotOnly, setHoneypotOnly] = useState(false);
  const [search, setSearch] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isStarting, setIsStarting] = useState(false);
  const [isStopping, setIsStopping] = useState(false);
  const [isLoadingReport, setIsLoadingReport] = useState(false);
  const [isPickingFolder, setIsPickingFolder] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const { theme, toggleTheme } = useTheme();

  useEffect(() => {
    if (!toastMessage) {
      return;
    }

    const timeoutId = window.setTimeout(() => {
      setToastMessage(null);
    }, 3000);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [toastMessage]);

  const captureError = useCallback((error: unknown) => {
    const message = error instanceof Error ? error.message : "Unexpected API error";
    setErrorMessage(message);
  }, []);

  const refreshHealth = useCallback(async () => {
    try {
      const data = await getHealth();
      setHealth(data);
    } catch (error) {
      captureError(error);
    }
  }, [captureError]);

  const refreshWatchStatus = useCallback(async () => {
    try {
      const data = await getWatchStatus();
      setWatchStatus(data);
    } catch (error) {
      captureError(error);
    }
  }, [captureError]);

  const refreshEvents = useCallback(async () => {
    try {
      const data = await getEvents();
      const sorted = [...data].sort(
        (left, right) => new Date(right.timestamp).getTime() - new Date(left.timestamp).getTime(),
      );
      setEvents(sorted);
    } catch (error) {
      captureError(error);
    }
  }, [captureError]);

  usePolling(refreshHealth, 10000, true);
  usePolling(refreshWatchStatus, 3000, true);
  usePolling(refreshEvents, 1000, activeTab === "events");

  const filteredEvents = useMemo(() => {
    const normalizedSearch = search.trim().toLowerCase();

    return events.filter((event) => {
      if (highRiskOnly && event.riskLevel !== "HIGH") {
        return false;
      }
      if (honeypotOnly && !event.honeypotTriggered) {
        return false;
      }
      if (normalizedSearch.length > 0 && !event.path.toLowerCase().includes(normalizedSearch)) {
        return false;
      }
      return true;
    });
  }, [events, highRiskOnly, honeypotOnly, search]);

  const handleStartWatch = async () => {
    const trimmed = directoryInput.trim();
    if (!trimmed) {
      setErrorMessage("Directory path is required.");
      return;
    }

    try {
      setIsStarting(true);
      setErrorMessage(null);
      await startWatch(trimmed);
      await refreshWatchStatus();
    } catch (error) {
      captureError(error);
    } finally {
      setIsStarting(false);
    }
  };

  const handleClearEvents = async () => {
    if (!window.confirm("Are you sure you want to clear all events? This cannot be undone.")) {
      return;
    }
    try {
      setErrorMessage(null);
      await clearEvents();
      setEvents([]);
      setToastMessage("Events cleared");
    } catch (error) {
      captureError(error);
    }
  };

  const handleStopWatch = async () => {
    try {
      setIsStopping(true);
      setErrorMessage(null);
      await stopWatch();
      setToastMessage("Watcher stopped successfully");
      await refreshWatchStatus();
    } catch (error) {
      captureError(error);
    } finally {
      setIsStopping(false);
    }
  };

  const handleRefreshReport = async () => {
    try {
      setIsLoadingReport(true);
      setErrorMessage(null);
      const data = await getReport();
      setReport(data);
    } catch (error) {
      captureError(error);
    } finally {
      setIsLoadingReport(false);
    }
  };

  const handleDownload = async (kind: "pdf" | "excel") => {
    try {
      setErrorMessage(null);
      if (kind === "pdf") {
        await downloadPdf();
      } else {
        await downloadExcel();
      }
    } catch (error) {
      captureError(error);
    }
  };

  const handlePickFolder = async () => {
    try {
      setIsPickingFolder(true);
      setErrorMessage(null);
      const data = await pickFolder();
      if (data?.path) {
        setDirectoryInput(data.path);
      }
    } catch (error) {
      captureError(error);
    } finally {
      setIsPickingFolder(false);
    }
  };

  return (
    <div className="dashboard">
      <header className="dashboard-header">
        <div>
          <h1>File Behavior Monitor Dashboard</h1>
          <p>Monitor watcher health, inspect file events, and export reports.</p>
        </div>
        <button
          type="button"
          className="secondary theme-toggle"
          onClick={toggleTheme}
          aria-label={`Switch to ${theme === "light" ? "dark" : "light"} mode`}
        >
          {theme === "light" ? "🌙 Dark mode" : "☀️ Light mode"}
        </button>
      </header>

      {errorMessage ? (
        <div className="alert" role="alert">
          <span>{errorMessage}</span>
          <button type="button" onClick={() => setErrorMessage(null)}>
            Dismiss
          </button>
        </div>
      ) : null}

      {toastMessage ? (
        <div className="toast" role="status" aria-live="polite">
          {toastMessage}
        </div>
      ) : null}

      <nav className="tabs" aria-label="Dashboard sections">
        {(Object.keys(TAB_LABELS) as TabKey[]).map((tab) => (
          <button
            key={tab}
            type="button"
            className={`tab ${activeTab === tab ? "active" : ""}`}
            onClick={() => setActiveTab(tab)}
          >
            {TAB_LABELS[tab]}
          </button>
        ))}
      </nav>

      <main>
        {activeTab === "control" ? (
          <section className="panel">
            <h2>Control Panel</h2>
            <div className="grid two-cols">
              <label className="field" htmlFor="directory">
                <span>Directory path</span>
                <div className="directory-row">
                  <input
                    id="directory"
                    className="directory-input"
                    type="text"
                    value={directoryInput}
                    onChange={(event) => setDirectoryInput(event.target.value)}
                    placeholder="D:\\Temp\\watchme"
                    disabled={watchStatus?.running ?? false}
                  />
                  <button
                    type="button"
                    onClick={handlePickFolder}
                    className="btn-outline browse-button"
                    disabled={(watchStatus?.running ?? false) || isPickingFolder}
                  >
                    {isPickingFolder ? "Opening..." : "Browse"}
                  </button>
                </div>
              </label>
              <div className="actions-row">
                {watchStatus?.running ? (
                  <button type="button" onClick={handleStopWatch} disabled={isStopping} className="btn-danger">
                    {isStopping ? "Stopping..." : "Stop Watch"}
                  </button>
                ) : (
                  <button type="button" onClick={handleStartWatch} disabled={isStarting} className="btn-primary">
                    {isStarting ? "Starting..." : "Start Watch"}
                  </button>
                )}
                <button
                  type="button"
                  onClick={handleClearEvents}
                  className="btn-outline"
                  disabled={watchStatus?.running ?? false}
                >
                  Clear events
                </button>
              </div>
            </div>

            <div className="status-grid">
              <StatusCard
                label="Watcher"
                value={watchStatus?.running ? "Running" : "Stopped"}
                detail={watchStatus?.directory ? `Directory: ${watchStatus.directory}` : "Directory not set"}
              />
              <StatusCard
                label="Health"
                value={health?.status ?? "Unknown"}
                detail={health?.timestamp ? `Last update: ${formatDate(health.timestamp)}` : "Waiting for heartbeat"}
              />
              <StatusCard
                label="Events Processed"
                value={String(watchStatus?.totalEventsProcessed ?? 0)}
                detail={watchStatus?.startedAt ? `Started: ${formatDate(watchStatus.startedAt)}` : "Not started"}
              />
              <HoneypotPanel />
            </div>
          </section>
        ) : null}

        {activeTab === "events" ? (
          <section className="panel">
            <h2>Live Events</h2>
            <div className="filters">
              <label>
                <input
                  type="checkbox"
                  checked={highRiskOnly}
                  onChange={(event) => setHighRiskOnly(event.target.checked)}
                />
                High Risk only
              </label>
              <label>
                <input
                  type="checkbox"
                  checked={honeypotOnly}
                  onChange={(event) => setHoneypotOnly(event.target.checked)}
                />
                Honeypot only
              </label>
              <input
                type="search"
                value={search}
                onChange={(event) => setSearch(event.target.value)}
                placeholder="Search path"
              />
            </div>

            <AnalyticsCards events={events} />

            <div className="analytics-charts">
              <RiskPieChart events={events} />
              <EventTimelineChart events={events} />
              <PatternPanel events={events} />
            </div>

            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Timestamp</th>
                    <th>EventType</th>
                    <th>File (path)</th>
                    <th>Risk</th>
                    <th>Honeypot</th>
                    <th>Notes</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredEvents.map((event) => (
                    <tr key={`${event.path}-${event.timestamp}-${event.eventType}`} className={event.honeypotTriggered ? "honeypot" : ""}>
                      <td>{formatDate(event.timestamp)}</td>
                      <td>{event.eventType}</td>
                      <td className="path-cell">{event.path}</td>
                      <td>
                        <span className={`badge ${badgeClassForRisk(event.riskLevel)}`}>
                          {event.riskLevel} ({event.riskScore})
                        </span>
                      </td>
                      <td>{event.honeypotTriggered ? "Yes" : "No"}</td>
                      <td>
                        <div className="chips">
                          {(event.notes ?? []).map((note) => (
                            <span className="chip" key={note}>
                              {note}
                            </span>
                          ))}
                        </div>
                      </td>
                    </tr>
                  ))}
                  {filteredEvents.length === 0 ? (
                    <tr>
                      <td colSpan={6} className="empty">
                        No events match current filters.
                      </td>
                    </tr>
                  ) : null}
                </tbody>
              </table>
            </div>
          </section>
        ) : null}

        {activeTab === "reports" ? (
          <section className="panel">
            <h2>Reports</h2>
            <div className="actions-row">
              <button type="button" onClick={handleRefreshReport} disabled={isLoadingReport}>
                {isLoadingReport ? "Loading..." : report ? "Refresh report" : "Load report"}
              </button>
              <button type="button" className="secondary" onClick={() => void handleDownload("pdf")}>
                Download PDF
              </button>
              <button type="button" className="secondary" onClick={() => void handleDownload("excel")}>
                Download Excel
              </button>
            </div>

            {report ? (
              <div className="report-content">
                <div className="summary-cards">
                  <SummaryCard label="Total Events" value={report.totalEvents} />
                  <SummaryCard label="Honeypot Triggers" value={report.honeypotTriggers} />
                  <SummaryCard label="Low Risk" value={report.lowRiskCount} />
                  <SummaryCard label="Medium Risk" value={report.mediumRiskCount} />
                  <SummaryCard label="High Risk" value={report.highRiskCount} />
                  <SummaryCard label="Detected Patterns" value={report.detectedPatterns.length} />
                </div>
                <div className="report-meta">
                  <p>
                    <strong>Directory:</strong> {report.directory ?? "N/A"}
                  </p>
                  <p>
                    <strong>Generated:</strong> {formatDate(report.generatedAt)}
                  </p>
                  <p>
                    <strong>Monitoring started:</strong>{" "}
                    {report.monitoringStartedAt ? formatDate(report.monitoringStartedAt) : "N/A"}
                  </p>
                  <p>
                    <strong>Detected patterns:</strong>{" "}
                    {report.detectedPatterns.length > 0 ? report.detectedPatterns.join(", ") : "None"}
                  </p>
                </div>
              </div>
            ) : (
              <p className="empty-state">Load report to view summary metrics.</p>
            )}
          </section>
        ) : null}
      </main>
    </div>
  );
}

function StatusCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <article className="status-card">
      <h3>{label}</h3>
      <strong>{value}</strong>
      <p>{detail}</p>
    </article>
  );
}

function SummaryCard({ label, value }: { label: string; value: number }) {
  return (
    <article className="summary-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function formatDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return date.toLocaleString();
}

function badgeClassForRisk(riskLevel: RiskLevel): string {
  if (riskLevel === "HIGH") {
    return "high";
  }
  if (riskLevel === "MEDIUM") {
    return "medium";
  }
  return "low";
}

export default App;
