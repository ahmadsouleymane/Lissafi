import { WHATSAPP } from "../config";

export default function Footer() {
  return (
    <footer className="footer">
      <div className="container footer-inner">
        <div className="footer-brand">
          <img src="/assets/logo-master.png" alt="Logo Lissafi" />
          <span>Lissafi</span>
        </div>
        <div className="footer-meta">Le cahier, c'est fini · Android</div>
        <div className="footer-meta">
          <a href={WHATSAPP} target="_blank" rel="noreferrer" style={{ color: "var(--green-deep)", fontWeight: 700 }}>
            WhatsApp
          </a>{" "}
          · © 2026 Lissafi
        </div>
      </div>
    </footer>
  );
}
