import { MessageCircle, Handshake } from "lucide-react";
import { WHATSAPP_URL, BECOME_PARTNER_URL } from "../config";

export default function Footer() {
  return (
    <footer className="footer">
      <div className="container footer-inner">
        <img
          src="/assets/logo-white.svg"
          alt="Lissafi"
          className="footer-logo"
        />
        <div className="footer-meta">
          La caisse de ta boutique, dans ta poche
        </div>
        <a
          className="footer-link"
          href={WHATSAPP_URL}
          target="_blank"
          rel="noopener noreferrer"
        >
          <MessageCircle size={15} /> WhatsApp : +225 0160 72 63 14
        </a>
        <a
          className="footer-partner"
          href={BECOME_PARTNER_URL}
          target="_blank"
          rel="noopener noreferrer"
        >
          <Handshake size={15} /> Devenir partenaire
        </a>
        <div className="footer-meta">© 2026 Lissafi</div>
      </div>
    </footer>
  );
}
