import { useEffect, useState } from "react";
import { Menu, X } from "lucide-react";
import { APK } from "../config";

const NAV = [
  ["#fonctionnalites", "Fonctionnalités"],
  ["#demo", "La démo"],
  ["#tarifs", "Tarifs"],
  ["#partenaires", "Partenaires"],
  ["#faq", "FAQ"],
  ["#installation", "Installer"],
];

export default function Header() {
  const [solid, setSolid] = useState(false);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const hero = document.getElementById("hero");
    if (!hero) return;
    const obs = new IntersectionObserver(([entry]) => setSolid(!entry.isIntersecting));
    obs.observe(hero);
    return () => obs.disconnect();
  }, []);

  return (
    <header className={`header ${solid ? "solid" : "over-hero"}`}>
      <div className="container header-inner">
        <a className="brand" href="#hero" aria-label="Lissafi · retour en haut" onClick={() => setOpen(false)}>
          <img
            className="brand-logo"
            src={solid ? "/assets/logo-header.svg" : "/assets/logo-white.svg"}
            alt="Lissafi"
          />
        </a>

        <nav className="header-nav" aria-label="Navigation principale">
          {NAV.slice(0, 4).map(([href, label]) => (
            <a key={href} href={href}>
              {label}
            </a>
          ))}
        </nav>

        <div className="header-actions">
          <a className="btn btn-ghost header-cta hide-sm" href="#installation">
            Comment installer ?
          </a>
          <a className="btn btn-primary header-cta" href={APK} download>
            Télécharger l'APK
          </a>
          <button
            className="header-menu-btn"
            onClick={() => setOpen(!open)}
            aria-label="Menu"
            aria-expanded={open}
          >
            {open ? <X size={22} /> : <Menu size={22} />}
          </button>
        </div>
      </div>

      {open && (
        <div className="mobile-menu">
          <nav className="mobile-menu-nav" aria-label="Navigation mobile">
            {NAV.map(([href, label]) => (
              <a key={href} href={href} onClick={() => setOpen(false)}>
                {label}
              </a>
            ))}
          </nav>
          <a className="btn btn-primary mobile-menu-cta" href={APK} download onClick={() => setOpen(false)}>
            Télécharger l'APK
          </a>
        </div>
      )}
    </header>
  );
}
