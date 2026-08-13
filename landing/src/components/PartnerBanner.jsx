import { useEffect, useState } from "react";
import { MessageCircle, X } from "lucide-react";
import { getPartnerCode, recordVisit, whatsappUrl } from "../lib/tracking";

// Bannière affichée quand un visiteur arrive via un lien partenaire (?p=CODE).
// Enregistre la visite et propose le CTA WhatsApp d'activation pré-rempli.
export default function PartnerBanner() {
  const [code, setCode] = useState("");
  const [dismissed, setDismissed] = useState(false);

  useEffect(() => {
    const c = getPartnerCode();
    if (c) {
      setCode(c);
      recordVisit(c);
    }
  }, []);

  if (!code || dismissed) return null;

  return (
    <div className="partner-banner">
      <div className="container partner-banner-inner">
        <p className="partner-banner-text">
          Tu viens de la part de <strong>{code}</strong>. Active Lissafi par WhatsApp — c'est plus rapide.
        </p>
        <div className="partner-banner-actions">
          <a className="btn btn-primary btn-sm" href={whatsappUrl(code)} target="_blank" rel="noopener noreferrer">
            <MessageCircle size={16} /> Activer sur WhatsApp
          </a>
          <button className="partner-banner-close" onClick={() => setDismissed(true)} aria-label="Fermer">
            <X size={18} />
          </button>
        </div>
      </div>
    </div>
  );
}
