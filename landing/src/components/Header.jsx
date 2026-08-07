import { WHATSAPP } from "../config";

export default function Header() {
  return (
    <header className="header">
      <div className="container header-inner">
        <a className="brand" href="#top" aria-label="Lissafi — retour en haut">
          <img src="/assets/logo-header.svg" alt="Lissafi" />
          <span className="brand-name">Lissafi</span>
        </a>
        <nav className="header-nav" aria-label="Navigation">
          <a href="#probleme">01 · Le cahier</a>
          <a href="#demo">02 · La caisse</a>
          <a href="#tarifs">03 · Tarifs</a>
          <a href="#faq">04 · FAQ</a>
        </nav>
        <a className="btn btn-whatsapp header-cta" href={WHATSAPP} target="_blank" rel="noreferrer">
          WhatsApp
        </a>
      </div>
    </header>
  );
}
