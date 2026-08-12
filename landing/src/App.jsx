import Header from "./components/Header";
import Hero from "./components/Hero";
import TrustBand from "./components/TrustBand";
import AvantApres from "./components/AvantApres";
import Problem from "./components/Problem";
import PhoneDemo from "./components/PhoneDemo";
import LocalFirst from "./components/LocalFirst";
import Calculator from "./components/Calculator";
import Pricing from "./components/Pricing";
import Install from "./components/Install";
import FAQ from "./components/FAQ";
import FinalCTA from "./components/FinalCTA";
import Footer from "./components/Footer";

export default function App() {
  return (
    <>
      <Header />
      <main>
        <Hero />
        <TrustBand />
        <AvantApres />
        <Problem />
        <PhoneDemo />
        <LocalFirst />
        <Calculator />
        <Pricing />
        <Install />
        <FAQ />
        <FinalCTA />
      </main>
      <Footer />
    </>
  );
}
