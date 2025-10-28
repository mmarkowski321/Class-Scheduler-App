// pages/app/StudentApp.jsx
import { NavLink, Routes, Route, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import AppShell from "./AppShell";
import StudentOverview from "./student/StudentOverview";
import StudentProfile from "./student/StudentProfile";
import StudentLessons from "./student/StudentLessons";
import StudentCalendar from "./student/StudentCalendar";
import StudentSettings from "./student/StudentSettings";
import StudentAvailability from "./student/StudentAvailability";
// NEW:
import StudentTutors from "./student/StudentTutors";

export default function StudentApp() {
  const navigate = useNavigate();
  const { t } = useTranslation("common");

  return (
    <AppShell
      titleKey="app.student.title"
      sidebar={
        <nav className="nav">
          <NavLink to="/app/student" end>🏠 {t("sidebar.nav.overview")}</NavLink>
          {/* NEW: przeglądanie korepetytorów */}
          <NavLink to="/app/student/tutors">🔎 {t("sidebar.nav.tutors")}</NavLink>
          <NavLink to="/app/student/profile">👤 {t("sidebar.nav.profile")}</NavLink>
          <NavLink to="/app/student/lessons">📝 {t("sidebar.nav.lessons")}</NavLink>
          <NavLink to="/app/student/calendar">🗓️ {t("sidebar.nav.calendar")}</NavLink>
          <NavLink to="/app/student/availability">⏰ {t("sidebar.nav.availability")}</NavLink>
          <NavLink to="/app/student/settings">⚙️ {t("sidebar.nav.settings")}</NavLink>
        </nav>
      }
    >
      <Routes>
        <Route index element={<StudentOverview onQuickBook={() => navigate("/tutor")} />} />
        {/* NEW: strona listy korepetytorów */}
        <Route path="tutors" element={<StudentTutors />} />
        <Route path="profile" element={<StudentProfile />} />
        <Route path="lessons" element={<StudentLessons />} />
        <Route path="calendar" element={<StudentCalendar />} />
        <Route path="availability" element={<StudentAvailability />} />
        <Route path="settings" element={<StudentSettings />} />
      </Routes>
    </AppShell>
  );
}
