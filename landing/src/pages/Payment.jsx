import { useState } from "react";
import { ArrowLeft, Check, CreditCard, Loader2, MessageCircle, Smartphone } from "lucide-react";
import { initiatePayment } from "../lib/payments";
import { formatFCFA } from "../lib/format";
import { WHATSAPP_NUMBER } from "../config";

// ============================================================
// Page de paiement en ligne (/payer). Hors de l'app, sur le même
// site que la landing. Aucune clé de paiement ici : on appelle le
// back-office, qui traite carte ET mobile money via GeniusPay
// (checkout hébergé). Sans carte ni mobile money : repli WhatsApp.
// ============================================================

const PLANS = {
  plus: {
    name: "Petite boutique",
    amount: 24000,
    features: [
      "Jusqu'à 200 produits",
      "Clients & crédits illimités",
      "Ventes illimitées",
      "Ticket WhatsApp & impression",
    ],
  },
  business: {
    name: "Commerce / Supermarché",
    amount: 50000,
    features: [
      "Tout illimité",
      "Multi-caisses (chiffre global)",
      "Plusieurs utilisateurs",
      "Export CSV · support prioritaire",
    ],
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
  const params = new URLSearchParams(window.location.search);
  const [plan, setPlan] = useState(params.get("plan") === "business" ? "business" : "plus");
  const period = params.get("period") || "yearly";
  const wantsShortPeriod = period === "monthly" || period === "quarterly";

  const [method, setMethod] = useState("mobile_money");
  const [country, setCountry] = useState("NE");
  const [name, setName] = useState("");
  const [email, setEmail] = useState(() => params.get("email") || "");
  const [phone, setPhone] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const p = PLANS[plan];

  async function submit(e) {
    e.preventDefault();
    setError("");

    if (!name.trim() || !email.trim() || !phone.trim()) {
      setError("Renseigne ton nom, ton email et ton téléphone.");
      return;
    }
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email.trim())) {
      setError("Cet email ne semble pas valide.");
      return;
    }

    setLoading(true);
    try {
      const result = await initiatePayment({
        plan,
        method,
        country: method === "card" ? "" : country,
        name: name.trim(),
        email: email.trim(),
        phone: phone.trim(),
      });

      // Carte et mobile money passent par le checkout hébergé GeniusPay.
      if (result.mode === "redirect" && result.redirectUrl) {
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

      setError("Réponse de paiement inattendue. Réessaie ou active via WhatsApp.");
      setLoading(false);
    } catch (err) {
      setError(err.message || "Une erreur est survenue. Réessaie.");
      setLoading(false);
    }
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
          <h1>Active ton Lissafi en 1 minute.</h1>
          <p>
            Paiement sécurisé par carte bancaire ou mobile money. Ton compte est
            activé automatiquement pour 1 an.
          </p>
        </div>

        <div className="pay-layout">
          {/* ── Récapitulatif ── */}
          <aside className="pay-summary">
            <div className="pay-summary-label">Ton abonnement</div>
            <div className="pay-plan-switch">
              {Object.entries(PLANS).map(([key, pl]) => (
                <button
                  key={key}
                  type="button"
                  className={`pay-plan-chip ${plan === key ? "active" : ""}`}
                  onClick={() => setPlan(key)}
                >
                  {pl.name}
                </button>
              ))}
            </div>
            <div className="pay-summary-price">
              {formatFCFA(p.amount)}<span>/an</span>
            </div>
            <div className="pay-summary-note">Activé 1 an · sans renouvellement automatique</div>
            <ul className="pay-summary-features">
              {p.features.map((f) => (
                <li key={f}>
                  <Check size={15} /> {f}
                </li>
              ))}
            </ul>
            <div className="pay-alt">
              <MessageCircle size={16} />
              <span>
                Pas de carte ni de mobile money, ou tu préfères payer en espèces ?{" "}
                <a href={whatsappPayUrl(p.name)} target="_blank" rel="noreferrer">
                  Active via WhatsApp
                </a>
                .
              </span>
            </div>
          </aside>

          {/* ── Formulaire ── */}
          <form className="pay-card" onSubmit={submit}>
            {wantsShortPeriod && (
              <div className="pay-hint pay-period-note">
                Le paiement en ligne active un abonnement <b>annuel</b> (le plus avantageux).
                Pour payer <b>au mois ou au trimestre</b>,{" "}
                <a href={whatsappPayUrl(p.name)} target="_blank" rel="noreferrer">
                  écris-nous sur WhatsApp
                </a>
                .
              </div>
            )}

            <fieldset className="pay-block">
              <legend>Moyen de paiement</legend>
              <div className="pay-radio-group two">
                <button
                  type="button"
                  className={`pay-option ${method === "mobile_money" ? "active" : ""}`}
                  onClick={() => setMethod("mobile_money")}
                >
                  <Smartphone size={20} className="pay-option-ico" />
                  <span className="pay-option-name">Mobile money</span>
                  <span className="pay-option-desc">Airtel, Moov, MTN…</span>
                </button>
                <button
                  type="button"
                  className={`pay-option ${method === "card" ? "active" : ""}`}
                  onClick={() => setMethod("card")}
                >
                  <CreditCard size={20} className="pay-option-ico" />
                  <span className="pay-option-name">Carte bancaire</span>
                  <span className="pay-option-desc">Visa, Mastercard</span>
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
              <legend>Tes informations</legend>
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
                <span>{method === "mobile_money" ? "Numéro mobile money" : "Téléphone (contact)"}</span>
                <input
                  className="pay-input"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  placeholder="+227 90 00 00 00"
                  inputMode="tel"
                  autoComplete="tel"
                />
              </label>
            </fieldset>

            {error && <div className="pay-error">{error}</div>}

            <button className="btn btn-primary btn-lg pay-submit" type="submit" disabled={loading}>
              {loading ? (
                <>
                  <Loader2 size={18} className="spin" /> Redirection…
                </>
              ) : (
                `Payer ${formatFCFA(p.amount)}`
              )}
            </button>

            <p className="pay-secure">
              Paiement sécurisé par GeniusPay · HTTPS. Tu seras redirigé pour finaliser,
              puis ton compte s'active automatiquement.
            </p>
          </form>
        </div>
      </main>
    </div>
  );
}
