import React, { useEffect, useRef, useState } from "react";
import {
  downloadFile,
  listFiles,
  listRecents,
  listDeleted,
  uploadFile,
  getThumbURL,
  deleteFile,
  undeleteFile,
} from "./api";
import VersionDrawer from "./VersionDrawer";
import ShareModal from "./ShareModal";

type Mode = "files" | "recents" | "deleted";
type Props = { username: string; onLogout: () => void; mode?: Mode; query?: string };
type Row = { name: string };

export default function Files({ username, onLogout, mode = "files", query = "" }: Props) {
  // ✅ all hooks are inside the component
  const [rows, setRows] = useState<Row[]>([]);
  const [sel, setSel] = useState<string>("");
  const [showHistory, setShowHistory] = useState<string | undefined>();
  const [showShare, setShowShare] = useState<string | undefined>();
  const [msg, setMsg] = useState<string>("");
  const [uploading, setUploading] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  async function refresh() {
    if (!username) { setRows([]); return; }
    let names: string[] = [];
    if (mode === "files") names = await listFiles();
    else if (mode === "recents") names = await listRecents();
    else names = await listDeleted();

    if (query) {
      const q = query.toLowerCase();
      names = names.filter(n => n.toLowerCase().includes(q));
    }
    setRows(names.map((n) => ({ name: n })));
    if (names.length && !sel) setSel(names[0]);
  }

  useEffect(() => { refresh(); /* eslint-disable-next-line react-hooks/exhaustive-deps */ }, [username, mode, query]);

  function onFileSelect(e: React.ChangeEvent<HTMLInputElement>) {
    const f = e.target.files?.[0];
    setSelectedFile(f || null);
    if (f) {
      setMsg(`Selected: ${f.name} (${(f.size / 1024).toFixed(1)} KB)`);
    }
  }

  async function handleUpload() {
    if (!selectedFile) {
      setMsg("Please select a file first");
      inputRef.current?.click();
      return;
    }

    setUploading(true);
    setMsg(`Uploading ${selectedFile.name}...`);
    try {
      await uploadFile(selectedFile);
      setMsg(`Successfully uploaded ${selectedFile.name}!`);
      setSelectedFile(null);
      if (inputRef.current) {
        inputRef.current.value = "";
      }
      // Refresh file list after a short delay
      setTimeout(() => {
        refresh();
        setMsg("");
      }, 1000);
    } catch (err: any) {
      console.error("Upload error:", err);
      const errorMsg = err?.message || err?.toString() || "Upload failed";
      setMsg(`Upload failed: ${errorMsg}`);
    } finally {
      setUploading(false);
    }
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
      // try fast thumbnail (non-blocking)
      getThumbURL(name).then((url) => { if (url) broadcast({ name, url, kind: "image" }); }).catch(() => {});
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

  async function doDelete(name: string) {
    if (mode !== "files") return;
    if (!confirm(`Delete ${name}? It will move to Deleted.`)) return;
    await deleteFile(name);
    refresh();
  }

  async function doUndelete(name: string) {
    if (mode !== "deleted") return;
    await undeleteFile(name);
    refresh();
  }

  return (
    <>
      {mode === "files" && (
        <div className="toolbar">
          <div className="filepick">
            <input 
              ref={inputRef} 
              id="fsel" 
              type="file" 
              onChange={onFileSelect}
              disabled={uploading}
            />
            <label htmlFor="fsel" className="btn-outline" style={{opacity: uploading ? 0.6 : 1}}>
              {selectedFile ? `Selected: ${selectedFile.name}` : "Choose file"}
            </label>
          </div>
          <button 
            className="btn" 
            onClick={handleUpload}
            disabled={uploading || !selectedFile}
            style={{opacity: (!selectedFile || uploading) ? 0.6 : 1}}
          >
            {uploading ? "Uploading..." : "Upload"}
          </button>
          <button 
            className="btn-outline" 
            onClick={refresh}
            disabled={uploading}
          >
            Refresh
          </button>
          <div className="sp" />
          {username && <span className="badge">User: {username}</span>}
          {!!msg && (
            <div className={msg.includes("failed") || msg.includes("Error") ? "err" : msg.includes("Success") ? "ok" : ""} style={{ marginLeft: 12, fontSize: "13px" }}>
              {msg}
            </div>
          )}
        </div>
      )}

      <div className="body" style={{ overflow: "auto" }}>
        <table className="table">
          <thead>
            <tr><th>Name</th><th className="cell-right">Actions</th></tr>
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
                  {mode !== "deleted" ? (
                    <>
                      <a className="link" onClick={(e) => { e.stopPropagation(); onRowClick(r.name); }}>Preview</a>{" · "}
                      <a className="link" onClick={(e) => { e.stopPropagation(); saveAs(r.name); }}>Download</a>{" · "}
                      <a className="link" onClick={(e) => { e.stopPropagation(); setShowHistory(r.name); }}>History</a>{" · "}
                      <a className="link" onClick={(e) => { e.stopPropagation(); setShowShare(r.name); }}>Share</a>{" · "}
                      <a className="link" onClick={(e) => { e.stopPropagation(); doDelete(r.name); }}>Delete</a>
                    </>
                  ) : (
                    <a className="link" onClick={(e) => { e.stopPropagation(); doUndelete(r.name); }}>Restore</a>
                  )}
                </td>
              </tr>
            ))}
            {!rows.length && (
              <tr>
                <td className="muted" colSpan={2} style={{ padding: "16px" }}>
                  {mode === "deleted" ? "Trash is empty." : "No files yet. Upload something!"}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {mode !== "deleted" && showHistory && <VersionDrawer name={showHistory} onClose={() => setShowHistory(undefined)} />}
      {mode !== "deleted" && showShare && <ShareModal name={showShare} onClose={() => setShowShare(undefined)} />}
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
