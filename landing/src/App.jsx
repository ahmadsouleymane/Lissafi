import Header from "./components/Header";
import Hero from "./components/Hero";
import Problem from "./components/Problem";
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
      <div className="grain" aria-hidden="true" />
      <Header />
      <main>
        <Hero />
        <div className="section-rule" aria-hidden="true" />
        <Problem />
        <PhoneDemo />
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
