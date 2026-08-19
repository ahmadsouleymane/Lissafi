import { useEffect, useState } from "react";
import { ArrowLeft, CheckCircle2, Loader2, XCircle } from "lucide-react";
import { pollStatus } from "../lib/payments";

// ============================================================
// Page de retour après la checkout GeniusPay (/paiement/succes
// ou /paiement/echec). Lit la référence stockée avant la redirection
// et interroge le statut jusqu'à confirmation.
// ============================================================

export default function PaymentResult({ variant }) {
  const isError = variant === "error";
  const [state, setState] = useState(isError ? "error" : "checking"); // checking | success | error
  const [status, setStatus] = useState("");
  const [activated, setActivated] = useState(true);

  useEffect(() => {
    if (isError) return;

    let stored = null;
    try {
      stored = JSON.parse(localStorage.getItem("lissafi_payment") || "null");
    } catch {
      stored = null;
    }
    if (!stored?.reference || !stored?.provider) {
      // Pas de référence connue : on affiche une confirmation générique.
      setState("unknown");
      return;
    }

    let cancelled = false;
    (async () => {
      const result = await pollStatus(stored.provider, stored.reference, {
        onTick: (d) => {
          if (!cancelled) setStatus(d.status);
        },
      });
      if (cancelled) return;
      const ok = result.status === "succeeded" || result.status === "completed";
      setActivated(result.activated === true);
      setState(ok ? "success" : "error");
      try {
        localStorage.removeItem("lissafi_payment");
      } catch {
        // sans impact
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [isError]);

  return (
    <div className="pay-shell">
      <header className="pay-header">
        <a className="pay-back" href="/">
          <ArrowLeft size={16} /> Retour au site
        </a>
      </header>
      <main className="pay-main">
        <div className="pay-result">
          {state === "checking" && (
            <>
              <Loader2 size={52} className="pay-result-ico spin" />
              <h1>Confirmation en cours…</h1>
              <p>Nous vérifions ton paiement. Reste sur cette page un instant.</p>
              {status && <span className="pay-poll-state">Statut : {status}</span>}
            </>
          )}

          {state === "success" && (
            <>
              <CheckCircle2 size={52} className="pay-result-ico ok" />
              <h1>{activated ? "Paiement confirmé 🎉" : "Paiement reçu"}</h1>
              <p>
                {activated
                  ? "Ton compte est activé pour 1 an. Ouvre l'app Lissafi pour en profiter."
                  : "Nous n'avons pas trouvé de compte Lissafi avec cet email. Vérifie l'email saisi puis contacte-nous sur WhatsApp avec ta preuve de paiement."}
              </p>
              <a className="btn btn-primary btn-lg" href="/#installation">
                Télécharger l'app
              </a>
            </>
          )}

          {state === "unknown" && (
            <>
              <CheckCircle2 size={52} className="pay-result-ico ok" />
              <h1>Merci pour ton paiement</h1>
              <p>
                Ton compte sera activé automatiquement d'ici quelques minutes. Si rien
                ne change dans l'app, contacte-nous sur WhatsApp.
              </p>
              <a className="btn btn-primary btn-lg" href="/#installation">
                Télécharger l'app
              </a>
            </>
          )}

          {state === "error" && (
            <>
              <XCircle size={52} className="pay-result-ico bad" />
              <h1>{isError ? "Paiement annulé" : "Paiement non confirmé"}</h1>
              <p>
                {isError
                  ? "Le paiement a été annulé. Tu peux réessayer quand tu veux."
                  : "Nous n'avons pas pu confirmer le paiement. S'il a été débité, il sera remboursé — sinon, réessaie."}
              </p>
              <a className="btn btn-ghost btn-lg" href="/payer">
                Réessayer
              </a>
            </>
          )}
        </div>
      </main>
    </div>
  );
}
