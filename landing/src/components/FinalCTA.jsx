import { motion } from "motion/react";
import { WHATSAPP, APK } from "../config";

const LINES = [
  ["Riz parfumé 25kg", "10 500"],
  ["Huile végétale 1L", "1 000"],
];

export default function FinalCTA() {
  return (
    <section className="section final" id="commencer">
      <div className="hero-bg-barcode" aria-hidden="true" />
      <div className="container">
        <div className="receipt" style={{ margin: "0 auto 44px" }}>
          <div className="receipt-head">
            <div className="shop">Ta boutique</div>
            <div>Reçu N° 0001 · aujourd'hui</div>
          </div>
          {LINES.map(([n, p]) => (
            <div className="receipt-line" key={n}>
              <span>{n}</span>
              <span className="mono-price">{p} F</span>
            </div>
          ))}
          <div className="receipt-total">
            <span>TOTAL</span>
            <span>—</span>
          </div>
          <motion.div
            className="receipt-barcode"
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            transition={{ delay: 0.4 }}
          />
          <div className="receipt-ref">La première vente, c'est toi.</div>
        </div>

        <motion.h2
          initial={{ opacity: 0, y: 24 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-80px" }}
          transition={{ duration: 0.6 }}
        >
          Ta boutique mérite mieux qu'un cahier.
        </motion.h2>

        <motion.p
          className="final-sub"
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, delay: 0.1 }}
        >
          Gratuit pour tester. Premium à 15 000 F/an pour tout débloquer.
          L'installation prend 2 minutes.
        </motion.p>

        <motion.div
          className="final-cta"
          initial={{ opacity: 0, y: 18 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, delay: 0.2 }}
        >
          <a className="btn btn-primary" href={APK} download>
            ⬇ Télécharger l'APK
          </a>
          <a className="btn btn-whatsapp" href={WHATSAPP} target="_blank" rel="noreferrer">
            Je veux une démo sur WhatsApp
          </a>
        </motion.div>
      </div>
    </section>
  );
}
