// src/components/layout/Leftbar.jsx
import "./Leftbar.css";
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";

export default function Leftbar({
  open,
  onClose,
  roleLabel, // z AppShell (/app/tutor -> "korepetytor", /app/student -> "uczeń")
  user = { name: "Użytkownik", role: "" },
  children,
}) {
  const navigate = useNavigate();
  const { t, i18n } = useTranslation("common");

  // Prefer data from localStorage when available
  const storedFirst = (typeof window !== 'undefined' && window.localStorage) ? localStorage.getItem("firstName") : null;
  const storedLast = (typeof window !== 'undefined' && window.localStorage) ? localStorage.getItem("lastName") : null;
  const storedRole = (typeof window !== 'undefined' && window.localStorage) ? localStorage.getItem("role") : null;

  const userName = (storedFirst || storedLast)
    ? `${storedFirst || ""} ${storedLast || ""}`.trim()
    : (user?.name || "Użytkownik");
  const avatar = (userName[0] || "U").toUpperCase();

  // Mapowanie roleLabel z URL-a na klucz tłumaczenia
  const roleDisplay =
    roleLabel
      ? (roleLabel === "korepetytor" ? t("sidebar.user.role.tutor") 
        : roleLabel === "administrator" ? "Administrator"
        : t("sidebar.user.role.student"))
      : (storedRole === "ADMIN" || user?.role === "admin"
          ? "Administrator"
          : (storedRole === "TUTOR" || user?.role === "tutor"
            ? t("sidebar.user.role.tutor")
            : (storedRole === "STUDENT" || user?.role === "student")
              ? t("sidebar.user.role.student")
              : ""));

  // przełączanie języka + zapamiętanie
  const setLang = (lng) => {
    i18n.changeLanguage(lng);
    try { localStorage.setItem("lang", lng); } catch {}
  };

  const activeLang = i18n.language?.startsWith("pl") ? "pl" : "en";

  useEffect(() => {
    // ustaw atrybut <html lang="..."> dla SEO / a11y
    document.documentElement.lang = activeLang;
  }, [activeLang]);

  const logout = () => {
    // TODO: podmień na realny logout (API/cookies)
    localStorage.removeItem("devRole");
    navigate("/", { replace: true });
  };

  return (
    <>
      <aside className={`leftbar ${open ? "open" : ""}`}>
        {/* Brand */}
        <div
          className="leftbar__brand"
          onClick={() => {
            try {
              const role = localStorage.getItem("role");
              if (role === "STUDENT") navigate("/app/student");
              else if (role === "TUTOR") navigate("/app/tutor");
              else if (role === "ADMIN") navigate("/app/admin");
              else navigate("/");
            } catch {
              navigate("/");
            }
            onClose?.();
          }}
          aria-label={t("sidebar.brand")}
          title={t("sidebar.brand")}
        >
          <div className="dot" />
          <span className="title">{t("sidebar.brand")}</span>
        </div>

        {/* User */}
        <div className="leftbar__user">
          <div className="avatar">{avatar}</div>
          <div className="meta">
            <strong>{userName}</strong>
            {roleDisplay ? <small>{roleDisplay}</small> : null}
          </div>
        </div>

        {/* Nawigacja (wpinane NavLinki z StudentApp/TutorApp) */}
        <nav className="leftbar__nav" onClick={onClose}>
          {children}
        </nav>

        {/* Język (PL / EN) */}
        <div className="leftbar__lang" aria-label={t("sidebar.lang.label")}>
          <div className="lang-group" role="group" aria-label={t("sidebar.lang.label")}>
            <button
              type="button"
              className={`lang-btn ${activeLang === "pl" ? "active" : ""}`}
              onClick={() => setLang("pl")}
            >
              {t("sidebar.lang.pl")}
            </button>
            <button
              type="button"
              className={`lang-btn ${activeLang === "en" ? "active" : ""}`}
              onClick={() => setLang("en")}
            >
              {t("sidebar.lang.en")}
            </button>
          </div>
        </div>

        {/* Stopka akcji */}
        <div className="leftbar__footer">
          <button
            className="btn ghost"
            onClick={() => { navigate("settings"); onClose?.(); }}
          >
            {t("sidebar.actions.settings")}
          </button>
          <button className="btn danger" onClick={logout}>
            {t("sidebar.actions.logout")}
          </button>
        </div>
      </aside>

      {/* Overlay (mobile) */}
      <div
        className={`leftbar__overlay ${open ? "show" : ""}`}
        onClick={onClose}
      />
    </>
  );
}
