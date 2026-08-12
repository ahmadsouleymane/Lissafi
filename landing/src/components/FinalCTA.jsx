import { motion } from "motion/react";
import { WHATSAPP, APK } from "../config";

export default function FinalCTA() {
  return (
    <section className="section final" id="commencer">
      <div className="container">
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
          Gratuit pour démarrer. L'installation prend deux minutes, et ta première
          vente peut être encaissée dans la foulée.
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
            Parler à quelqu'un sur WhatsApp
          </a>
        </motion.div>
      </div>
    </section>
  );
}
