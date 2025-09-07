// src/Files.tsx
import React, { useEffect, useRef, useState } from "react";
import { downloadFile, listFiles, uploadFile, getThumbURL } from "./api";
import VersionDrawer from "./VersionDrawer";
import ShareModal from "./ShareModal";

type Props = { username: string; onLogout: () => void };
type Row = { name: string };

export default function Files({ username, onLogout }: Props) {
  const [rows, setRows] = useState<Row[]>([]);
  const [sel, setSel] = useState<string>("");
  const [showHistory, setShowHistory] = useState<string | undefined>();
  const [showShare, setShowShare] = useState<string | undefined>();
  const inputRef = useRef<HTMLInputElement>(null);

  async function refresh() {
    if (!username) { setRows([]); return; }
    const names = await listFiles();
    setRows(names.map((n) => ({ name: n })));
    if (names.length && !sel) setSel(names[0]);
  }

  useEffect(() => { refresh(); /* eslint-disable-next-line react-hooks/exhaustive-deps */ }, [username]);

  async function onUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0];
    if (!f) return;
    await uploadFile(f);
    e.target.value = "";
    refresh();
  }

  // ---- Preview logic ----
  const hoverTimer = useRef<number | undefined>();
  const lastAbort = useRef<AbortController | null>(null);

  function schedulePreview(name: string) {
    window.clearTimeout(hoverTimer.current);
    hoverTimer.current = window.setTimeout(() => loadPreview(name), 120);
  }
  function broadcast(detail: any) {
    window.dispatchEvent(new CustomEvent("file-preview", { detail }));
  }

  async function loadPreview(name: string) {
    lastAbort.current?.abort();
    const ac = new AbortController();
    lastAbort.current = ac;
    try {
      // Try thumbnail fast (non-blocking)
      getThumbURL(name).then((url) => {
        if (url) broadcast({ name, url, kind: "image" });
      }).catch(() => {});

      const blob = await downloadFile(name, ac.signal);
      const url = URL.createObjectURL(blob);
      const kind = guessKind(name, blob.type);
      broadcast({ name, url, kind });
    } catch {
      broadcast({ name, url: undefined, kind: "unknown" });
    }
  }

  function onRowEnter(n: string) { schedulePreview(n); }
  function onRowClick(n: string) { setSel(n); loadPreview(n); }

  return (
    <>
        <div className="toolbar">
        <div className="filepick">
            <input ref={inputRef} id="fsel" type="file" onChange={onUpload}/>
            <label htmlFor="fsel" className="btn-outline">Choose file</label>
        </div>
        <button className="btn" onClick={()=>inputRef.current?.click()}>Upload</button>
        <button className="btn-outline" onClick={refresh}>Refresh</button>
        <div className="sp" />
        {username && <span className="badge">User: {username}</span>}
        </div>

      <div className="body" style={{ overflow: "auto" }}>
        <table className="table">
          <thead>
            <tr>
              <th>Name</th>
              <th className="cell-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((r) => (
              <tr
                key={r.name}
                className={"row" + (sel === r.name ? " selected" : "")}
                onMouseEnter={() => onRowEnter(r.name)}
                onClick={() => onRowClick(r.name)}
              >
                <td>{iconFor(r.name)} {r.name}</td>
                <td className="cell-right">
                  <a className="link" onClick={(e) => { e.stopPropagation(); onRowClick(r.name); }}>Preview</a>{" · "}
                  <a className="link" onClick={(e) => { e.stopPropagation(); saveAs(r.name); }}>Download</a>{" · "}
                  <a className="link" onClick={(e) => { e.stopPropagation(); setShowHistory(r.name); }}>History</a>{" · "}
                  <a className="link" onClick={(e) => { e.stopPropagation(); setShowShare(r.name); }}>Share</a>
                </td>
              </tr>
            ))}
            {!rows.length && (
            <tr>
                <td className="muted" colSpan={2} style={{padding:"16px"}}>
                No files yet. Upload something!
                </td>
            </tr>
            )}

          </tbody>
        </table>
      </div>

      {/* Drawers / Modals */}
      {showHistory && <VersionDrawer name={showHistory} onClose={() => setShowHistory(undefined)} />}
      {showShare && <ShareModal name={showShare} onClose={() => setShowShare(undefined)} />}
    </>
  );
}

/* ---------- helpers ---------- */

function guessKind(name: string, mime: string) {
  const n = name.toLowerCase();
  if (mime.startsWith("image/") || /\.(png|jpg|jpeg|gif|webp|bmp|svg)$/.test(n)) return "image";
  if (mime.startsWith("audio/") || /\.(mp3|wav|ogg)$/.test(n)) return "audio";
  if (mime === "application/pdf" || /\.pdf$/.test(n)) return "pdf";
  if (/\.(txt|md|json|csv|log)$/i.test(n)) return "text";
  return "file";
}

function iconFor(name: string) {
  const k = guessKind(name, "");
  const map: any = { image: "🖼️", audio: "🎵", pdf: "📄", text: "📝", file: "📦" };
  return <span style={{ marginRight: 8 }}>{map[k] || "📦"}</span>;
}

async function saveAs(name: string) {
  const blob = await downloadFile(name);
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url; a.download = name; a.click();
  URL.revokeObjectURL(url);
}
