import { useState } from "react";
import { Sun, Moon } from "lucide-react";
import { toggleTheme, getTheme } from "../lib/theme";

export default function ThemeToggle() {
  const [theme, setTheme] = useState(getTheme());
  return (
    <button
      className="theme-toggle"
      aria-label={theme === "dark" ? "Passer en mode clair" : "Passer en mode sombre"}
      onClick={() => setTheme(toggleTheme())}
    >
      {theme === "dark" ? <Sun size={18} /> : <Moon size={18} />}
    </button>
  );
}
