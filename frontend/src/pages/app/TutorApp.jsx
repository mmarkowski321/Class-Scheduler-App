import { NavLink, Routes, Route, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { useEffect } from "react";
import AppShell from "./AppShell";
import TutorOverview from "./tutor/TutorOverview";
import TutorProfile from "./tutor/TutorProfile";
import TutorLessons from "./tutor/TutorLessons";
import TutorCalendar from "./tutor/TutorCalendar";
import TutorSettings from "./tutor/TutorSettings";
import TutorAvailability from "./tutor/TutorAvailability";
import TutorReviews from "./tutor/TutorReviews";

export default function TutorApp() {
  const { t } = useTranslation("common");
  const navigate = useNavigate();

  useEffect(() => {
    const role = localStorage.getItem("role");
    if (!role) {
      navigate("/login", { replace: true });
      return;
    }
    if (role !== "TUTOR") {
      navigate(role === "STUDENT" ? "/app/student" : "/app/admin", {
        replace: true,
      });
    }
  }, [navigate]);

  return (
    <AppShell
      titleKey="app.tutor.title"
      sidebar={
        <nav className="nav">
          <NavLink to="/app/tutor" end>
            🏠 {t("sidebar.nav.overview")}
          </NavLink>
          <NavLink to="/app/tutor/profile">👤 {t("sidebar.nav.profile")}</NavLink>
          <NavLink to="/app/tutor/lessons">📝 {t("sidebar.nav.lessons")}</NavLink>
          <NavLink to="/app/tutor/calendar">🗓️ {t("sidebar.nav.calendar")}</NavLink>
          <NavLink to="/app/tutor/availability">⏰ {t("sidebar.nav.availability")}</NavLink>
          <NavLink to="/app/tutor/reviews">⭐ {t("sidebar.nav.reviews")}</NavLink>
          <NavLink to="/app/tutor/settings">⚙️ {t("sidebar.nav.settings")}</NavLink>
        </nav>
      }
    >
      <Routes>
        <Route index element={<TutorOverview />} />
        <Route path="profile" element={<TutorProfile />} />
        <Route path="lessons" element={<TutorLessons />} />
        <Route path="calendar" element={<TutorCalendar />} />
        <Route path="availability" element={<TutorAvailability />} />
        <Route path="reviews" element={<TutorReviews />} />
        <Route path="settings" element={<TutorSettings />} />
      </Routes>
    </AppShell>
  );
}
