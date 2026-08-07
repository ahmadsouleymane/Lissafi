export function formatFCFA(n) {
  const s = String(Math.round(n));
  const grouped = s.replace(/\B(?=(\d{3})+(?!\d))/g, " ");
  return grouped + " F";
}
