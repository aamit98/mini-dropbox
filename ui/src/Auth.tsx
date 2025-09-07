import React, { useState } from "react";
import { login, register } from "./api";

export default function Auth({ onLoggedIn }:{ onLoggedIn:(u:string)=>void }){
  const [u,setU] = useState(""); const [p,setP] = useState("");
  const [msg,setMsg] = useState<{t:""|"ok"|"err", m:string}>({t:"", m:""});

  async function doRegister(){
    try{
      await register(u,p);
      setMsg({t:"ok", m:"Registered. Now login."});
    }catch{ setMsg({t:"err", m:"Register failed"}) }
  }
  async function doLogin(){
    try{
      await login(u,p);
      onLoggedIn(u);
    }catch{ setMsg({t:"err", m:"Login failed"}) }
  }

  return (
    <div className="authbar">
      <input placeholder="username" value={u} onChange={e=>setU(e.target.value)} />
      <input placeholder="password" type="password" value={p} onChange={e=>setP(e.target.value)} />
      <button className="btn" onClick={doRegister}>Register</button>
      <button className="btn" onClick={doLogin}>Login</button>
      {msg.t==="ok" && <span className="ok">{msg.m}</span>}
      {msg.t==="err" && <span className="err">{msg.m}</span>}
    </div>
  );
}
