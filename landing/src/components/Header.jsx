import { APK } from "../config";
import ThemeToggle from "./ThemeToggle";

export default function Header() {
  return (
    <header className="header">
      <div className="container header-inner">
        <a className="brand" href="#top" aria-label="Lissafi — retour en haut">
          <img src="/assets/logo-header.svg" alt="Lissafi" />
        </a>
        <nav className="header-nav" aria-label="Navigation">
          <a href="#probleme">Le problème</a>
          <a href="#demo">La caisse</a>
          <a href="#tarifs">Tarifs</a>
          <a href="#faq">FAQ</a>
        </nav>
        <div className="header-actions">
          <ThemeToggle />
          <a className="btn btn-primary header-cta" href={APK} download>
            Télécharger
          </a>
        </div>
      </div>
    </header>
  );
}
