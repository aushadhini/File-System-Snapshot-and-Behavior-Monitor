export const API_BASE = "http://localhost:8080";

export async function healthCheck() {
  const res = await fetch(`${API_BASE}/`);
  return res.text();
}
