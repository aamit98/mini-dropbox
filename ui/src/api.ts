const API = import.meta.env.VITE_API ?? ""; // leave VITE_API undefined in dev


/* ---------------- Token helpers ---------------- */
const KEY = "token";
export function setToken(t: string){ localStorage.setItem(KEY, t); }
export function getToken(){ return localStorage.getItem(KEY) || ""; }
export function clearToken(){ localStorage.removeItem(KEY); }
export type ShareOptions = { hours?: number; maxDownloads?: number; versionId?: number };

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

export async function me(): Promise<{username: string, admin?: boolean}>{
  const r = await fetch(`${API}/api/auth/me`, { headers: authHeaders() });
  if (!r.ok) throw new Error("Not logged in");
  return r.json();
}

/* ---------------- Files (v2) ---------------- */
export async function listFiles(): Promise<string[]>{
  const r = await fetch(`${API}/api/v2/files`, { headers: authHeaders() });
  if (!r.ok) throw new Error(await r.text());
  const entries = await r.json();
  return entries.map((entry: any) => entry.logicalName);
}

export async function listRecents(): Promise<string[]>{
  const r = await fetch(`${API}/api/v2/files/recents`, { headers: authHeaders() });
  if (!r.ok) throw new Error(await r.text());
  const entries = await r.json();
  return entries.map((entry: any) => entry.logicalName);
}

export async function listDeleted(): Promise<string[]>{
  const r = await fetch(`${API}/api/v2/files/deleted`, { headers: authHeaders() });
  if (!r.ok) throw new Error(await r.text());
  const entries = await r.json();
  return entries.map((entry: any) => entry.logicalName);
}

export async function undeleteFile(name: string){
  const r = await fetch(`${API}/api/v2/files/${encodeURIComponent(name)}/undelete`, {
    method: "POST",
    headers: authHeaders()
  });
  if (!r.ok) throw new Error(await r.text());
}

export async function deleteFile(name: string){
  const r = await fetch(`${API}/api/v2/files/${encodeURIComponent(name)}`, {
    method: "DELETE",
    headers: authHeaders()
  });
  if (!r.ok) throw new Error(await r.text());
}

export async function uploadFile(file: File){
  const fd = new FormData();
  fd.append("file", file);
  
  // Don't set Content-Type header - browser needs to set it with boundary for multipart/form-data
  const headers = authHeaders();
  
  try {
    const r = await fetch(`${API}/api/v2/files/upload`, {
      method: "POST",
      headers: headers, // Only Authorization header, no Content-Type
      body: fd
    });
    
    if (!r.ok) {
      let errorText = "Upload failed";
      try {
        errorText = await r.text();
        // Try to parse as JSON for better error messages
        try {
          const errorJson = JSON.parse(errorText);
          errorText = errorJson.message || errorJson.error || errorText;
        } catch {
          // Not JSON, use text as-is
        }
      } catch {
        errorText = `Upload failed: ${r.status} ${r.statusText}`;
      }
      throw new Error(errorText);
    }
    return r.json();
  } catch (error: any) {
    if (error.name === 'TypeError' && error.message.includes('fetch')) {
      throw new Error("Network error: Could not connect to server. Is the REST server running?");
    }
    throw error;
  }
}

export async function downloadFile(name: string, signal?: AbortSignal): Promise<Blob>{
  const r = await fetch(`${API}/api/v2/files/${encodeURIComponent(name)}`, {
    headers: authHeaders(),
    signal
  });
  if (!r.ok) throw new Error(await r.text());
  return r.blob();
}

/* ---------------- Optional thumbnails ---------------- */
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
  const token = getToken(); // read from localStorage
  const url = token ? `/api/logs/stream?token=${encodeURIComponent(token)}` : `/api/logs/stream`;
  const es = new EventSource(url);
  es.onmessage = (m) => { try { onEvent(JSON.parse(m.data)); } catch {} };
  es.onerror = () => { es.close(); };
  return () => es.close();
}
/* ---------------- Versions (v2) ---------------- */
export type FileVersion = {
  id: number; 
  versionNo: number; 
  sizeBytes: number;  // FIXED: Changed from 'size' to 'sizeBytes'
  createdAt: number; 
  createdBy: string;
};

export async function listVersions(name: string): Promise<FileVersion[]>{
  const r = await fetch(`${API}/api/v2/files/${encodeURIComponent(name)}/versions`, {
    headers: authHeaders()
  });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function restoreVersion(name: string, versionNo: number): Promise<void>{
  const r = await fetch(`${API}/api/v2/files/${encodeURIComponent(name)}/restore?version=${versionNo}`, {
    method: "POST",
    headers: authHeaders()
  });
  if (!r.ok) throw new Error(await r.text());
}

export async function createShare(name: string, opts: ShareOptions): Promise<{ url: string }>{
  const body: any = {};
  if (opts.hours !== undefined) body.hours = opts.hours;
  if (opts.maxDownloads !== undefined) body.maxDownloads = opts.maxDownloads;
  if (opts.versionId !== undefined) body.version = opts.versionId;
  const r = await fetch(`${API}/api/v2/files/${encodeURIComponent(name)}/share`, {
    method: "POST",
    headers: { ...authHeaders(), "Content-Type": "application/json" },
    body: JSON.stringify(body)
  });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

/* ---------------- Admin ---------------- */
export type UserInfo = { username: string; admin: boolean };

export async function listUsers(): Promise<UserInfo[]>{
  const r = await fetch(`${API}/api/admin/users`, { headers: authHeaders() });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function deleteUser(username: string){
  const r = await fetch(`${API}/api/admin/users/${encodeURIComponent(username)}`, {
    method: "DELETE",
    headers: authHeaders()
  });
  if (!r.ok) throw new Error(await r.text());
}

export async function toggleAdmin(username: string): Promise<UserInfo>{
  const r = await fetch(`${API}/api/admin/users/${encodeURIComponent(username)}/toggle-admin`, {
    method: "POST",
    headers: authHeaders()
  });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}