import { Reveal } from "./ui";

const POINTS = [
  "Conçu avec des commerçants de Niamey",
  "Fonctionne même sans réseau",
  "Tes données restent sur ton téléphone",
];

export default function TrustBand() {
  return (
    <section className="trust-band">
      <div className="container trust-band-inner">
        {POINTS.map((p) => (
          <Reveal key={p} as="span" className="trust-point">
            {p}
          </Reveal>
        ))}
      </div>
    </section>
  );
}
