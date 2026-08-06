import { redirect } from "next/navigation";
import { getAdminSession } from "@/lib/session";
import { AppShell } from "@/components/AppShell";

export const dynamic = "force-dynamic";

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const session = await getAdminSession();
  if (!session) redirect("/login");

  return <AppShell email={session.email}>{children}</AppShell>;
}
