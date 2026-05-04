import type { FileEvent } from "../../api/types";

interface PatternPanelProps {
  events: FileEvent[];
}

const SEVERITY_BY_PATTERN: Record<string, string> = {
  CRITICAL_INTRUSION_PATTERN: "critical",
  MASS_CHANGE_SUSPECTED: "mass-change",
  RAPID_DELETE_SPIKE: "delete-spike",
  SUSPICIOUS_EXTENSION: "suspicious-extension",
};

export function PatternPanel({ events }: PatternPanelProps) {
  const patternCounts = events.reduce<Record<string, number>>((counts, event) => {
    for (const note of event.notes ?? []) {
      counts[note] = (counts[note] ?? 0) + 1;
    }
    return counts;
  }, {});

  const patterns = Object.entries(patternCounts)
    .map(([pattern, count]) => ({
      pattern,
      count,
      severityClass: SEVERITY_BY_PATTERN[pattern] ?? "default",
    }))
    .sort((left, right) => right.count - left.count || left.pattern.localeCompare(right.pattern));

  if (patterns.length === 0) {
    return null;
  }

  return (
    <article className="chart-card pattern-panel">
      <h3>Detected Behavior Patterns</h3>
      <ul className="pattern-list">
        {patterns.map((item) => (
          <li key={item.pattern}>
            <span className="pattern-name">{item.pattern}</span>
            <span className={`badge pattern-badge ${item.severityClass}`}>{item.count}</span>
          </li>
        ))}
      </ul>
    </article>
  );
}
