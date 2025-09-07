import React, { useEffect, useMemo, useState } from "react";
import { fetchRecentLogs, streamLogs, TftpLogEvent } from "./api";

function fmtTs(ts:number){ try { return new Date(ts).toLocaleTimeString(); } catch { return "" } }

export default function ActivityPanel(){
  const [rows, setRows] = useState<TftpLogEvent[]>([]);

  useEffect(()=>{
    let stop = () => {};
    (async () => {
      try { setRows(await fetchRecentLogs(50)); } catch {}
      stop = streamLogs(ev => setRows(prev => {
        const next = [...prev, ev];
        if (next.length > 200) next.shift();
        return next;
      }));
    })();
    return () => stop();
  },[]);

  const neat = useMemo(()=> rows.slice().reverse(), [rows]);

  return (
    <div className="card">
      <div className="toolbar"><strong>Activity (live)</strong></div>
      <div className="body" style={{maxHeight: 260, overflow:"auto"}}>
        <table className="table">
          <thead>
            <tr><th style={{width:88}}>Time</th><th>Event</th><th>User</th><th>File</th></tr>
          </thead>
          <tbody>
            {neat.map((e,i)=>(
              <tr key={i}>
                <td>{fmtTs(e.ts)}</td>
                <td>{badge(e.event)}</td>
                <td>{e.user || ""}</td>
                <td>{e.file || ""}</td>
              </tr>
            ))}
            {!neat.length && <tr><td colSpan={4} className="muted" style={{padding:12}}>No activity yet.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function badge(ev:string){
  const color = ev.startsWith("FILE_") ? "var(--good)" : ev==="ERROR" ? "var(--danger)" : "var(--brand-2)";
  return <span className="badge" style={{border:`1px solid ${color}`}}>{ev}</span>;
}
