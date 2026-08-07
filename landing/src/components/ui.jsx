import { motion } from "motion/react";

/** Étiquette mono de section, type dossier de caisse */
export function Kicker({ children }) {
  return <span className="kicker">{children}</span>;
}

/** Wrapper de révélation au scroll */
export function Reveal({ children, as = "div", delay = 0, y = 26, className = "" }) {
  const Tag = motion[as] || motion.div;
  return (
    <Tag
      className={`reveal ${className}`.trim()}
      initial={{ opacity: 0, y }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, margin: "-80px" }}
      transition={{ duration: 0.65, delay, ease: [0.22, 1, 0.36, 1] }}
    >
      {children}
    </Tag>
  );
}
