import { useEffect, useRef, useState } from "react";
import {
  ArrowLeft,
  CheckCircle2,
  CreditCard,
  Loader2,
  Smartphone,
  XCircle,
} from "lucide-react";
import { initiatePayment, pollStatus } from "../lib/payments";
import { formatFCFA } from "../lib/format";
import { WHATSAPP_NUMBER } from "../config";

// ============================================================
// Page de paiement en ligne (/payer). Hors de l'app, sur le même
// site que la landing. Aucune clé de paiement ici : on appelle le
// back-office, qui route iPayMoney (Niger) ou GeniusPay (ailleurs).
// ============================================================

const PLANS = {
  plus: {
    name: "Petite boutique",
    amount: 24000,
    desc: "~200 produits, ventes illimitées, 1 an",
  },
  business: {
    name: "Commerce / Supermarché",
    amount: 50000,
    desc: "Tout illimité, multi-caisses, 1 an",
  },
};

// Le paiement en ligne active un abonnement annuel. Les cadences mensuelle/
// trimestrielle choisies dans l'app passent par WhatsApp (activation manuelle).
function whatsappPayUrl(planName) {
  const text = `Bonjour, je veux m'abonner à Lissafi (${planName}). Comment payer ?`;
  return `https://wa.me/${WHATSAPP_NUMBER}?text=${encodeURIComponent(text)}`;
}

const COUNTRIES = [
  ["NE", "Niger"],
  ["CI", "Côte d'Ivoire"],
  ["SN", "Sénégal"],
  ["BJ", "Bénin"],
  ["TG", "Togo"],
  ["BF", "Burkina Faso"],
  ["ML", "Mali"],
  ["CM", "Cameroun"],
  ["CG", "Congo"],
  ["CD", "RD Congo"],
  ["GA", "Gabon"],
  ["RW", "Rwanda"],
  ["KE", "Kenya"],
  ["UG", "Ouganda"],
  ["ZM", "Zambie"],
];

export default function Payment() {
  const [plan, setPlan] = useState(
    () => new URLSearchParams(window.location.search).get("plan") === "business" ? "business" : "plus",
  );
  const period = new URLSearchParams(window.location.search).get("period") || "yearly";
  const wantsShortPeriod = period === "monthly" || period === "quarterly";
  const [method, setMethod] = useState("mobile_money");
  const [country, setCountry] = useState("NE");
  const [name, setName] = useState("");
  const [email, setEmail] = useState(
    () => new URLSearchParams(window.location.search).get("email") || "",
  );
  const [phone, setPhone] = useState("");

  const [phase, setPhase] = useState("form"); // form | waiting | done-success | done-error
  const [error, setError] = useState("");
  const [pollState, setPollState] = useState("");
  const [activated, setActivated] = useState(true);
  const pending = useRef(null);

  const isNigerMobile = method === "mobile_money" && country === "NE";

  async function submit(e) {
    e.preventDefault();
    setError("");
    setPhase("form");

    if (!name.trim() || !email.trim() || !phone.trim()) {
      setError("Renseigne ton nom, ton email et ton téléphone.");
      return;
    }
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email.trim())) {
      setError("Cet email ne semble pas valide.");
      return;
    }

    try {
      const result = await initiatePayment({
        plan,
        method,
        country: method === "card" ? "" : country,
        name: name.trim(),
        email: email.trim(),
        phone: phone.trim(),
      });

      if (result.mode === "redirect") {
        // GeniusPay : on retient la référence puis on part sur sa page checkout.
        try {
          localStorage.setItem(
            "lissafi_payment",
            JSON.stringify({ provider: result.provider, reference: result.reference }),
          );
        } catch {
          // Stockage indisponible — le retour se fera sans suivi précis.
        }
        window.location.href = result.redirectUrl;
        return;
      }

      // iPayMoney (push-to-phone) : le client valide sur son téléphone.
      pending.current = { provider: result.provider, reference: result.reference };
      setPhase("waiting");
    } catch (err) {
      setError(err.message || "Une erreur est survenue. Réessaie.");
    }
  }

  // Polling du statut tant qu'on est en attente d'un push iPayMoney.
  useEffect(() => {
    if (phase !== "waiting" || !pending.current) return;
    let cancelled = false;
    (async () => {
      const result = await pollStatus(pending.current.provider, pending.current.reference, {
        onTick: (d) => {
          if (!cancelled) setPollState(d.status);
        },
      });
      if (cancelled) return;
      const ok = result.status === "succeeded" || result.status === "completed";
      setActivated(result.activated === true);
      setPhase(ok ? "done-success" : "done-error");
    })();
    return () => {
      cancelled = true;
    };
  }, [phase]);

  if (phase === "done-success") {
    return <Result kind="success" plan={plan} activated={activated} />;
  }
  if (phase === "done-error") {
    return <Result kind="error" onRetry={() => setPhase("form")} />;
  }

  return (
    <div className="pay-shell">
      <header className="pay-header">
        <a className="pay-back" href="/">
          <ArrowLeft size={16} /> Retour au site
        </a>
        <a className="brand" href="/" aria-label="Lissafi">
          <img className="brand-logo" src="/assets/logo-header.svg" alt="Lissafi" />
        </a>
      </header>

      <main className="pay-main">
        <div className="pay-head">
          <span className="kicker">Paiement en ligne</span>
          <h1>Active ton Lissafi, paie en 1 minute.</h1>
          <p>
            Paiement sécurisé. Après confirmation, ton compte est activé
            automatiquement (1 an).
          </p>
        </div>

        <form className="pay-card" onSubmit={submit}>
          <fieldset className="pay-block">
            <legend>1 · Ton abonnement</legend>
            {wantsShortPeriod && (
              <div className="pay-hint" style={{ marginBottom: 12 }}>
                Le paiement en ligne active un abonnement <b>annuel</b> (le plus avantageux).
                Pour payer <b>au mois ou au trimestre</b>,{" "}
                <a href={whatsappPayUrl(PLANS[plan].name)} target="_blank" rel="noreferrer">
                  écris-nous sur WhatsApp
                </a>
                .
              </div>
            )}
            <div className="pay-radio-group">
              {Object.entries(PLANS).map(([key, p]) => (
                <button
                  key={key}
                  type="button"
                  className={`pay-option ${plan === key ? "active" : ""}`}
                  onClick={() => setPlan(key)}
                >
                  <span className="pay-option-name">{p.name}</span>
                  <span className="pay-option-amount">{formatFCFA(p.amount)}</span>
                  <span className="pay-option-desc">{p.desc}</span>
                </button>
              ))}
            </div>
          </fieldset>

          <fieldset className="pay-block">
            <legend>2 · Moyen de paiement</legend>
            <div className="pay-radio-group two">
              <button
                type="button"
                className={`pay-option ${method === "mobile_money" ? "active" : ""}`}
                onClick={() => setMethod("mobile_money")}
              >
                <Smartphone size={18} className="pay-option-ico" />
                <span className="pay-option-name">Mobile money</span>
              </button>
              <button
                type="button"
                className={`pay-option ${method === "card" ? "active" : ""}`}
                onClick={() => setMethod("card")}
              >
                <CreditCard size={18} className="pay-option-ico" />
                <span className="pay-option-name">Carte bancaire</span>
              </button>
            </div>

            {method === "mobile_money" && (
              <label className="pay-field">
                <span>Pays</span>
                <select className="pay-input" value={country} onChange={(e) => setCountry(e.target.value)}>
                  {COUNTRIES.map(([code, label]) => (
                    <option key={code} value={code}>
                      {label}
                    </option>
                  ))}
                </select>
              </label>
            )}
          </fieldset>

          <fieldset className="pay-block">
            <legend>3 · Tes informations</legend>
            <label className="pay-field">
              <span>Nom complet</span>
              <input
                className="pay-input"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Aïcha Moussa"
                autoComplete="name"
              />
            </label>

            <label className="pay-field">
              <span>Email de ton compte Lissafi</span>
              <input
                className="pay-input"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="toi@exemple.com"
                autoComplete="email"
              />
              <small className="pay-hint">
                Celui que tu utilises dans l'app. C'est lui qui sera activé.
              </small>
            </label>

            <label className="pay-field">
              <span>{method === "mobile_money" ? "Numéro de téléphone" : "Téléphone (contact)"}</span>
              <input
                className="pay-input"
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                placeholder={method === "mobile_money" ? "+227 90 00 00 00" : "+227 90 00 00 00"}
                inputMode="tel"
                autoComplete="tel"
              />
              {isNigerMobile && (
                <small className="pay-hint">
                  Le numéro mobile money (Airtel, ZamaniCash ou Moov) qui recevra la
                  demande de confirmation.
                </small>
              )}
            </label>
          </fieldset>

          {error && <div className="pay-error">{error}</div>}

          <button className="btn btn-primary btn-lg pay-submit" type="submit" disabled={phase === "waiting"}>
            {phase === "waiting" ? (
              <>
                <Loader2 size={18} className="spin" /> En attente de confirmation…
              </>
            ) : (
              `Payer ${formatFCFA(PLANS[plan].amount)}`
            )}
          </button>

          {phase === "waiting" && (
            <div className="pay-waiting">
              <Loader2 size={26} className="spin" />
              <p>
                Valide la demande sur ton téléphone
                {isNigerMobile ? " (message de ton opérateur mobile money)." : "."}
              </p>
              {pollState && (
                <span className="pay-poll-state">Statut : {pollState}</span>
              )}
            </div>
          )}

          <p className="pay-secure">
            Paiement traité par {isNigerMobile ? "iPayMoney" : "GeniusPay"}. Tes données
            sont transmises en HTTPS.
          </p>

          <p className="pay-secure">
            Pas de carte ni de mobile money, ou tu préfères payer en espèces ?{" "}
            <a href={whatsappPayUrl(PLANS[plan].name)} target="_blank" rel="noreferrer">
              Active ton compte via WhatsApp
            </a>
            .
          </p>
        </form>
      </main>
    </div>
  );
}

function Result({ kind, plan, activated = true, onRetry }) {
  const isOk = kind === "success";
  return (
    <div className="pay-shell">
      <header className="pay-header">
        <a className="pay-back" href="/">
          <ArrowLeft size={16} /> Retour au site
        </a>
      </header>
      <main className="pay-main">
        <div className="pay-result">
          {isOk ? (
            <CheckCircle2 size={52} className="pay-result-ico ok" />
          ) : (
            <XCircle size={52} className="pay-result-ico bad" />
          )}
          <h1>{isOk ? "Paiement confirmé 🎉" : "Paiement échoué"}</h1>
          <p>
            {isOk
              ? activated
                ? `Ton compte ${plan === "business" ? "Commerce / Supermarché" : "Petite boutique"} est activé pour 1 an. Ouvre l'app Lissafi pour en profiter.`
                : "Paiement reçu, mais aucun compte Lissafi ne correspond à cet email. Vérifie l'email saisi puis contacte-nous sur WhatsApp avec ta preuve de paiement."
              : "Le paiement n'a pas abouti. Tu peux réessayer ou nous contacter sur WhatsApp."}
          </p>
          {isOk ? (
            <a className="btn btn-primary btn-lg" href="/#installation">
              Télécharger l'app
            </a>
          ) : (
            <button className="btn btn-ghost btn-lg" onClick={onRetry}>
              Réessayer
            </button>
          )}
        </div>
      </main>
    </div>
  );
}
