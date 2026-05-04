import type { FileEvent } from "../../api/types";

interface AnalyticsCardsProps {
  events: FileEvent[];
}

export function AnalyticsCards({ events }: AnalyticsCardsProps) {
  const lowCount = events.filter((event) => event.riskLevel === "LOW").length;
  const mediumCount = events.filter((event) => event.riskLevel === "MEDIUM").length;
  const highCount = events.filter((event) => event.riskLevel === "HIGH").length;
  const honeypotCount = events.filter((event) => event.honeypotTriggered).length;

  const cards = [
    { label: "Total", value: events.length },
    { label: "Low", value: lowCount },
    { label: "Medium", value: mediumCount },
    { label: "High", value: highCount },
    { label: "Honeypot", value: honeypotCount },
  ];

  return (
    <div className="analytics-cards">
      {cards.map((card) => (
        <article key={card.label} className="analytics-card">
          <span>{card.label}</span>
          <strong>{card.value}</strong>
        </article>
      ))}
    </div>
  );
}
