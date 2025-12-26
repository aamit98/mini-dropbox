import React, { useEffect, useState } from "react";
import { FileVersion, listVersions, restoreVersion } from "./api";

export default function VersionDrawer({name, onClose}:{name:string, onClose:()=>void}){
  const [rows, setRows] = useState<FileVersion[]>([]);
  const [msg, setMsg] = useState<string>("");

  useEffect(()=>{
    (async ()=>{
      try { setRows(await listVersions(name)); setMsg(""); }
      catch (e:any) {
        setMsg("No versions found for this file (upload it again to enable versions).");
      }
    })();
  },[name]);

  async function doRestore(v: FileVersion){
    try {
      await restoreVersion(name, v.versionNo);
      setMsg(`✓ Restored to v${v.versionNo}`);
      // Refresh version list after restore
      setTimeout(() => {
        listVersions(name).then(setRows).catch(() => {});
      }, 500);
    } catch (error: any) {
      setMsg(`Failed to restore: ${error.message || 'Unknown error'}`);
    }
  }
  
  return (
    <div className="drawer">
      <div className="drawer-head">
        <strong>Version history</strong>
        <span className="sp" />
        <button className="btn" onClick={onClose}>Close</button>
      </div>
      <div className="drawer-body">
        {!!msg && <div className="muted" style={{marginBottom:8}}>{msg}</div>}
        <table className="table">
          <thead><tr><th>#</th><th>Size</th><th>Created</th><th>By</th><th className="cell-right">Actions</th></tr></thead>
          <tbody>
            {rows.map(v=>(
              <tr key={v.id}>
                <td>v{v.versionNo}</td>
                <td>{(v.sizeBytes/1024).toFixed(1)} KB</td>
                <td>{new Date(v.createdAt).toLocaleString()}</td>
                <td>{v.createdBy}</td>
                <td className="cell-right"><a className="link" onClick={()=>doRestore(v)}>Restore</a></td>
              </tr>
            ))}
            {!rows.length && <tr><td colSpan={5} className="muted" style={{padding:12}}>No versions yet.</td></tr>}
          </tbody>
        </table>
      </div>
    </div>
  );
}