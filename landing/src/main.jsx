import React from "react";
import { createRoot } from "react-dom/client";
import App from "./App.jsx";
import Payment from "./pages/Payment.jsx";
import PaymentResult from "./pages/PaymentResult.jsx";
import "./index.css";

// Mini-routage par chemin : la landing reste un SPA, on ajoute 3 routes
// dédiées au paiement (/payer, /paiement/succes, /paiement/echec) servies
// par la même page via une rewrite Vercel.
function Root() {
  const path = window.location.pathname;
  if (path === "/payer") return <Payment />;
  if (path === "/paiement/succes") return <PaymentResult variant="success" />;
  if (path === "/paiement/echec") return <PaymentResult variant="error" />;
  return <App />;
}

createRoot(document.getElementById("root")).render(
  <React.StrictMode>
    <Root />
  </React.StrictMode>
);
