import { useEffect, useRef, useState } from "react";
import { MessageCircle, X } from "lucide-react";
import { getPartnerCode, recordVisit, savePartnerCode, whatsappUrl } from "../lib/tracking";

// Bannière affichée quand un visiteur arrive via un lien partenaire (?p=CODE).
// Enregistre la visite et propose le CTA WhatsApp d'activation pré-rempli.
export default function PartnerBanner() {
  const [code, setCode] = useState("");
  const [dismissed, setDismissed] = useState(false);
  const bannerRef = useRef(null);

  useEffect(() => {
    const c = getPartnerCode();
    if (c) {
      setCode(c);
      savePartnerCode(c);
      recordVisit(c);
    }
  }, []);

  // La bannière est `position: fixed` au sommet. On mesure sa hauteur et on la
  // stocke dans --banner-h (via la classe `has-partner-banner` sur <html>) pour
  // que le CSS pousse le header et la hero en dessous — évite la superposition.
  useEffect(() => {
    if (!code || dismissed) return;
    const el = bannerRef.current;
    if (!el) return;
    const root = document.documentElement;
    root.classList.add("has-partner-banner");
    const update = () => root.style.setProperty("--banner-h", `${el.offsetHeight}px`);
    update();
    const cleanup = () => {
      root.classList.remove("has-partner-banner");
      root.style.removeProperty("--banner-h");
    };
    if (typeof ResizeObserver !== "undefined") {
      const ro = new ResizeObserver(update);
      ro.observe(el);
      return () => {
        ro.disconnect();
        cleanup();
      };
    }
    return cleanup;
  }, [code, dismissed]);

  if (!code || dismissed) return null;

  return (
    <div className="partner-banner" ref={bannerRef}>
      <div className="container partner-banner-inner">
        <p className="partner-banner-text">
          Tu viens de la part de <strong>{code}</strong>. Ton téléchargement est lié à ce code —
          tu n'as rien à faire. Ou active directement par WhatsApp.
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
