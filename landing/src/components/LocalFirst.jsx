import { CloudOff, RefreshCw, ShieldCheck } from "lucide-react";
import { Kicker, Reveal } from "./ui";

const POINTS = [
  {
    Icon: CloudOff,
    title: "Ça marche sans réseau",
    desc: "Chaque vente s'enregistre directement sur ton téléphone, même en zone blanche ou en pleine coupure.",
  },
  {
    Icon: RefreshCw,
    title: "La synchro se fait toute seule",
    desc: "Dès que le réseau revient, tout part se sauvegarder en arrière-plan. Tu n'as rien à faire.",
  },
  {
    Icon: ShieldCheck,
    title: "Tes données ne se perdent jamais",
    desc: "Téléphone perdu, cassé, volé : tes ventes, tes clients et ton stock restent sauvegardés en ligne.",
  },
];

export default function LocalFirst() {
  return (
    <section className="section local-first" id="local-first">
      <div className="container">
        <Reveal>
          <Kicker>Fiabilité</Kicker>
          <h2>Le réseau n'est pas toujours fiable. Lissafi, si.</h2>
        </Reveal>

        <div className="local-first-grid">
          {POINTS.map((p, i) => (
            <Reveal key={p.title} delay={i * 0.08}>
              <div className="local-first-card">
                <p.Icon size={22} color="var(--primary)" />
                <h3>{p.title}</h3>
                <p>{p.desc}</p>
              </div>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
