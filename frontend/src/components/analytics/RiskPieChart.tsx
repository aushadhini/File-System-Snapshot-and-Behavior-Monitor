import { Pie, PieChart, Cell, ResponsiveContainer, Tooltip, Legend } from "recharts";
import type { FileEvent } from "../../api/types";

interface RiskPieChartProps {
  events: FileEvent[];
}

const COLORS = {
  LOW: "#22c55e",
  MEDIUM: "#f59e0b",
  HIGH: "#ef4444",
};

export function RiskPieChart({ events }: RiskPieChartProps) {
  const data = [
    { name: "Low", value: events.filter((event) => event.riskLevel === "LOW").length, key: "LOW" },
    { name: "Medium", value: events.filter((event) => event.riskLevel === "MEDIUM").length, key: "MEDIUM" },
    { name: "High", value: events.filter((event) => event.riskLevel === "HIGH").length, key: "HIGH" },
  ].filter((entry) => entry.value > 0);

  if (data.length === 0) {
    return <p className="empty-state">No events available for risk distribution.</p>;
  }

  return (
    <div className="chart-card">
      <h3>Risk Distribution</h3>
      <div className="chart-area">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie data={data} dataKey="value" nameKey="name" outerRadius={90} label>
              {data.map((entry) => (
                <Cell key={entry.key} fill={COLORS[entry.key as keyof typeof COLORS]} />
              ))}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
