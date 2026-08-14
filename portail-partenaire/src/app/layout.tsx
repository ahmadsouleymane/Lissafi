import type { Metadata } from "next";
import { Inter } from "next/font/google";
import "./globals.css";

// Inter auto-hébergé (identité de marque Lissafi) — pas de requête externe,
// compatible avec la CSP stricte (font-src 'self').
const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
  display: "swap",
});

export const metadata: Metadata = {
  title: { default: "Lissafi Partenaires", template: "%s · Lissafi Partenaires" },
  description:
    "Deviens partenaire Lissafi : présente la caisse aux commerçants et touche une commission en espèces à chaque abonnement.",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="fr" className={inter.variable}>
      <body className="font-sans">{children}</body>
    </html>
  );
}
