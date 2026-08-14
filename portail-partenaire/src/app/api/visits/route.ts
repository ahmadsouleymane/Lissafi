import { NextRequest, NextResponse } from "next/server";
import { supabaseAdmin } from "@/lib/supabase";

export const dynamic = "force-dynamic";

// ============================================================
// Beacon public de visite partenaire.
// La landing (statique, autre origine) POST { code } à chaque clic sur
// un lien `?p=CODE`. On résout le partenaire et on insère la visite via
// service_role. CORS restreint à l'origine de la landing.
// ============================================================

/** Origines autorisées à appeler ce beacon (landing + dev). */
function allowedOrigins(): string[] {
  const list = [
    process.env.NEXT_PUBLIC_LANDING_URL,
    process.env.LANDING_URL,
    "http://localhost:5173",
    "http://localhost:4173",
  ];
  return list
    .filter((v): v is string => Boolean(v))
    .map((v) => v.trim().replace(/\/+$/, ""));
}

function corsHeaders(origin: string | null): Record<string, string> {
  const allowed = allowedOrigins();
  const match = origin && allowed.includes(origin.replace(/\/+$/, "")) ? origin : "";
  return {
    "Access-Control-Allow-Origin": match,
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type",
    Vary: "Origin",
  };
}

export async function OPTIONS(req: NextRequest) {
  return new NextResponse(null, { status: 204, headers: corsHeaders(req.headers.get("origin")) });
}

export async function POST(req: NextRequest) {
  const origin = req.headers.get("origin");
  const headers = corsHeaders(origin);

  // Origine non autorisée → 403 (l'en-tête Allow-Origin vide bloque déjà côté navigateur).
  if (origin && !headers["Access-Control-Allow-Origin"]) {
    return new NextResponse(null, { status: 403, headers });
  }

  let code = "";
  try {
    const body = await req.json();
    code = String(body?.code || "").trim().toUpperCase();
  } catch {
    return new NextResponse(null, { status: 400, headers });
  }
  if (!code) return new NextResponse(null, { status: 400, headers });

  const { data: partner } = await supabaseAdmin()
    .from("partners")
    .select("id")
    .eq("code", code)
    .maybeSingle();

  // Code inconnu → 404 silencieux (pas de fuite d'information).
  if (!partner) return new NextResponse(null, { status: 404, headers });

  await supabaseAdmin()
    .from("partner_visits")
    .insert({ partner_id: partner.id, created_at: Date.now() });

  return new NextResponse(null, { status: 204, headers });
}
