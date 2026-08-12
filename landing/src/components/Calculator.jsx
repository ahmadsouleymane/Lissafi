import { useState } from "react";
import { useMotionValue, useSpring, useMotionValueEvent } from "motion/react";
import { Kicker, Reveal } from "./ui";
import { formatFCFA } from "../lib/format";

const PRICE = 25000;

export default function Calculator() {
  const [slider, setSlider] = useState(15000);
  const mv = useMotionValue(15000);
  const spring = useSpring(mv, { stiffness: 90, damping: 18 });
  const [monthly, setMonthly] = useState(15000);

  useMotionValueEvent(spring, "change", (v) => setMonthly(Math.round(v)));

  const annualLoss = monthly * 12;
  const saving = Math.max(0, annualLoss - PRICE);

  return (
    <section className="section" id="calcul">
      <div className="container calc-wrap">
        <Reveal>
          <Kicker>Fais le calcul</Kicker>
          <h2>Combien ton cahier te fait-il perdre ?</h2>
          <p style={{ marginTop: 16, color: "var(--text-secondary)", fontSize: 16, maxWidth: "30em" }}>
            Glisse le curseur : c'est ce que tes clients te doivent en crédits oubliés,
            chaque mois. Regarde la différence.
          </p>
        </Reveal>

        <Reveal delay={0.1}>
          <div className="calc-panel">
            <label>Crédits oubliés chaque mois</label>
            <input
              className="calc-slider"
              type="range"
              min={0}
              max={50000}
              step={500}
              value={slider}
              onChange={(e) => {
                const v = Number(e.target.value);
                setSlider(v);
                mv.set(v);
              }}
              aria-label="Montant des crédits oubliés chaque mois"
            />
            <div className="calc-scales">
              <span>0 F</span>
              <span>25 000 F</span>
              <span>50 000 F</span>
            </div>

            <div className="calc-numbers">
              <div className="calc-line">
                <span>Perdu par an avec le cahier</span>
                <b>{formatFCFA(annualLoss)}</b>
              </div>
              <div className="calc-line">
                <span>Lissafi Essentiel</span>
                <b>25 000 F /an</b>
              </div>
              <div className="calc-line saving">
                <span>Tu récupères jusqu'à</span>
                <b>{formatFCFA(saving)} /an</b>
              </div>
            </div>

            <div className="calc-verdict">
              Pour <b>68 F par jour</b>, Lissafi se rentabilise dès le premier crédit récupéré.
            </div>
          </div>
        </Reveal>
      </div>
    </section>
  );
}
