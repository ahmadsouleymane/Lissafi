// Logo Lissafi pour le back-office — panier vert + coche
export function Logo({ size = 32, withText = true }: { size?: number; withText?: boolean }) {
  return (
    <span className="inline-flex items-center gap-2">
      <svg width={size} height={size} viewBox="0 0 48 48" fill="none" aria-hidden>
        <rect width="48" height="48" rx="10" fill="#2e8b57" />
        <path
          d="M12 14h3l2.5 14a3 3 0 0 0 3 2.5h12.2a3 3 0 0 0 3-2.4l2.3-11.1H15.6"
          stroke="white"
          strokeWidth="2.6"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        <circle cx="20.5" cy="35" r="2.1" fill="white" />
        <circle cx="31.5" cy="35" r="2.1" fill="white" />
        <path d="M30 18.5l2.5 2.5L38 15" stroke="white" strokeWidth="2.6" strokeLinecap="round" strokeLinejoin="round" />
      </svg>
      {withText && (
        <span className="text-lg font-bold tracking-tight text-slate-900">
          Lissafi<span className="text-brand-600"> Admin</span>
        </span>
      )}
    </span>
  );
}
