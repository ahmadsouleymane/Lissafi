import { NextRequest, NextResponse } from "next/server";
import { landingBaseUrl } from "@/lib/partners";

// ============================================================
// Téléchargement de l'APK avec code partenaire embarqué.
//
// La landing ne sert plus un .apk figé : elle pointe ici. Quand un visiteur
// arrive via un lien `?p=CODE`, on inscrit `lissafi.partner=CODE` dans le
// COMMENTAIRE de fin du ZIP — le seul endroit du fichier que la signature
// Android (APK Signature Scheme v2/v3) ne couvre pas. L'app le relit à son
// premier lancement (ApplicationInfo.sourceDir) pour retrouver qui l'a
// référée, même si le .apk a été repartagé (WhatsApp, Bluetooth, clé USB) —
// contrairement au presse-papiers.
//
// L'APK de base est récupéré depuis la landing (source unique, déjà commitée
// et publiée par `npm run release-apk`), puis mis en cache pour l'instance.
// ============================================================

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

const CODE_REGEX = /^PTN-[A-Z0-9]{3,}$/;

// Cache d'instance : on ne re-télécharge pas 13 Mo à chaque requête.
let baseApk: Buffer | null | undefined;

async function getBaseApk(): Promise<Buffer | null> {
  if (baseApk !== undefined) return baseApk;
  try {
    const url = `${landingBaseUrl()}/lissafi.apk`;
    const res = await fetch(url, { cache: "no-store" });
    if (!res.ok) {
      console.error("[download] échec récupération APK:", res.status, url);
      baseApk = null;
      return null;
    }
    baseApk = Buffer.from(await res.arrayBuffer());
  } catch (e) {
    console.error("[download] exception récupération APK:", e);
    baseApk = null;
  }
  return baseApk;
}

// Signature « End of Central Directory » (PK\x05\x06), dernière structure du ZIP.
const EOCD_SIG = [0x50, 0x4b, 0x05, 0x06];

/**
 * Ajoute `comment` au commentaire global du ZIP, sans casser la signature
 * Android. Met à jour la longueur du commentaire dans l'EOCD (offset +20).
 * Retourne l'APK d'origine si la structure n'est pas reconnue (défensif).
 */
function appendZipComment(apk: Buffer, comment: string): Buffer {
  let eocd = -1;
  // Le commentaire fait au plus 65 535 octets, juste avant l'EOCD (22 octets).
  const start = Math.max(0, apk.length - 65557);
  for (let i = apk.length - 22; i >= start; i--) {
    if (
      apk[i] === EOCD_SIG[0] &&
      apk[i + 1] === EOCD_SIG[1] &&
      apk[i + 2] === EOCD_SIG[2] &&
      apk[i + 3] === EOCD_SIG[3]
    ) {
      eocd = i;
      break;
    }
  }
  if (eocd === -1) return apk;

  const oldLen = apk.readUInt16LE(eocd + 20);
  const commentBuf = Buffer.from(comment, "utf8");
  const out = Buffer.concat([apk, commentBuf]);
  out.writeUInt16LE((oldLen + commentBuf.length) & 0xffff, eocd + 20);
  return out;
}

export async function GET(req: NextRequest) {
  const apk = await getBaseApk();
  if (!apk) {
    return new NextResponse("APK indisponible. Contacte le support Lissafi.", { status: 503 });
  }

  const code = (req.nextUrl.searchParams.get("p") || "").trim().toUpperCase();
  const body = CODE_REGEX.test(code)
    ? appendZipComment(apk, `lissafi.partner=${code}`)
    : Buffer.from(apk); // copie fraîche : ne pas détacher le buffer mis en cache

  // NextResponse attend un BodyInit DOM (ArrayBufferView sur ArrayBuffer) ;
  // Buffer est un Uint8Array<ArrayBufferLike> → on le normalise explicitement.
  return new NextResponse(new Uint8Array(body), {
    headers: {
      "Content-Type": "application/vnd.android.package-archive",
      "Content-Disposition": 'attachment; filename="lissafi.apk"',
      "Content-Length": String(body.length),
      "Cache-Control": "no-store",
    },
  });
}
