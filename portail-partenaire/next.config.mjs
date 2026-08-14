/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,

  // En-têtes de sécurité. Le portail est public mais ne doit rien exposer de
  // sensible : la clé service_role reste côté serveur, jamais dans le navigateur.
  async headers() {
    return [
      {
        source: "/(.*)",
        headers: [
          { key: "X-Frame-Options", value: "DENY" },
          { key: "X-Content-Type-Options", value: "nosniff" },
          { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
          { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
          {
            key: "Content-Security-Policy",
            // img-src https: pour l'image QR (api.qrserver.com) ; polices auto-hébergées (next/font) → 'self'.
            value: "default-src 'self'; script-src 'self' 'unsafe-inline' 'unsafe-eval'; style-src 'self' 'unsafe-inline'; img-src 'self' data: blob: https:; font-src 'self'; connect-src 'self' https://*.supabase.co;",
          },
        ],
      },
    ];
  },
};

export default nextConfig;
