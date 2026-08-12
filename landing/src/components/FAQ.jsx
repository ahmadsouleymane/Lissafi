import { useState } from "react";
import { motion, AnimatePresence } from "motion/react";
import { Plus } from "lucide-react";
import { Kicker, Reveal } from "./ui";

const ITEMS = [
  {
    q: "Ça marche vraiment sans réseau ?",
    a: "Oui. Les ventes s'enregistrent directement sur ton téléphone. Quand le réseau revient, tout se synchronise tout seul en arrière-plan.",
  },
  {
    q: "C'est quoi la limite de la version gratuite ?",
    a: "10 produits et 10 clients. Les ventes et l'historique restent illimités. C'est fait pour que tu testes en conditions réelles. Essentiel ou Premium débloquent tout, sans limite.",
  },
  {
    q: "Comment je paie Essentiel ou Premium ?",
    a: "En espèces ou par mobile money. Dès réception, on t'active ton offre pour 365 jours. Simple, sans abonnement qui se renouvelle tout seul.",
  },
  {
    q: "Sur quel téléphone ça marche ?",
    a: "Android, même les plus simples (Tecno, Infinix, Itel, Samsung — dès 2 Go de RAM). iPhone : pas encore.",
  },
];

function FaqItem({ q, a, open, onToggle }) {
  return (
    <div className={`faq-item ${open ? "open" : ""}`}>
      <button className="faq-q" onClick={onToggle} aria-expanded={open}>
        {q}
        <Plus className="ico" size={18} />
      </button>
      <AnimatePresence initial={false}>
        {open && (
          <motion.div
            className="faq-a"
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3, ease: [0.22, 1, 0.36, 1] }}
          >
            <p>{a}</p>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

export default function FAQ() {
  const [open, setOpen] = useState(0);
  return (
    <section className="section" id="faq">
      <div className="container" style={{ maxWidth: 760 }}>
        <Reveal>
          <Kicker>Les questions</Kicker>
          <h2>Ce que tout le monde demande.</h2>
        </Reveal>
        <div className="faq-list">
          {ITEMS.map((it, i) => (
            <FaqItem
              key={it.q}
              {...it}
              open={open === i}
              onToggle={() => setOpen(open === i ? -1 : i)}
            />
          ))}
        </div>
      </div>
    </section>
  );
}
