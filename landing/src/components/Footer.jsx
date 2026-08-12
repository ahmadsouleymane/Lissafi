import { MessageCircle } from "lucide-react";

// Numéro WhatsApp d'activation (même numéro que dans l'app — PremiumManager.kt)
const WHATSAPP_URL = "https://wa.me/2250160726314";

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
        <div className="footer-meta">© 2026 Lissafi</div>
      </div>
    </footer>
  );
}
