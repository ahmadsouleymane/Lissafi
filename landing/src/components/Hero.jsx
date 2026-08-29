import { motion } from "motion/react";
import { Download } from "lucide-react";
import { copyPartnerCodeForDownload, downloadUrl } from "../lib/tracking";

export default function Hero() {
  return (
    <section className="hero on-ink" id="hero">
      <div className="container hero-grid">
        <div className="hero-copy">
          <motion.span
            className="hero-badge"
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
          > Caisse enregistreuse pour ton téléphone
          </motion.span>

          <motion.h1
            className="hero-title"
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.08 }}
          >
            Le cahier, <span className="accent">c'est fini.</span>
          </motion.h1>

          <motion.p
            className="hero-sub"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.18 }}
          >
            La caisse, les crédits clients, le stock et les rapports du soir
            dans une seule app Android. Ça marche même sans réseau.
          </motion.p>

          <motion.div
            className="hero-cta"
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.28 }}
          >
            <a className="btn btn-primary btn-lg" href={downloadUrl()} download onClick={() => copyPartnerCodeForDownload()}>
              <Download size={18} /> Télécharger l'application
            </a>
            <a className="btn btn-ghost btn-lg" href="#demo">
              Voir la démo
            </a>
          </motion.div>

          <motion.p
            initial={{ opacity: 0, y: 12 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.4 }}
            style={{ marginTop: 16, fontSize: 14, color: "var(--text-secondary)" }}
          >
            14 jours d'essai gratuit · sans carte bancaire
          </motion.p>
        </div>

        <motion.div
          className="hero-shot-wrap"
          initial={{ opacity: 0, y: 34 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.9, delay: 0.3, ease: [0.22, 1, 0.36, 1] }}
        >
          <div className="hero-glow" aria-hidden="true" />
          <div className="hero-pill top">
            <span className="dot" /> Monnaie rendue : 1 750 F
          </div>
          <img
            className="hero-shot"
            src="/assets/screens/caisse.png"
            alt="Écran de caisse de l'application Lissafi"
          />
          <div className="hero-pill bottom">✓ Vente enregistrée · 10:37</div>
        </motion.div>
      </div>
    </section>
  );
}
