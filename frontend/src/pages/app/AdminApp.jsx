import { NavLink, Routes, Route } from "react-router-dom";
import { useTranslation } from "react-i18next";
import AppShell from "./AppShell";
import AdminOverview from "./admin/AdminOverview";
import AdminUsers from "./admin/AdminUsers";
import AdminMessages from "./admin/AdminMessages";
import AdminReviews from "./admin/AdminReviews";

export default function AdminApp() {
  const { t } = useTranslation("common");

  return (
    <AppShell
      titleKey="app.admin.title"
      sidebar={
        <nav className="nav">
          <NavLink to="/app/admin" end>
            🏠 {t("sidebar.nav.overview")}
          </NavLink>
          <NavLink to="/app/admin/users">
            👥 {t("app.admin.nav.users")}
          </NavLink>
          <NavLink to="/app/admin/messages">
            📧 {t("app.admin.nav.messages")}
          </NavLink>
          <NavLink to="/app/admin/reviews">
            ⭐ {t("app.admin.nav.reviews")}
          </NavLink>
        </nav>
      }
    >
      <Routes>
        <Route index element={<AdminOverview />} />
        <Route path="users" element={<AdminUsers />} />
        <Route path="messages" element={<AdminMessages />} />
        <Route path="reviews" element={<AdminReviews />} />
      </Routes>
    </AppShell>
  );
}

