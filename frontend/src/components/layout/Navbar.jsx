import { useState } from "react";
import "./Navbar.css";
import LanguageSwitcher from "../ui/LanguageSwitcher";
import Button from "../ui/Button";
import { useTranslation } from "react-i18next";

function Navbar() {
  const [menuOpen, setMenuOpen] = useState(false);
  const { t } = useTranslation("common");

  return (
    <header className="navbar">
      <div className="navbar-container">
        {/* 🔹 Logo */}
        <div className="logo">
          <span>{t("header.logo")}</span>
        </div>

        {/* 🔹 Menu desktop + mobile */}
        <nav className={`nav-links ${menuOpen ? "active" : ""}`}>
          <a href="#features">{t("header.nav.features")}</a>
          <a href="#student">{t("header.nav.forStudent")}</a>
          <a href="#teacher">{t("header.nav.forTutor")}</a>
          <a href="#about">{t("header.nav.about")}</a>
          <a href="#contact">{t("header.nav.contact")}</a>

          {/* 🔹 Buttons */}
          <div className="nav-actions">
            <Button variant="primary" size="small" className="nav-btn">
              {t("header.actions.login")}
            </Button>
            <Button variant="primary" size="small" className="nav-btn">
              {t("header.actions.register")}
            </Button>
          </div>

          {/* 🔹 Language switcher (mobile) */}
          <div className="nav-lang-mobile">
            <LanguageSwitcher />
          </div>
        </nav>

        {/* 🔹 Language switcher (desktop) */}
        <div className="nav-lang-desktop">
          <LanguageSwitcher />
        </div>

        {/* 🔹 Burger icon */}
        <div
          className={`burger ${menuOpen ? "open" : ""}`}
          onClick={() => setMenuOpen(!menuOpen)}
        >
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>

      {/* 🔹 Overlay */}
      {menuOpen && <div className="overlay" onClick={() => setMenuOpen(false)}></div>}
    </header>
  );
}

export default Navbar;
