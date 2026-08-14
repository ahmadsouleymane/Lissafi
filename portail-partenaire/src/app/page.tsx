import { redirect } from "next/navigation";

// La racine du portail = la page partenaire.
export default function RootPage() {
  redirect("/partenaire");
}
