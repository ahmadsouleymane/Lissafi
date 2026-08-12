import { motion } from "motion/react";
import { APK } from "../config";
import { Kicker } from "./ui";

export default function Hero() {
  return (
    <section className="hero on-ink" id="top">
      <div className="container hero-grid">
        <div>
          <motion.div initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }}>
            <Kicker>Caisse enregistreuse mobile</Kicker>
          </motion.div>

          <motion.h1
            initial={{ opacity: 0, y: 26 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.08 }}
          >
            Le cahier, <span className="accent">c'est fini.</span>
          </motion.h1>

          <motion.p
            className="hero-sub"
            initial={{ opacity: 0, y: 22 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.18 }}
          >
            Toute ta boutique dans ton téléphone : la caisse, la monnaie, les crédits clients,
            le stock. Ça marche même sans réseau.
          </motion.p>

          <motion.div
            className="hero-cta"
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.7, delay: 0.28 }}
          >
            <a className="btn btn-primary" href={APK} download>
              ⬇ Télécharger l'APK
            </a>
            <a className="btn btn-ghost" href="#demo">
              Voir la démo
            </a>
          </motion.div>

          <motion.div
            className="hero-meta"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.8, delay: 0.45 }}
          >
            <span><b>Android uniquement</b></span>
            <span><b>Gratuit</b></span>
          </motion.div>
        </div>

        <motion.div
          className="hero-shot-wrap"
          initial={{ opacity: 0, y: 34 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.85, delay: 0.3, ease: [0.22, 1, 0.36, 1] }}
        >
          <img className="hero-shot" src="/assets/screens/caisse.png" alt="Écran de caisse de l'application Lissafi" />
        </motion.div>
      </div>
    </section>
  );
}
