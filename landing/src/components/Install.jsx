import { copyPartnerCodeForDownload, downloadUrl } from "../lib/tracking";
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
    <section className="section rail" id="installation">
      <div className="container">
        <Reveal className="section-head center">
          <Kicker>L'installation</Kicker>
          <h2>Trois étapes, deux minutes.</h2>
          <p>Et ta boutique est dans ton téléphone, prête à encaisser.</p>
        </Reveal>

        <div className="install-grid">
          {STEPS.map((s, i) => (
            <Reveal key={s.n} delay={i * 0.08} className="install-step">
              <div className="step-num">{s.n}</div>
              <h3>{s.t}</h3>
              <p>{s.d}</p>
            </Reveal>
          ))}
        </div>

        <Reveal delay={0.15}>
          <p className="install-help">
            Un téléphone qui bloque ? Relis l'étape 1, chaque marque nomme ce
            réglage un peu différemment. Le fichier à installer :{" "}
            <a href={downloadUrl()} download className="link-primary" onClick={() => copyPartnerCodeForDownload()}>
              lissafi.apk
            </a>
            .
          </p>
        </Reveal>
      </div>
    </section>
  );
}
