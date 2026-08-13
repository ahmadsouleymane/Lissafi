import { motion } from "motion/react";
import { Handshake, Link2, Wallet, ArrowRight } from "lucide-react";
import { BECOME_PARTNER_URL } from "../config";
import { Kicker, Reveal } from "./ui";

const TIERS = [
  { plan: "Pack Boutique", amount: "5 000" },
  { plan: "Lissafi Plus", amount: "10 000" },
  { plan: "Lissafi Business", amount: "15 000" },
];

const STEPS = [
  { icon: Handshake, title: "Inscris-toi", body: "Compte gratuit en 30 secondes. Actif tout de suite." },
  { icon: Link2, title: "Partage ton lien", body: "Ton lien perso + QR code sur WhatsApp, TikTok ou en affichette." },
  { icon: Wallet, title: "Sois payé", body: "Commission en espèces à chaque client, sur Orange Money ou Moov." },
];

export default function Partnership() {
  return (
    <section className="partner-section on-ink" id="partenaires">
      <img
        src="/assets/logo-icon.svg"
        alt=""
        aria-hidden="true"
        className="partner-watermark"
      />
      <div className="container">
        <Reveal className="section-head center">
          <Kicker>Programme partenaire</Kicker>
          <h2>Gagne de l'argent en présentant Lissafi.</h2>
          <p>
            Tu connais des commerçants ? Amène-les sur Lissafi et touche une
            commission <b>en espèces</b> à chaque abonnement. Sans stock, sans
            plafond, sans investissement.
          </p>
        </Reveal>

        <div className="partner-tiers">
          {TIERS.map((t, i) => (
            <Reveal key={t.plan} delay={i * 0.08} className="partner-tier">
              <span className="partner-tier-plan">{t.plan}</span>
              <span className="partner-tier-amount">
                {t.amount} <small>F</small>
              </span>
              <span className="partner-tier-label">par client</span>
            </Reveal>
          ))}
        </div>

        <div className="partner-steps">
          {STEPS.map((s, i) => {
            const Icon = s.icon;
            return (
              <Reveal key={s.title} delay={0.1 + i * 0.08} className="partner-step">
                <span className="partner-step-icon">
                  <Icon size={20} />
                </span>
                <div>
                  <h3>{s.title}</h3>
                  <p>{s.body}</p>
                </div>
              </Reveal>
            );
          })}
        </div>

        <motion.div
          className="partner-cta"
          initial={{ opacity: 0, y: 18 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-80px" }}
          transition={{ duration: 0.6, delay: 0.2 }}
        >
          <a
            className="btn btn-primary btn-lg"
            href={BECOME_PARTNER_URL}
            target="_blank"
            rel="noopener noreferrer"
          >
            Devenir partenaire <ArrowRight size={18} />
          </a>
          <span className="partner-cta-note">Gratuit · payé sur mobile money</span>
        </motion.div>
      </div>
    </section>
  );
}
