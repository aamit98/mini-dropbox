import React, { useEffect, useState } from "react";
import ActivityPanel from "./ActivityPanel";
import Files from "./Files";
import { clearToken, getToken, me } from "./api";
import AuthPage from "./Authpage";
import AdminPanel from "./AdminPanel";

type Mode = "files" | "recents" | "deleted" | "admin";

export default function App() {
  const [username, setUsername] = useState<string>("");
  const [isAdmin, setIsAdmin] = useState<boolean>(false);
  const [mode, setMode] = useState<Mode>("files");
  const [query, setQuery] = useState("");

  async function hydrate() {
    const t = getToken();
    if (!t) { setUsername(""); setIsAdmin(false); return; }
    try {
      const m = await me();
      setUsername(m.username || "");
      setIsAdmin(!!m.admin);
    } catch {
      clearToken();
      setUsername("");
      setIsAdmin(false);
    }
  }

  useEffect(() => { hydrate(); }, []);

  function logout() {
    clearToken();
    setUsername("");
    setIsAdmin(false);
  }

  if (!username) {
    return <AuthPage onLoggedIn={(u) => { setUsername(u); hydrate(); }} />;
  }

  return (
    <div className="app">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="logo"><div className="dot" /> Mini Dropbox</div>
        <nav className="nav">
          <a className={mode==="files"?"active":""} onClick={()=>setMode("files")}>Files</a>
          <a className={mode==="recents"?"active":""} onClick={()=>setMode("recents")}>Recents</a>
          <a className={mode==="deleted"?"active":""} onClick={()=>setMode("deleted")}>Deleted</a>
          {isAdmin && <a className={mode==="admin"?"active":""} onClick={()=>setMode("admin")}>Admin</a>}
        </nav>
        <div style={{marginTop:"auto", color:"var(--muted)"}}>© You</div>
      </aside>

      {/* Main column */}
      <section className="main">
        <div className="topbar">
          <div className="h1">{mode==="files"?"Your files": mode==="recents"?"Recent files": mode==="deleted"?"Deleted files":"Admin"}</div>

          {mode!=="admin" && (
            <div className="search">
              <span className="icon">🔎</span>
              <input placeholder="Search (client side)" value={query} onChange={e=>setQuery(e.target.value)} />
            </div>
          )}

          <div className="rightbar">
            <div className="pill">Signed in as <strong style={{marginLeft:6}}>{username}{isAdmin?" (admin)":""}</strong></div>
            <button type="button" className="btn" onClick={logout}>Logout</button>
          </div>
        </div>

        <div className="content">
          <div className="card">
            {mode==="admin"
              ? <AdminPanel />
              : <Files username={username} onLogout={logout} mode={mode} query={query} />
            }
          </div>

          {mode!=="admin" && (
            <div style={{display:"grid", gap:14}}>
              <PreviewDock />
              <ActivityPanel />
            </div>
          )}
        </div>
      </section>
    </div>
  );
}

/** Passive preview panel that listens for CustomEvents from Files.tsx */
function PreviewDock(){
  const [state, setState] = useState<{name?:string, url?:string, kind?:string, size?:string}>({});
  useEffect(()=>{
    function onEv(e:any){ setState(e.detail || {}); }
    window.addEventListener("file-preview", onEv as any);
    return ()=> window.removeEventListener("file-preview", onEv as any);
  },[]);
  return (
    <div className="card preview">
      <div className="head">
        <strong>Preview</strong>
        <span className="sp" />
        {state.name ? <span className="badge">{state.kind || "file"}</span> : null}
      </div>
      <div className="pad">
        {!state.name && <div className="muted">Hover or select a file to preview</div>}
        {state.name && renderPreview(state)}
      </div>
    </div>
  );
}

function renderPreview(s:{name?:string, url?:string, kind?:string}){
  if (!s.url) return <div className="muted">Loading preview…</div>;
  if (s.kind === "image") return <img src={s.url} alt={s.name} />;
  if (s.kind === "audio") return <audio controls src={s.url} />;
  if (s.kind === "pdf")   return <embed src={s.url} type="application/pdf" width="100%" height="100%" />;
  if (s.kind === "text")  return <iframe src={s.url} style={{width:"100%",height:"100%",border:"0"}} />;
  return <div className="muted">No preview available</div>;
}
