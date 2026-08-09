import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [
    react(),
    {
      name: "inject-site-url",
      transformIndexHtml(html) {
        // `{SITE_URL}` dans index.html → domaine final (VITE_SITE_URL), vide par défaut.
        const site = process.env.VITE_SITE_URL || "";
        return html.replace(/\{SITE_URL\}/g, site);
      },
    },
  ],
  base: "./",
});
