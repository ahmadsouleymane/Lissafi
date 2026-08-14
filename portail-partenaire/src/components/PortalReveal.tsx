"use client";

import { CSSProperties, ReactNode, useEffect, useRef, useState } from "react";

// Révélation au défilement, sans dépendance (IntersectionObserver + keyframe CSS
// `portalFadeUp` de globals.css).
export function Reveal({
  children,
  delay = 0,
  className = "",
}: {
  children: ReactNode;
  delay?: number;
  className?: string;
}) {
  const ref = useRef<HTMLDivElement>(null);
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

  return (
    <div ref={ref} data-reveal className={className} style={style}>
      {children}
    </div>
  );
}
