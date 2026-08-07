import { useState, useEffect, useRef } from "react";
import { motion, AnimatePresence } from "motion/react";
import { Kicker, Reveal } from "./ui";
import { formatFCFA } from "../lib/format";

/* ------------------------------------------------ */
/* Mini-écrans de l'app (construits en CSS, aucun mockup) */
/* ------------------------------------------------ */

function CaisseScreen() {
  const [cart, setCart] = useState([]);
  const [given, setGiven] = useState(null);
  const [printed, setPrinted] = useState(false);

  const PRODUCTS = [
    { n: "Riz parfumé 25kg", p: 10500 },
    { n: "Huile végétale 1L", p: 1000 },
    { n: "Sucre 1kg", p: 700 },
  ];

  const total = cart.reduce((s, c) => s + c.p * c.q, 0);
  const change = given != null ? given - total : null;

  const add = (prod) =>
    setCart((cs) => {
      const f = cs.find((c) => c.n === prod.n);
      if (f) return cs.map((c) => (c.n === prod.n ? { ...c, q: c.q + 1 } : c));
      return [...cs, { ...prod, q: 1 }];
    });

  const reset = () => {
    setPrinted(false);
    setGiven(null);
    setCart([]);
  };

  return (
    <div className="app-screen">
      <div className="app-topbar">
        <span className="t">Caisse</span>
        <span className="dot" />
      </div>
      <div className="app-body">
        {PRODUCTS.map((pr) => (
          <button key={pr.n} className="app-product" onClick={() => add(pr)}>
            <span className="name">{pr.n}</span>
            <span className="p">{formatFCFA(pr.p)}</span>
          </button>
        ))}

        <div className="app-cart">
          {cart.length === 0 ? (
            <div className="app-note" style={{ padding: "6px 0" }}>
              Touche un produit pour l'ajouter →
            </div>
          ) : (
            cart.map((c) => (
              <div className="row" key={c.n}>
                <span>{c.n} ×{c.q}</span>
                <span>{formatFCFA(c.p * c.q)}</span>
              </div>
            ))
          )}
          <div className="total">
            <span>TOTAL</span>
            <span>{formatFCFA(total)}</span>
          </div>
        </div>

        <div className="app-amounts">
          {[5000, 10000, 15000].map((a) => (
            <button key={a} className={given === a ? "on" : ""} onClick={() => setGiven(a)}>
              {a.toLocaleString("fr-FR").replace(/ /g, " ")} F
            </button>
          ))}
        </div>

        {given != null && total > 0 && (
          <div className="app-ok" style={{ padding: "9px 12px", borderRadius: 10, fontSize: 11 }}>
            MONNAIE À RENDRE : <b>{change >= 0 ? formatFCFA(change) : "insuffisant"}</b>
          </div>
        )}

        <button
          className="app-btn"
          disabled={cart.length === 0 || given == null}
          onClick={() => setPrinted(true)}
        >
          Encaisser
        </button>
      </div>

      {printed && (
        <div className="app-print">
          <div className="ticket">
            <div style={{ textAlign: "center", marginBottom: 6 }}>
              <b>BOUTIQUE ALBARAKA</b>
            </div>
            {cart.map((c) => (
              <div key={c.n} style={{ display: "flex", justifyContent: "space-between" }}>
                <span>{c.n} ×{c.q}</span>
                <span>{formatFCFA(c.p * c.q)}</span>
              </div>
            ))}
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                borderTop: "1px dashed #211f1a",
                marginTop: 6,
                paddingTop: 4,
              }}
            >
              <b>TOTAL</b>
              <b>{formatFCFA(total)}</b>
            </div>
            <div className="barcode" />
            <div style={{ textAlign: "center", fontSize: 9 }}>LISSAFI — MERCI !</div>
            <button onClick={reset}>Fermer</button>
          </div>
        </div>
      )}
    </div>
  );
}

function CreditsScreen() {
  return (
    <div className="app-screen">
      <div className="app-topbar">
        <span className="t">Crédits clients</span>
        <span className="dot" />
      </div>
      <div className="app-body">
        <div style={{ textAlign: "center", padding: "4px 0 10px" }}>
          <div style={{ fontSize: 22, fontWeight: 700, color: "var(--green-deep)" }}>47 500 F</div>
          <div style={{ fontSize: 9, color: "var(--ink-soft)" }}>TOTAL DÛ</div>
        </div>
        <div className="app-row">
          <span>Moussa Diallo</span>
          <span className="price">12 500 F</span>
        </div>
        <div className="app-row">
          <span>Mariama Issa</span>
          <span className="price">8 000 F</span>
        </div>
        <div className="app-row app-alert">
          <span>Oumarou Bako</span>
          <span className="badge">21+ jours</span>
        </div>
        <div className="app-row">
          <span>Rakia Ali</span>
          <span className="price">20 000 F</span>
        </div>
        <div className="app-note">+ Ajouter un crédit · barrer un paiement</div>
      </div>
    </div>
  );
}

function StockScreen() {
  return (
    <div className="app-screen">
      <div className="app-topbar">
        <span className="t">Stock</span>
        <span className="dot" />
      </div>
      <div className="app-body">
        <div className="app-row app-alert">
          <span>Jus de mangue</span>
          <span className="badge">3 restants</span>
        </div>
        <div className="app-row app-alert">
          <span>Pain (baguette)</span>
          <span className="badge">Rupture</span>
        </div>
        <div className="app-row app-ok">
          <span>Sachet d'eau (pack)</span>
          <span>✓ 120</span>
        </div>
        <div className="app-row">
          <span>Riz 25kg</span>
          <span>34</span>
        </div>
        <div className="app-note">L'alerte sonne avant la rupture</div>
      </div>
    </div>
  );
}

function HorsligneScreen() {
  return (
    <div className="app-screen">
      <div className="app-topbar">
        <span className="t">Caisse</span>
        <span className="dot" />
      </div>
      <div className="app-body">
        <div className="app-ok" style={{ textAlign: "center", padding: "10px", borderRadius: 10 }}>
          <b style={{ fontSize: 12 }}>✈️ Mode avion</b>
          <div style={{ fontSize: 10, marginTop: 2 }}>Vente enregistrée LOCALEMENT</div>
        </div>
        <div className="app-big-total">3 400 F</div>
        <div className="app-note">Le réseau revient → tout se synchronise</div>
        <div className="app-btn" style={{ opacity: 0.7, pointerEvents: "none" }}>
          Encaisser
        </div>
      </div>
    </div>
  );
}

function TicketScreen() {
  return (
    <div className="app-screen">
      <div className="app-topbar">
        <span className="t">Ticket</span>
        <span className="dot" />
      </div>
      <div className="app-body">
        <div className="app-cart">
          <div className="row">
            <span>Riz 25kg</span>
            <span>10 500 F</span>
          </div>
          <div className="row">
            <span>Sucre 1kg</span>
            <span>700 F</span>
          </div>
          <div className="total">
            <span>TOTAL</span>
            <span>11 200 F</span>
          </div>
        </div>
        <div className="app-ok" style={{ textAlign: "center", padding: "12px", borderRadius: 10 }}>
          <b style={{ fontSize: 12 }}>📲 Envoyer par WhatsApp</b>
          <div style={{ fontSize: 10, marginTop: 2 }}>La preuve part sur SON téléphone</div>
        </div>
      </div>
    </div>
  );
}

function SoirScreen() {
  return (
    <div className="app-screen">
      <div className="app-topbar">
        <span className="t">Rapport du jour</span>
        <span className="dot" />
      </div>
      <div className="app-body">
        <div style={{ textAlign: "center", padding: "6px 0 8px" }}>
          <div style={{ fontSize: 22, fontWeight: 700, color: "var(--green-deep)" }}>52 500 F</div>
          <div style={{ fontSize: 9, color: "var(--ink-soft)" }}>18 VENTES · 3 CRÉDITS</div>
        </div>
        <div className="app-row">
          <span>Panier moyen</span>
          <span className="price">2 065 F</span>
        </div>
        <div className="app-row">
          <span>Top : Lait Candia</span>
          <span>12 ventes</span>
        </div>
        <div className="app-note">Tu fermes, tu sais ce que tu as fait</div>
      </div>
    </div>
  );
}

/* ------------------------------------------------ */
/* La démo scroll-driven                             */
/* ------------------------------------------------ */

const FEATURES = [
  {
    id: "caisse",
    num: "01",
    title: "La caisse dans la poche",
    desc: "Touche un produit : le panier se remplit, le total se calcule, la monnaie se rend. Zéro erreur dans la foule.",
    Screen: CaisseScreen,
  },
  {
    id: "credits",
    num: "02",
    title: "Les crédits qui se rappellent",
    desc: "Le nom, le montant, la date. Plus jamais de « je paie demain » oublié.",
    Screen: CreditsScreen,
  },
  {
    id: "stock",
    num: "03",
    title: "Le stock à l'unité près",
    desc: "L'alerte sonne avant la rupture. Tu commandes avec des chiffres, pas des souvenirs.",
    Screen: StockScreen,
  },
  {
    id: "horsligne",
    num: "04",
    title: "Sans réseau, ça marche",
    desc: "Le marché, la foule, zéro barre... la vente continue. La synchro repart toute seule.",
    Screen: HorsligneScreen,
  },
  {
    id: "ticket",
    num: "05",
    title: "La preuve par WhatsApp",
    desc: "Le ticket part sur le téléphone du client. La confiance, la fin des disputes.",
    Screen: TicketScreen,
  },
  {
    id: "soir",
    num: "06",
    title: "Le soir, tout est là",
    desc: "Ton chiffre, tes ventes, tes crédits. Sans compter tes billets un par un.",
    Screen: SoirScreen,
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

  const ActiveScreen = FEATURES[active].Screen;

  return (
    <section className="section demo" id="demo">
      <div className="container">
        <Reveal>
          <Kicker>N° 002 — Essaie-la</Kicker>
          <h2>La caisse, en vrai. Scrolle, regarde, touche.</h2>
        </Reveal>

        <div className="demo-grid" style={{ marginTop: 44 }}>
          <div className="phone-sticky">
            <div className="phone">
              <div className="phone-notch" aria-hidden="true" />
              <div className="phone-screen">
                <AnimatePresence mode="wait">
                  <motion.div
                    key={FEATURES[active].id}
                    style={{ position: "absolute", inset: 0 }}
                    initial={{ opacity: 0, x: 26 }}
                    animate={{ opacity: 1, x: 0 }}
                    exit={{ opacity: 0, x: -26 }}
                    transition={{ duration: 0.28 }}
                  >
                    <ActiveScreen />
                  </motion.div>
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
                <h3 style={{ opacity: active === i ? 1 : 0.55, transition: "opacity 0.35s ease" }}>
                  {f.title}
                </h3>
                <p>{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
