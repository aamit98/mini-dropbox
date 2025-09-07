import React, { useState } from "react";
import { createShare } from "./api";

export default function ShareModal({name, onClose}:{name:string, onClose:()=>void}){
  const [hours, setHours] = useState(24);
  const [maxDls, setMaxDls] = useState(10);
  const [url, setUrl] = useState<string>("");
  const [err, setErr] = useState<string>("");

  async function doCreate(){
    setErr("");
    setUrl("");
    try {
      const r = await createShare(name, { hours, maxDownloads: maxDls });
      // show absolute URL so you can click it right away
      const absolute = `${window.location.origin}${r.url}`;
      setUrl(absolute);
    } catch (e:any) {
      // show what actually happened (404 from v2 is the common case)
      setErr(typeof e?.message === "string" ? e.message : "Share failed");
    }
  }

  function copy(){
    if (!url) return;
    navigator.clipboard.writeText(url).catch(()=>{});
  }

  return (
    <div className="modal">
      <div className="modal-card">
        <div className="drawer-head">
          <strong>Create share link</strong>
          <span className="sp" />
          <button className="btn" onClick={onClose}>Close</button>
        </div>
        <div className="pad" style={{display:"grid", gap:10}}>
          <label>Expires in (hours)
            <input type="number" value={hours} onChange={e=>setHours(Math.max(1, +e.target.value||1))}/>
          </label>
          <label>Max downloads
            <input type="number" value={maxDls} onChange={e=>setMaxDls(Math.max(1, +e.target.value||1))}/>
          </label>

          <button className="btn" onClick={doCreate}>Create</button>
          {err && <div className="err">{err}</div>}
          {url && (
            <div>
              <a className="pill" href={url} target="_blank" rel="noreferrer">{url}</a>
              <button className="btn" onClick={copy} style={{marginLeft:8}}>Copy</button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
