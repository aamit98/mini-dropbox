// ui/src/api.ts
const API = import.meta.env.VITE_API ?? ""; // vite proxy forwards /api to :8080

/* ---------------- Token helpers ---------------- */
const KEY = "token";
export function setToken(t: string){ localStorage.setItem(KEY, t); }
export function getToken(){ return localStorage.getItem(KEY) || ""; }
export function clearToken(){ localStorage.removeItem(KEY); }

/* ---------------- Headers ---------------- */
export function authHeaders(): Record<string, string> {
  const t = getToken();
  const h: Record<string, string> = {};
  if (t) h["Authorization"] = `Bearer ${t}`;
  return h;
}

/* ---------------- Auth ---------------- */
export async function register(username: string, password: string){
  const r = await fetch(`${API}/api/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password })
  });
  if (!r.ok) throw new Error(await r.text());
}

export async function login(username: string, password: string){
  const r = await fetch(`${API}/api/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password })
  });
  if (!r.ok) throw new Error(await r.text());
  const j = await r.json();
  if (!j.token) throw new Error("no token");
  setToken(j.token);
  return j;
}

export async function me(): Promise<{username: string}>{
  const r = await fetch(`${API}/api/auth/me`, { headers: authHeaders() });
  if (!r.ok) throw new Error("Not logged in");
  return r.json();
}

/* ---------------- Files (v1) ---------------- */
export async function listFiles(): Promise<string[]>{
  const r = await fetch(`${API}/api/files`, { headers: authHeaders() });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function uploadFile(file: File){
  const fd = new FormData();
  fd.append("file", file);
  const r = await fetch(`${API}/api/files/upload`, {
    method: "POST",
    headers: authHeaders(),
    body: fd
  });
  if (!r.ok) throw new Error(await r.text());
}

export async function downloadFile(name: string, signal?: AbortSignal): Promise<Blob>{
  const r = await fetch(`${API}/api/files/${encodeURIComponent(name)}`, {
    headers: authHeaders(),
    signal
  });
  if (!r.ok) throw new Error(await r.text());
  return r.blob();
}

/* ---------------- Optional thumbnails (works when backend adds it) ---------------- */
export async function getThumbURL(name: string): Promise<string | null> {
  try {
    const r = await fetch(`${API}/api/files/${encodeURIComponent(name)}/thumb`, {
      headers: authHeaders()
    });
    if (!r.ok) return null;
    const b = await r.blob();
    return URL.createObjectURL(b);
  } catch { return null; }
}

/* ---------------- Logs (SSE) ---------------- */
export type TftpLogEvent = {
  ts: number; event: string; user?: string; file?: string;
  block?: number; code?: number; msg?: string;
};

export async function fetchRecentLogs(limit = 50): Promise<TftpLogEvent[]>{
  const r = await fetch(`${API}/api/logs?limit=${limit}`, { headers: authHeaders() });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export function streamLogs(onEvent: (e: TftpLogEvent)=>void): () => void {
  const es = new EventSource(`${API}/api/logs/stream`);
  es.onmessage = (m) => {
    try { onEvent(JSON.parse(m.data)); } catch {}
  };
  es.onerror = () => { es.close(); };
  return () => es.close();
}

/* ---------------- Versions (placeholder endpoints) ---------------- */
// Backend you’ll add later (suggested paths).
export type FileVersion = {
  id: number; versionNo: number; size: number; createdAt: number; createdBy: string;
};

export async function listVersions(name: string): Promise<FileVersion[]>{
  const r = await fetch(`${API}/api/v2/files/${encodeURIComponent(name)}/versions`, {
    headers: authHeaders()
  });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function restoreVersion(name: string, versionId: number): Promise<void>{
  const r = await fetch(`${API}/api/v2/files/${encodeURIComponent(name)}/restore?version=${versionId}`, {
    method: "POST",
    headers: authHeaders()
  });
  if (!r.ok) throw new Error(await r.text());
}

/* ---------------- Share links (placeholder endpoints) ---------------- */
export type ShareOptions = { hours?: number; maxDownloads?: number; versionId?: number };
export async function createShare(name: string, opts: ShareOptions): Promise<{ url: string }>{
  const r = await fetch(`${API}/api/v2/files/${encodeURIComponent(name)}/share`, {
    method: "POST",
    headers: { ...authHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify(opts)
  });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}
