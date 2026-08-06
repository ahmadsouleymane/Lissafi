// ============================================================
// Graphiques SVG/CSS — zéro dépendance, rendus côté serveur
// ============================================================

export type ChartPoint = { label: string; value: number };

/** Graphique en barres verticales. */
export function BarChart({
  data,
  height = 150,
  formatValue,
  color = "#2e8b57",
}: {
  data: ChartPoint[];
  height?: number;
  formatValue?: (n: number) => string;
  color?: string;
}) {
  const max = Math.max(1, ...data.map((d) => d.value));
  return (
    <div>
      <div className="flex items-end gap-[3px]" style={{ height }}>
        {data.map((d, i) => {
          const barHeight = Math.max(3, Math.round((d.value / max) * height));
          return (
            <div key={i} className="flex-1 flex items-end justify-center h-full">
              <div
                className="w-full max-w-[30px] rounded-t transition-opacity hover:opacity-80"
                style={{ height: barHeight, backgroundColor: color }}
                title={formatValue ? formatValue(d.value) : String(d.value)}
              />
            </div>
          );
        })}
      </div>
      <div className="mt-1.5 flex gap-[3px]">
        {data.map((d, i) => (
          <div key={i} className="flex-1 truncate text-center text-[9px] text-slate-400">
            {d.label}
          </div>
        ))}
      </div>
    </div>
  );
}

/** Graphique en ligne (courbe + aire), responsive. */
export function LineChart({
  data,
  height = 150,
  color = "#2e8b57",
}: {
  data: ChartPoint[];
  height?: number;
  color?: string;
}) {
  if (data.length === 0) return <div className="text-xs text-slate-400">Aucune donnée</div>;
  const vbW = 600;
  const max = Math.max(1, ...data.map((d) => d.value));
  const stepX = data.length > 1 ? vbW / (data.length - 1) : vbW;

  const pts = data.map((d, i) => {
    const x = i * stepX;
    const y = height - (d.value / max) * (height - 10) - 4;
    return [x, y] as const;
  });

  const linePath = pts.map(([x, y], i) => `${i === 0 ? "M" : "L"} ${x.toFixed(1)} ${y.toFixed(1)}`).join(" ");
  const areaPath = `${linePath} L ${vbW} ${height} L 0 ${height} Z`;

  return (
    <svg viewBox={`0 0 ${vbW} ${height}`} className="w-full h-auto" role="img" aria-label="Graphique">
      <defs>
        <linearGradient id="lissafi-area" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={color} stopOpacity="0.25" />
          <stop offset="100%" stopColor={color} stopOpacity="0" />
        </linearGradient>
      </defs>
      <path d={areaPath} fill="url(#lissafi-area)" />
      <path d={linePath} fill="none" stroke={color} strokeWidth="2" strokeLinejoin="round" strokeLinecap="round" />
      {pts.map(([x, y], i) => (
        <circle key={i} cx={x} cy={y} r="2.5" fill={color} />
      ))}
    </svg>
  );
}
