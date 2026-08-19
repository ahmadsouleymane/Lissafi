"use client";

import { useEffect } from "react";

export default function GlobalError({ error, reset }: { error: Error & { digest?: string }; reset: () => void }) {
  useEffect(() => {
    console.error("[global] erreur de rendu:", error);
  }, [error]);

  return (
    <html lang="fr">
      <body>
        <div style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", background: "#0f5132", padding: "16px" }}>
          <div style={{ maxWidth: 380, width: "100%", background: "white", borderRadius: 16, padding: 24, boxShadow: "0 10px 30px rgba(0,0,0,.2)" }}>
            <h1 style={{ fontSize: 18, fontWeight: 700, color: "#0f172a", margin: 0 }}>Une erreur est survenue</h1>
            <p style={{ fontSize: 14, color: "#475569", marginTop: 8 }}>
              Le back-office n&apos;a pas pu s&apos;afficher. Souvent temporaire — réessaie dans un instant.
            </p>
            {error.digest && <p style={{ fontSize: 12, color: "#94a3b8", marginTop: 8 }}>Référence : {error.digest}</p>}
            <button
              onClick={reset}
              style={{ marginTop: 16, background: "#2e8b57", color: "white", border: "none", borderRadius: 8, padding: "8px 16px", fontSize: 14, fontWeight: 500, cursor: "pointer" }}
            >
              Réessayer
            </button>
          </div>
        </div>
      </body>
    </html>
  );
}
