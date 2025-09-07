import { createRoot } from "react-dom/client";
import App from "./App";
import "./styles.css";

console.log("[main] booting (normal)");
createRoot(document.getElementById("root")!).render(<App />);