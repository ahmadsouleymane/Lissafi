import { WHATSAPP, APK, INSTALL_SCREENSHOT } from "../config";
import { Kicker, Reveal } from "./ui";

const STEPS = [
  {
    n: "1",
    t: "Autoriser les sources inconnues",
    d: "Réglages → Applications → Accès spécial → « Installer des applications inconnues ». Active-le pour Chrome (ou ton gestionnaire de fichiers). Un seul réglage, à faire une seule fois.",
  },
  {
    n: "2",
    t: "Télécharger et installer l'APK",
    d: "Appuie sur « Télécharger l'APK » sur ce site. Ouvre le fichier depuis tes téléchargements, appuie sur Installer, puis Ouvrir. 2 minutes, pas plus.",
  },
  {
    n: "3",
    t: "Créer ta boutique",
    d: "Ton nom, le nom de ta boutique, tes 3 premiers produits. L'app est prête : tu peux encaisser ta première vente tout de suite.",
  },
];

export default function Install() {
  return (
    <section className="section" id="installation">
      <div className="container">
        <Reveal>
          <Kicker>N° 005 — L'installation</Kicker>
          <h2>3 étapes, 2 minutes, et ta boutique est dans ton téléphone.</h2>
        </Reveal>

        <div className={`install-layout ${INSTALL_SCREENSHOT ? "has-shot" : ""}`}>
          <div className="install-grid">
            {STEPS.map((s, i) => (
              <Reveal key={s.n} delay={i * 0.08}>
                <div className="install-step">
                  <div className="step-num">{s.n}</div>
                  <h3>{s.t}</h3>
                  <p>{s.d}</p>
                </div>
              </Reveal>
            ))}
          </div>

          {INSTALL_SCREENSHOT && (
            <Reveal delay={0.15}>
              <div className="install-shot">
                <img src={INSTALL_SCREENSHOT} alt="Capture d'écran du processus d'installation Lissafi" />
              </div>
            </Reveal>
          )}
        </div>

        <Reveal delay={0.18}>
          <p className="install-help">
            Un téléphone qui bloque ? Écris-moi sur{" "}
            <a href={WHATSAPP} target="_blank" rel="noreferrer" className="link-primary">
              WhatsApp
            </a>{" "}
            et je t'accompagne jusqu'au bout. Le fichier APK :{" "}
            <a href={APK} download className="link-primary">
              lissafi.apk
            </a>
            .
          </p>
        </Reveal>
      </div>
    </section>
  );
}
