import { WHATSAPP } from "../config";

export default function Footer() {
  return (
    <footer className="footer">
      <div className="container footer-inner">
        <div className="footer-brand">
          <img src="/assets/logo-header.svg" alt="Lissafi" className="footer-logo" />
        </div>
        <div className="footer-meta">La caisse de ta boutique, dans ta poche · Android</div>
        <div className="footer-meta">
          <a href={WHATSAPP} target="_blank" rel="noreferrer" className="link-primary">
            WhatsApp
          </a>{" "}
          · © 2026 Lissafi
        </div>
      </div>
    </footer>
  );
}
