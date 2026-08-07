import { WHATSAPP, APK } from "../config";
import { Kicker, Reveal } from "./ui";

const STEPS = [
  {
    n: "1",
    t: "Autoriser les sources inconnues",
    d: "Réglages → Sécurité → « Installer des applis inconnues ». Un réglage, une seule fois.",
  },
  {
    n: "2",
    t: "Installer l'APK",
    d: "Télécharge le fichier, ouvre-le, confirme. 2 minutes, c'est tout.",
  },
  {
    n: "3",
    t: "Créer ton compte",
    d: "Ton nom, ta boutique, tes premiers produits. Prêt pour la première vente.",
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

        <Reveal delay={0.18}>
          <p style={{ marginTop: 28, color: "var(--ink-soft)", fontSize: 15 }}>
            Un téléphone qui bloque ? Écris-moi sur{" "}
            <a
              href={WHATSAPP}
              target="_blank"
              rel="noreferrer"
              style={{ color: "var(--green-deep)", fontWeight: 600 }}
            >
              WhatsApp
            </a>{" "}
            et je t'accompagne jusqu'au bout. Le fichier APK :{" "}
            <a href={APK} download style={{ color: "var(--green-deep)", fontWeight: 600 }}>
              lissafi.apk
            </a>
            .
          </p>
        </Reveal>
      </div>
    </section>
  );
}
