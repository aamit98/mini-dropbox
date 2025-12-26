import React from "react";
import Auth from "./Auth";

export default function AuthPage({ onLoggedIn }:{ onLoggedIn:(u:string)=>void }) {
  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="logo" style={{justifyContent:"center", marginBottom:"8px"}}>
          <div className="dot" /> Mini Dropbox
        </div>
        <h1 style={{margin:"0 0 8px 0", fontSize:24, fontWeight:700}}>Welcome</h1>
        <p className="muted" style={{marginBottom:24, fontSize:14}}>
          Sign in to your account or create a new one to get started.
        </p>
        <Auth onLoggedIn={onLoggedIn} />
      </div>
    </div>
  );
}
