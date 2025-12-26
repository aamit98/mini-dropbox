import React, { useState } from "react";
import { login, register } from "./api";

export default function Auth({ onLoggedIn }:{ onLoggedIn:(u:string)=>void }){
  const [u,setU] = useState(""); const [p,setP] = useState("");
  const [msg,setMsg] = useState<{t:""|"ok"|"err", m:string}>({t:"", m:""});
  const [loading, setLoading] = useState(false);

  async function doRegister(){
    if (!u.trim() || !p.trim()) {
      setMsg({t:"err", m:"Please enter username and password"});
      return;
    }
    setLoading(true);
    setMsg({t:"", m:""});
    try{
      await register(u.trim(),p);
      setMsg({t:"ok", m:"Registration successful! You can now login."});
      setP(""); // Clear password after registration
    }catch(e: any){ 
      setMsg({t:"err", m: e.message || "Registration failed. Username may already exist."}) 
    } finally {
      setLoading(false);
    }
  }
  
  async function doLogin(){
    if (!u.trim() || !p.trim()) {
      setMsg({t:"err", m:"Please enter username and password"});
      return;
    }
    setLoading(true);
    setMsg({t:"", m:""});
    try{
      await login(u.trim(),p);
      onLoggedIn(u.trim());
    }catch(e: any){ 
      setMsg({t:"err", m: e.message || "Login failed. Check your credentials."}) 
    } finally {
      setLoading(false);
    }
  }

  function handleKeyPress(e: React.KeyboardEvent, action: () => void) {
    if (e.key === 'Enter') {
      action();
    }
  }

  return (
    <div className="auth-form">
      <div className="auth-input-group">
        <label>Username</label>
        <input 
          placeholder="Enter username" 
          value={u} 
          onChange={e=>setU(e.target.value)}
          onKeyPress={e => handleKeyPress(e, doLogin)}
          disabled={loading}
        />
      </div>
      <div className="auth-input-group">
        <label>Password</label>
        <input 
          placeholder="Enter password" 
          type="password" 
          value={p} 
          onChange={e=>setP(e.target.value)}
          onKeyPress={e => handleKeyPress(e, doLogin)}
          disabled={loading}
        />
      </div>
      
      <div className="auth-demo-info">
        <div className="muted" style={{fontSize: "12px", marginBottom: "12px"}}>
          <strong>Demo Account:</strong> Username: <code>admin</code> | Password: <code>admin</code>
        </div>
      </div>

      {msg.t==="ok" && <div className="ok">{msg.m}</div>}
      {msg.t==="err" && <div className="err">{msg.m}</div>}

      <div className="auth-buttons">
        <button 
          className="btn-outline" 
          onClick={doRegister}
          disabled={loading}
        >
          {loading ? "..." : "Register"}
        </button>
        <button 
          className="btn" 
          onClick={doLogin}
          disabled={loading}
        >
          {loading ? "..." : "Sign In"}
        </button>
      </div>
    </div>
  );
}
