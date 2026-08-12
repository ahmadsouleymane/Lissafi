import { useState, useEffect, useRef } from "react";
import { AnimatePresence, motion } from "motion/react";
import { Kicker, Reveal } from "./ui";

const FEATURES = [
  {
    id: "caisse",
    num: "01",
    title: "La caisse dans la poche",
    desc: "Touche un produit : le panier se remplit, le total se calcule, la monnaie se rend. Zéro erreur dans la foule.",
    img: "/assets/screens/caisse.png",
  },
  {
    id: "produits",
    num: "02",
    title: "Le stock à l'unité près",
    desc: "L'alerte sonne avant la rupture. Tu commandes avec des chiffres, pas des souvenirs.",
    img: "/assets/screens/produits.png",
  },
  {
    id: "clients",
    num: "03",
    title: "Les crédits qui se rappellent",
    desc: "Le nom, le montant, la date. Plus jamais de « je paie demain » oublié.",
    img: "/assets/screens/clients.png",
  },
  {
    id: "recu",
    num: "04",
    title: "La preuve par WhatsApp",
    desc: "Le ticket part directement sur le téléphone du client. La confiance, la fin des disputes.",
    img: "/assets/screens/recu.png",
  },
  {
    id: "rapports",
    num: "05",
    title: "Le soir, tout est là",
    desc: "Ton chiffre, tes ventes, tes crédits. Sans compter tes billets un par un.",
    img: "/assets/screens/rapports.png",
  },
];

export default function PhoneDemo() {
  const [active, setActive] = useState(0);
  const refs = useRef([]);

  useEffect(() => {
    const obs = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) setActive(Number(e.target.dataset.i));
        });
      },
      { rootMargin: "-42% 0px -42% 0px" }
    );
    refs.current.forEach((r) => r && obs.observe(r));
    return () => obs.disconnect();
  }, []);

  return (
    <section className="section demo" id="demo">
      <div className="container">
        <Reveal>
          <Kicker>La démo</Kicker>
          <h2>La caisse, en vrai. Scrolle, regarde.</h2>
        </Reveal>

        <div className="demo-grid" style={{ marginTop: 44 }}>
          <div className="phone-sticky">
            <div className="phone">
              <div className="phone-notch" aria-hidden="true" />
              <div className="phone-screen">
                <AnimatePresence mode="wait">
                  <motion.img
                    key={FEATURES[active].id}
                    className="phone-demo-media"
                    src={FEATURES[active].img}
                    alt={`Écran ${FEATURES[active].title} de l'application Lissafi`}
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 0.28 }}
                  />
                </AnimatePresence>
              </div>
            </div>
          </div>

          <div className="feature-blocks">
            {FEATURES.map((f, i) => (
              <div
                key={f.id}
                className={`feature ${active === i ? "active" : ""}`}
                data-i={i}
                ref={(el) => (refs.current[i] = el)}
              >
                <span className="num">{f.num}</span>
                <h3>{f.title}</h3>
                <p>{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
