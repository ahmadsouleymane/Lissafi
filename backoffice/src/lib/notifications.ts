import "server-only";
import { getApps, initializeApp, cert, type App } from "firebase-admin/app";
import { getMessaging } from "firebase-admin/messaging";
import { supabaseAdmin } from "./supabase";

// Doit correspondre au CHANNEL_ID de LissafiMessagingService.kt (canal Android),
// pour que les notifications reçues en premier plan et en arrière-plan utilisent
// le même canal.
const ANDROID_CHANNEL_ID = "lissafi_recap";

let firebaseApp: App | null = null;

function getFirebaseApp(): App {
  if (firebaseApp) return firebaseApp;
  const existing = getApps();
  if (existing.length > 0) {
    firebaseApp = existing[0];
    return firebaseApp;
  }
  const json = process.env.FIREBASE_SERVICE_ACCOUNT_JSON?.trim();
  if (!json) throw new Error("FIREBASE_SERVICE_ACCOUNT_JSON manquant — voir .env.example.");
  const credentials = JSON.parse(json);
  firebaseApp = initializeApp({ credential: cert(credentials) });
  return firebaseApp;
}

export type PushResult = { success: number; failed: number };

/**
 * Envoie une notification push (titre + texte) à une liste de tokens FCM,
 * par lots de 500 (limite de l'API FCM). Supprime de `device_tokens` les
 * tokens invalides rencontrés (nettoyage best-effort, non bloquant).
 */
export async function sendPushToTokens(tokens: string[], title: string, body: string): Promise<PushResult> {
  const unique = Array.from(new Set(tokens.filter(Boolean)));
  if (unique.length === 0) return { success: 0, failed: 0 };

  const messaging = getMessaging(getFirebaseApp());
  const staleTokens: string[] = [];
  let success = 0;
  let failed = 0;

  for (let i = 0; i < unique.length; i += 500) {
    const batch = unique.slice(i, i + 500);
    const response = await messaging.sendEachForMulticast({
      tokens: batch,
      notification: { title, body },
      android: { notification: { channelId: ANDROID_CHANNEL_ID } },
    });
    success += response.successCount;
    failed += response.failureCount;
    response.responses.forEach((r, idx) => {
      if (!r.success && r.error?.code === "messaging/registration-token-not-registered") {
        staleTokens.push(batch[idx]);
      }
    });
  }

  if (staleTokens.length > 0) {
    await supabaseAdmin().from("device_tokens").delete().in("fcm_token", staleTokens);
  }

  return { success, failed };
}
