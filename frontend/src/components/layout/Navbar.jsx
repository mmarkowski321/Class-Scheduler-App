// src/components/layout/Navbar.jsx
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import "./Navbar.css";
import LanguageSwitcher from "../ui/LanguageSwitcher";
import Button from "../ui/Button";
import { useTranslation } from "react-i18next";

function Navbar() {
  const [menuOpen, setMenuOpen] = useState(false);
  const { t } = useTranslation("common");
  const navigate = useNavigate();

  const goTo = (anchor) => {
    navigate("/", { state: { scrollTo: anchor } });
    setMenuOpen(false);
  };

  return (
    <header className="navbar">
      <div className="navbar-container">
        {/* Logo */}
        <div className="logo" onClick={() => navigate("/")}>
          <span>{t("header.logo")}</span>
        </div>

        {/* Menu */}
        <nav className={`nav-links ${menuOpen ? "active" : ""}`}>
          {/* TYLKO Features przewija */}
          <button
            type="button"
            className="nav-linklike"
            onClick={() => goTo("features")}
          >
            {t("header.nav.features")}
          </button>

          {/* Dla ucznia -> osobna strona */}
          <button
            type="button"
            className="nav-linklike"
            onClick={() => {
              navigate("/student");
              setMenuOpen(false);
            }}
          >
            {t("header.nav.forStudent")}
          </button>

          {/* Dla nauczyciela (tutor) -> osobna strona */}
          <button
            type="button"
            className="nav-linklike"
            onClick={() => {
              navigate("/tutor");
              setMenuOpen(false);
            }}
          >
            {t("header.nav.forTutor")}
          </button>

          {/* O nas / Kontakt -> osobne strony */}
          <button
            type="button"
            className="nav-linklike"
            onClick={() => {
              navigate("/about");
              setMenuOpen(false);
            }}
          >
            {t("header.nav.about")}
          </button>
          <button
            type="button"
            className="nav-linklike"
            onClick={() => {
              navigate("/contact");
              setMenuOpen(false);
            }}
          >
            {t("header.nav.contact")}
          </button>

          {/* Actions */}
          <div className="nav-actions">
            <Button
              variant="primary"
              size="small"
              className="nav-btn"
              onClick={() => {
                navigate("/login");
                setMenuOpen(false);
              }}
            >
              {t("header.actions.login")}
            </Button>
            <Button
              variant="primary"
              size="small"
              className="nav-btn"
              onClick={() => {
                navigate("/register");
                setMenuOpen(false);
              }}
            >
              {t("header.actions.register")}
            </Button>
          </div>

          {/* Language (mobile) */}
          <div className="nav-lang-mobile">
            <LanguageSwitcher />
          </div>
        </nav>

        {/* Language (desktop) */}
        <div className="nav-lang-desktop">
          <LanguageSwitcher />
        </div>

        {/* Burger */}
        <div
          className={`burger ${menuOpen ? "open" : ""}`}
          onClick={() => setMenuOpen(!menuOpen)}
        >
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>

      {menuOpen && <div className="overlay" onClick={() => setMenuOpen(false)} />}
    </header>
  );
}

export default Navbar;
