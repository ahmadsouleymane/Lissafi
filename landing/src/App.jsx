import { Analytics } from "@vercel/analytics/react";
import PartnerBanner from "./components/PartnerBanner";
import Header from "./components/Header";
import Hero from "./components/Hero";
import FeatureBento from "./components/FeatureBento";
import PhoneDemo from "./components/PhoneDemo";
import Calculator from "./components/Calculator";
import Pricing from "./components/Pricing";
import Install from "./components/Install";
import FAQ from "./components/FAQ";
import FinalCTA from "./components/FinalCTA";
import Footer from "./components/Footer";

export default function App() {
  return (
    <>
      <PartnerBanner />
      <Header />
      <main>
        <Hero />
        <FeatureBento />
        <PhoneDemo />
        <Calculator />
        <Pricing />
        <Install />
        <FAQ />
        <FinalCTA />
      </main>
      <Footer />
      <Analytics />
    </>
  );
}
