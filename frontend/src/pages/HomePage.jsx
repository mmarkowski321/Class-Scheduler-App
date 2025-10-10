import Navbar from '../components/layout/Navbar'
import Hero from '../components/sections/Hero'
import Features from '../components/sections/Features'
import Footer from '../components/layout/Footer'

function HomePage() {
  return (
    <div className="home-page">
      <Navbar />
      <main>
        <Hero />
        <Features />
      </main>
      <Footer />
    </div>
  )
}

export default HomePage