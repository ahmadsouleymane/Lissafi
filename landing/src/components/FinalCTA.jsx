import { motion } from "motion/react";
import { Download } from "lucide-react";
import { APK } from "../config";

export default function FinalCTA() {
  return (
    <section className="final on-ink" id="commencer">
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
          Gratuit pour tester. Deux minutes pour installer. Et ta première vente
          peut être encaissée dans la foulée.
        </motion.p>

        <motion.div
          className="final-cta"
          initial={{ opacity: 0, y: 18 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6, delay: 0.2 }}
        >
          <a className="btn btn-primary btn-lg" href={APK} download>
            <Download size={18} /> Télécharger l'APK
          </a>
          <a className="btn btn-ghost btn-lg" href="#installation">
            Comment installer ?
          </a>
        </motion.div>
      </div>
    </section>
  );
}
