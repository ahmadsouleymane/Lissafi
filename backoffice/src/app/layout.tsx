import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: { default: "Lissafi Admin", template: "%s · Lissafi Admin" },
  description: "Back-office d'administration Lissafi — comptes premium, statistiques, logs et support.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="fr">
      <body>{children}</body>
    </html>
  );
}
