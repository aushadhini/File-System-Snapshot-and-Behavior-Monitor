import { useCallback, useMemo, useState } from "react";
import { getHoneypotStatus } from "../../api/client";
import type { HoneypotStatus } from "../../api/types";
import { usePolling } from "../../hooks/usePolling";

const MAX_VISIBLE_PATHS = 10;

export function HoneypotPanel() {
  const [status, setStatus] = useState<HoneypotStatus | null>(null);
  const [isExpanded, setIsExpanded] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refreshStatus = useCallback(async () => {
    try {
      const data = await getHoneypotStatus();
      setStatus(data);
      setError(null);
    } catch (refreshError) {
      const message = refreshError instanceof Error ? refreshError.message : "Failed to load honeypot status";
      setError(message);
    }
  }, []);

  usePolling(refreshStatus, 5000, true);

  const deployedPaths = status?.deployedPaths ?? [];
  const visiblePaths = useMemo(
    () => (isExpanded ? deployedPaths : deployedPaths.slice(0, MAX_VISIBLE_PATHS)),
    [deployedPaths, isExpanded],
  );

  const hasMorePaths = deployedPaths.length > MAX_VISIBLE_PATHS;
  const trapFolder = status?.trapFolderName?.trim() ? status.trapFolderName : ".sys_trap";

  return (
    <article className="status-card honeypot-panel">
      <h3>Honeypot Deployment</h3>
      <dl className="honeypot-stats">
        <div>
          <dt>Enabled</dt>
          <dd>{status?.enabled ? "Yes" : "No"}</dd>
        </div>
        <div>
          <dt>Trap folder</dt>
          <dd>{trapFolder}</dd>
        </div>
        <div>
          <dt>Deployed files</dt>
          <dd>{status?.deployedCount ?? 0}</dd>
        </div>
      </dl>

      {error ? <p className="honeypot-note">Status unavailable: {error}</p> : null}

      {visiblePaths.length > 0 ? (
        <>
          <ul className="honeypot-paths">
            {visiblePaths.map((path) => (
              <li key={path}>{path}</li>
            ))}
          </ul>

          {hasMorePaths ? (
            <button type="button" className="btn-outline honeypot-toggle" onClick={() => setIsExpanded((prev) => !prev)}>
              {isExpanded ? "Show less" : `Show more (${deployedPaths.length - MAX_VISIBLE_PATHS})`}
            </button>
          ) : null}
        </>
      ) : (
        <p className="honeypot-note">No deployed honeypot files reported.</p>
      )}
    </article>
  );
}
