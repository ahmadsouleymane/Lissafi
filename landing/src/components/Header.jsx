import { useEffect, useState } from "react";
import { APK } from "../config";

export default function Header() {
  const [solid, setSolid] = useState(false);

  useEffect(() => {
    const hero = document.getElementById("hero");
    if (!hero) return;
    const obs = new IntersectionObserver(
      ([entry]) => setSolid(!entry.isIntersecting)
    );
    obs.observe(hero);
    return () => obs.disconnect();
  }, []);

  return (
    <header className={`header ${solid ? "solid" : "over-hero"}`}>
      <div className="container header-inner">
        <a className="brand" href="#hero" aria-label="Lissafi — retour en haut">
          <img
            className="brand-logo"
            src={solid ? "/assets/logo-header.svg" : "/assets/logo-white.svg"}
            alt="Lissafi"
          />
        </a>
        <nav className="header-nav" aria-label="Navigation principale">
          <a href="#fonctionnalites">Fonctionnalités</a>
          <a href="#demo">La démo</a>
          <a href="#tarifs">Tarifs</a>
          <a href="#faq">FAQ</a>
        </nav>
        <div className="header-actions">
          <a
            className="btn btn-ghost header-cta hide-sm"
            href="#installation"
          >
            Comment installer ?
          </a>
          <a className="btn btn-primary header-cta" href={APK} download>
            Télécharger l'APK
          </a>
        </div>
      </div>
    </header>
  );
}
