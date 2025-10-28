// src/pages/app/AppShell.jsx
import Container from "../../components/ui/Container";
import { useLocation } from "react-router-dom";
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import Leftbar from "../../components/layout/Leftbar";
import "./app.css";

export default function AppShell({ sidebar, children, title, titleKey }) {
  const { pathname } = useLocation();
  const [open, setOpen] = useState(false);
  const { t } = useTranslation("common");

  const roleLabel = useMemo(() => {
    if (pathname.startsWith("/app/tutor")) return "korepetytor";
    if (pathname.startsWith("/app/student")) return "uczeń";
    return "";
  }, [pathname]);

  const crumbs = useMemo(() => {
    const parts = pathname.replace(/^\/+|\/+$/g, "").split("/");
    const start = parts[0] === "app" ? 1 : 0;
    return "/" + parts.slice(start).join("/");
  }, [pathname]);

  return (
    <div className="app-wrap gradient-bg">
      <Leftbar open={open} onClose={() => setOpen(false)} roleLabel={roleLabel}>
        {sidebar}
      </Leftbar>

      <button className="hamburger" aria-label="menu" onClick={() => setOpen(true)}>
        <span /><span /><span />
      </button>

      <main className="app-main">
        <Container size="xxl">
          <header className="app-header">
            <div>
              {/* jeśli podasz titleKey – przetłumacz go; w innym wypadku pokaż zwykły title */}
              <h1>{titleKey ? t(titleKey) : title}</h1>
              <div className="crumb">{crumbs}</div>
            </div>
          </header>

          <section className="app-content">{children}</section>
        </Container>
      </main>

      {open && <div className="app-overlay" onClick={() => setOpen(false)} />}
    </div>
  );
}
