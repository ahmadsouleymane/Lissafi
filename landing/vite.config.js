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
      async closeBundle() {
        // Applique `{SITE_URL}` aussi à robots.txt et sitemap.xml (fichiers public/).
        const site = process.env.VITE_SITE_URL || "";
        if (!site) return;
        const fs = await import("node:fs/promises");
        const path = await import("node:path");
        for (const f of ["robots.txt", "sitemap.xml"]) {
          try {
            const file = path.resolve("dist", f);
            const content = await fs.readFile(file, "utf8");
            await fs.writeFile(file, content.replace(/\{SITE_URL\}/g, site));
          } catch {
            // fichier absent (nettoyage du dist) : on ignore.
          }
        }
      },
    },
  ],
  base: "./",
});
