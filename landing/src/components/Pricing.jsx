import { WHATSAPP, APK } from "../config";
import { Kicker, Reveal } from "./ui";

export default function Pricing() {
  return (
    <section className="section" id="tarifs">
      <div className="container">
        <Reveal>
          <Kicker>N° 004 — Les tarifs</Kicker>
          <h2>Gratuit pour tester. 68 F par jour pour tout débloquer.</h2>
        </Reveal>

        <div className="pricing-grid">
          <Reveal>
            <div className="price-card">
              <div className="price-plan">Gratuit</div>
              <div className="price-amount">0 F</div>
              <div className="price-sub">pour découvrir la caisse</div>
              <ul className="price-features">
                <li>10 produits au catalogue</li>
                <li>10 clients suivis</li>
                <li>10 ventes par jour</li>
                <li>Ticket WhatsApp</li>
                <li>Marche sans réseau</li>
                <li className="off">Rapports & export illimités</li>
                <li className="off">Gestion de stock complète</li>
              </ul>
              <a className="btn btn-ghost" href={APK} download>
                Télécharger l'APK
              </a>
            </div>
          </Reveal>

          <Reveal delay={0.1}>
            <div className="price-card premium">
              <span className="price-badge">Populaire</span>
              <div className="price-plan">Lissafi Essentiel</div>
              <div className="price-amount">
                25 000 F<small>/an</small>
              </div>
              <div className="price-sub">= 68 F par jour · tout illimité</div>
              <ul className="price-features">
                <li>Produits illimités</li>
                <li>Clients & crédits illimités</li>
                <li>Ventes illimitées chaque jour</li>
                <li>Historique illimité</li>
                <li>Gestion de stock complète</li>
                <li>Rapports jour / semaine / mois</li>
                <li>Export CSV de tes données</li>
                <li>Support prioritaire WhatsApp</li>
              </ul>
              <a className="btn btn-primary" href={WHATSAPP} target="_blank" rel="noreferrer">
                Je passe à Essentiel →
              </a>
            </div>
          </Reveal>
        </div>
      </div>
    </section>
  );
}
