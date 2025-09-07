import React from "react";
import Auth from "./Auth";

export default function AuthPage({ onLoggedIn }:{ onLoggedIn:(u:string)=>void }) {
  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="logo" style={{justifyContent:"center"}}><div className="dot" /> Mini Dropbox</div>
        <h1 style={{margin:"8px 0 4px", fontSize:20}}>Sign in</h1>
        <p className="muted" style={{marginBottom:12}}>Create an account or sign in to continue.</p>
        <Auth onLoggedIn={onLoggedIn} />
      </div>
    </div>
  );
}
