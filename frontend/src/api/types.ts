export type RiskLevel = "LOW" | "MEDIUM" | "HIGH";

export interface HealthResponse {
  status: string;
  service: string;
  timestamp: string;
}

export interface WatchStatus {
  running: boolean;
  directory: string | null;
  startedAt: string | null;
  totalEventsProcessed: number;
}

export interface FileEvent {
  path: string;
  timestamp: string;
  eventType: string;
  honeypotTriggered: boolean;
  riskScore: number;
  riskLevel: RiskLevel;
  notes: string[] | null;
}

export interface ReportSummary {
  directory: string | null;
  generatedAt: string;
  monitoringStartedAt: string | null;
  totalEvents: number;
  honeypotTriggers: number;
  lowRiskCount: number;
  mediumRiskCount: number;
  highRiskCount: number;
  detectedPatterns: string[];
  events: FileEvent[];
}

export interface HoneypotStatus {
  enabled: boolean;
  deployOnStart: boolean;
  cleanupOnStop: boolean;
  trapFolderName: string | null;
  watchedDirectory: string | null;
  deployedCount: number;
  deployedPaths: string[];
}
