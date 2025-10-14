// src/pages/HomePage.jsx
import Navbar from '../components/layout/Navbar'
import Hero from '../components/sections/Hero'
import Features from '../components/sections/Features'
import Footer from '../components/layout/Footer'
import ScrollToSection from '../components/utils/ScrollToSection'

function HomePage() {
  return (
    <div className="home-page">
      <Navbar />
      <main>
        <Hero />
        <div id="features">
          <Features />
        </div>
      </main>
      <Footer />
      <ScrollToSection />
    </div>
  )
}

export default HomePage
