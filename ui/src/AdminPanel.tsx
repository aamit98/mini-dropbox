import React, { useEffect, useState } from "react";
import { listUsers, deleteUser, toggleAdmin, UserInfo } from "./api";

export default function AdminPanel(){
  const [rows, setRows] = useState<UserInfo[]>([]);
  const [msg, setMsg] = useState("");

  async function refresh(){
    try { setRows(await listUsers()); setMsg(""); }
    catch (e){ setMsg("Admin API not available or you are not admin"); }
  }
  useEffect(()=>{ refresh(); },[]);

  async function doToggle(u: UserInfo){
    try { await toggleAdmin(u.username); refresh(); } catch {}
  }
  async function doDelete(u: UserInfo){
    if (!confirm(`Delete ${u.username}?`)) return;
    try { await deleteUser(u.username); refresh(); } catch {}
  }

  return (
    <div className="pad">
      <div className="toolbar"><strong>Users</strong></div>
      {!!msg && <div className="err" style={{marginBottom:8}}>{msg}</div>}
      <table className="table">
        <thead><tr><th>User</th><th>Admin</th><th className="cell-right">Actions</th></tr></thead>
        <tbody>
          {rows.map(u=>(
            <tr key={u.username}>
              <td>{u.username}</td>
              <td>{u.admin ? "Yes" : "No"}</td>
              <td className="cell-right">
                <a className="link" onClick={()=>doToggle(u)}>{u.admin?"Revoke admin":"Make admin"}</a>{" · "}
                <a className="link" onClick={()=>doDelete(u)}>Delete</a>
              </td>
            </tr>
          ))}
          {!rows.length && <tr><td colSpan={3} className="muted" style={{padding:12}}>No users.</td></tr>}
        </tbody>
      </table>
    </div>
  );
}
