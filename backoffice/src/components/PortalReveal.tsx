"use client";

import { CSSProperties, ReactNode, useEffect, useRef, useState } from "react";

// Révélation au défilement, sans dépendance (IntersectionObserver + keyframe CSS
// `portalFadeUp` de globals.css). Reproduit l'effet de la landing dans le portail.
export function Reveal({
  children,
  delay = 0,
  className = "",
  as: Tag = "div",
}: {
  children: ReactNode;
  delay?: number;
  className?: string;
  as?: "div" | "section" | "li";
}) {
  const ref = useRef<HTMLElement>(null);
  const [shown, setShown] = useState(false);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const io = new IntersectionObserver(
      ([entry]) => {
        if (entry.isIntersecting) {
          setShown(true);
          io.disconnect();
        }
      },
      { rootMargin: "-60px" }
    );
    io.observe(el);
    return () => io.disconnect();
  }, []);

  const style: CSSProperties = shown
    ? { animation: `portalFadeUp 0.7s cubic-bezier(0.22, 1, 0.36, 1) ${delay}s forwards` }
    : { opacity: 0 };

  // @ts-expect-error — ref polymorphe volontairement souple.
  return <Tag ref={ref} className={className} style={style}>{children}</Tag>;
}
