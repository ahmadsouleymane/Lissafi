const STORAGE_KEY = "lissafi-theme";

export function initTheme() {
  const saved = localStorage.getItem(STORAGE_KEY);
  const theme = saved || (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");
  document.documentElement.setAttribute("data-theme", theme);
  return theme;
}

export function toggleTheme() {
  const current = document.documentElement.getAttribute("data-theme");
  const next = current === "dark" ? "light" : "dark";
  document.documentElement.setAttribute("data-theme", next);
  localStorage.setItem(STORAGE_KEY, next);
  return next;
}

export function getTheme() {
  return document.documentElement.getAttribute("data-theme") || "light";
}
