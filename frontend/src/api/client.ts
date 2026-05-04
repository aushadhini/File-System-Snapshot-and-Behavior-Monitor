import type { FileEvent, HealthResponse, HoneypotStatus, ReportSummary, WatchStatus } from "./types";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(init?.headers ?? {}),
    },
    ...init,
  });

  if (!response.ok) {
    let message = `Request failed with status ${response.status}`;
    try {
      const data: unknown = await response.json();
      if (typeof data === "object" && data !== null && "error" in data) {
        const maybeError = (data as { error?: unknown }).error;
        if (typeof maybeError === "string") {
          message = maybeError;
        }
      }
    } catch {
      // ignore parse error and keep generic message
    }
    throw new Error(message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}

export function getHealth() {
  return request<HealthResponse>("/health");
}

export function startWatch(directory: string) {
  return request<{ message: string; directory: string }>("/watch/start", {
    method: "POST",
    body: JSON.stringify({ directory }),
  });
}

export function getWatchStatus() {
  return request<WatchStatus>("/watch/status");
}

export function stopWatch() {
  return request<{ message: string }>("/watch/stop", { method: "POST" });
}

export function getEvents() {
  return request<FileEvent[]>("/events");
}

export function clearEvents() {
  return request<{ cleared: boolean }>("/events", { method: "DELETE" });
}

export function getReport() {
  return request<ReportSummary>("/report");
}

export function getHoneypotStatus() {
  return request<HoneypotStatus>("/honeypot/status");
}

export type PickFolderResponse = {
  path?: string;
};

export function pickFolder() {
  return request<PickFolderResponse | undefined>("/system/pick-folder");
}

async function downloadFile(path: string, filename: string) {
  const response = await fetch(`${API_BASE_URL}${path}`);
  if (!response.ok) {
    throw new Error(`Failed to download ${filename}`);
  }

  const blob = await response.blob();
  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.append(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(url);
}

export function downloadPdf() {
  return downloadFile("/report/pdf", "file-behavior-report.pdf");
}

export function downloadExcel() {
  return downloadFile("/report/excel", "file-behavior-report.xlsx");
}
