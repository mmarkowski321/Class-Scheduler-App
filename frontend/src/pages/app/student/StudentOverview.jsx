import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import Alert from "../../../components/ui/Alert";
import Button from "../../../components/ui/Button";
import "../tutor/tutor-overview.css";
import { fetchStudentOverview } from "../../../services/lessons";
import { SUBJECTS } from "../../../data/subjects";
import { ensureRoleInStorage } from "../../../utils/auth";

const STATUS_WITH_DETAILS = new Set([
  "REQUESTED",
  "RESCHEDULED",
  "CANCELLED",
]);

const formatDate = (iso, lang) => {
  if (!iso) return "";
  return new Date(iso).toLocaleString(lang === "pl" ? "pl-PL" : "en-US", {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
};

export default function StudentOverview({ onQuickBook }) {
  const { t, i18n } = useTranslation("common");
  const [data, setData] = useState({
    upcoming: [],
    history: [],
    attention: [],
    newTutors: [],
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const token = useMemo(
    () => localStorage.getItem("token") || localStorage.getItem("access_token"),
    []
  );

  const subjectLabelMap = useMemo(() => {
    const map = new Map();
    SUBJECTS.forEach((entry) => {
      map.set(
        entry.value,
        i18n.language === "en" ? entry.labelEn : entry.label
      );
    });
    return map;
  }, [i18n.language]);

  useEffect(() => {
    if (token) {
      ensureRoleInStorage(token);
    }
    const loadOverview = async () => {
      if (!token) {
        setError(t("auth.required"));
        setLoading(false);
        return;
      }
      setLoading(true);
      setError("");
      try {
        const response = await fetchStudentOverview(token, i18n.language);
        setData({
          upcoming: response.upcoming || [],
          history: response.history || [],
          attention: response.attention || [],
          newTutors: response.newTutors || [],
        });
      } catch (err) {
        setError(err.message || t("app.student.overview.errors.load"));
      } finally {
        setLoading(false);
      }
    };
    loadOverview();
  }, [token, i18n.language, t]);

  const renderLesson = (lesson) => (
    <div key={lesson.id} className="overview-item">
      <div className="overview-main">
        <strong>{lesson.tutorName || t("app.student.overview.tutorUnknown")}</strong>
        {lesson.status && (
          <span className={`status status-${lesson.status.toLowerCase()}`}>
            {t(`status.${lesson.status.toLowerCase()}`, lesson.status)}
          </span>
        )}
      </div>
      <div className="overview-sub">
        {formatDate(lesson.start, i18n.language)}{" "}
        {lesson.durationMinutes ? `• ${lesson.durationMinutes} ${t("app.tutor.profile.affix.min")}` : ""}
      </div>
      {STATUS_WITH_DETAILS.has(lesson.status) && lesson.notes && (
        <div className="overview-comment">“{lesson.notes}”</div>
      )}
    </div>
  );

  const renderUpcoming = () =>
    data.upcoming.length ? (
      data.upcoming.map(renderLesson)
    ) : (
      <Alert variant="info">{t("app.student.overview.empty.upcoming")}</Alert>
    );

  const renderHistory = () =>
    data.history.length ? (
      data.history.map(renderLesson)
    ) : (
      <Alert variant="info">{t("app.student.overview.empty.history")}</Alert>
    );

  const renderAttention = () =>
    data.attention.length ? (
      data.attention.map(renderLesson)
    ) : (
      <Alert variant="success">
        {t("app.student.overview.empty.attention")}
      </Alert>
    );

  const renderNewTutors = () =>
    data.newTutors.length ? (
      data.newTutors.map((tutor) => {
        const subjects = Array.from(tutor.subjects || []).map(
          (value) => subjectLabelMap.get(value) || value
        );
        return (
          <div key={tutor.id} className="overview-item">
            <div className="overview-main">
              <strong>
                {tutor.firstName} {tutor.lastName}
              </strong>
            </div>
            <div className="overview-sub">
              {formatDate(tutor.createdAt, i18n.language)}
              {subjects.length ? ` • ${subjects.join(", ")}` : ""}
            </div>
          </div>
        );
      })
    ) : (
      <Alert variant="info">{t("app.student.overview.empty.newTutors")}</Alert>
    );

  if (loading) {
    return (
      <div className="card">
        <Alert variant="info">{t("loading", { defaultValue: "Ładowanie..." })}</Alert>
      </div>
    );
  }

  if (error) {
    return (
      <div className="card">
        <Alert variant="error">{error}</Alert>
      </div>
    );
  }

  return (
    <div className="overview-grid">
      <div className="card">
        <div className="card-head">
          <h3>{t("app.student.overview.sections.upcoming")}</h3>
        </div>
        {renderUpcoming()}
      </div>

      <div className="card">
        <div className="card-head">
          <h3>{t("app.student.overview.sections.history")}</h3>
        </div>
        {renderHistory()}
      </div>

      <div className="card">
        <div className="card-head">
          <h3>{t("app.student.overview.sections.attention")}</h3>
        </div>
        {renderAttention()}
      </div>

      <div className="card">
        <div className="card-head">
          <h3>{t("app.student.overview.sections.newTutors")}</h3>
        </div>
        {renderNewTutors()}
      </div>

      <div className="card">
        <div className="card-head">
          <h3>{t("app.student.overview.sections.quickActions")}</h3>
        </div>
        <div className="overview-item">
          <div className="overview-main">
            <strong>{t("app.student.overview.actions.bookTitle")}</strong>
          </div>
          <div className="overview-sub">
            {t("app.student.overview.actions.bookDescription")}
          </div>
          <div className="overview-actions" style={{ marginTop: "12px" }}>
            <Button onClick={() => window.location.assign("/app/student/tutors")}>
              {t("app.student.overview.actions.browseButton")}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}
