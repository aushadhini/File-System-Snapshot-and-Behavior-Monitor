import { Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis, CartesianGrid } from "recharts";
import type { FileEvent } from "../../api/types";

interface EventTimelineChartProps {
  events: FileEvent[];
}

export function EventTimelineChart({ events }: EventTimelineChartProps) {
  const grouped = events.reduce<Record<string, number>>((accumulator, event) => {
    const date = new Date(event.timestamp);
    if (Number.isNaN(date.getTime())) {
      return accumulator;
    }

    date.setSeconds(0, 0);
    const minuteKey = date.toISOString();
    accumulator[minuteKey] = (accumulator[minuteKey] ?? 0) + 1;
    return accumulator;
  }, {});

  const data = Object.entries(grouped)
    .sort(([left], [right]) => new Date(left).getTime() - new Date(right).getTime())
    .map(([minute, count]) => ({
      minute: new Date(minute).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
      count,
    }));

  if (data.length === 0) {
    return <p className="empty-state">No events available for timeline.</p>;
  }

  return (
    <div className="chart-card">
      <h3>Events per Minute</h3>
      <div className="chart-area">
        <ResponsiveContainer width="100%" height="100%">
          <LineChart data={data} margin={{ top: 10, right: 20, left: 0, bottom: 0 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="minute" />
            <YAxis allowDecimals={false} />
            <Tooltip />
            <Line type="monotone" dataKey="count" stroke="#2563eb" strokeWidth={2} dot={false} />
          </LineChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
