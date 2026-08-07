import { motion } from "motion/react";
import { WHATSAPP, APK } from "../config";
import { Kicker } from "./ui";

const RECEIPT_LINES = [
  ["Riz parfumé 25kg", "10 500"],
  ["Huile végétale 1L", "1 000"],
  ["Sucre 1kg", "700"],
];
const TOTAL = "12 200";
const CHANGE = "2 800";

export default function Hero() {
  return (
    <section className="hero" id="top">
      <div className="hero-bg-barcode" aria-hidden="true" />
      <div className="container hero-grid">
        <div>
          <motion.div initial={{ opacity: 0, y: 18 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }}>
            <Kicker>N° 001 — Caisse enregistreuse mobile · Android</Kicker>
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
            le stock. Ça marche même sans réseau. Gratuit pour tester.
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
              Voir la caisse
            </a>
            <a className="btn btn-whatsapp" href={WHATSAPP} target="_blank" rel="noreferrer">
              WhatsApp
            </a>
          </motion.div>

          <motion.div
            className="hero-meta"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.8, delay: 0.45 }}
          >
            <span><b>Android uniquement</b> · APK direct</span>
            <span><b>Gratuit</b> pour tester</span>
            <span>Premium <b>15 000 F/an</b> = 41 F/jour</span>
          </motion.div>
        </div>

        {/* Le reçu qui s'imprime */}
        <motion.div
          className="receipt-wrap"
          initial={{ opacity: 0, y: 34, rotateX: 14 }}
          animate={{ opacity: 1, y: 0, rotateX: 0 }}
          transition={{ duration: 0.85, delay: 0.3, ease: [0.22, 1, 0.36, 1] }}
        >
          <div className="scan-beam" aria-hidden="true" />
          <div className="receipt" role="img" aria-label="Exemple de ticket de caisse Lissafi">
            <div className="receipt-head">
              <div className="shop">Boutique Albaraka</div>
              <div>Reçu N° 0047 · 05/08/2026</div>
            </div>
            {RECEIPT_LINES.map(([name, price], i) => (
              <motion.div
                className="receipt-line"
                key={name}
                initial={{ opacity: 0, y: 6 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.7 + i * 0.22, duration: 0.3 }}
              >
                <span>{name}</span>
                <span className="mono-price">{price} F</span>
              </motion.div>
            ))}
            <motion.div
              className="receipt-total"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 1.45 }}
            >
              <span>TOTAL</span>
              <span>{TOTAL} F</span>
            </motion.div>
            <motion.div
              className="receipt-change"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 1.7 }}
            >
              <span>MONNAIE À RENDRE</span>
              <span>{CHANGE} F</span>
            </motion.div>
            <motion.div className="receipt-barcode" initial={{ opacity: 0 }} animate={{ opacity: 1 }} transition={{ delay: 1.9 }} />
            <motion.div
              className="receipt-ref"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 2.1 }}
            >
              Lissafi — ton commerce, maîtrisé
            </motion.div>
          </div>
        </motion.div>
      </div>
    </section>
  );
}
