import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import "./tutor-overview.css";
import { fetchTutorOverview } from "../../../services/lessons";

const formatDate = (iso, lang) => {
  if (!iso) return "";
  return new Date(iso).toLocaleString(lang === "pl" ? "pl-PL" : "en-US", {
    day: "2-digit",
    month: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
};

export default function TutorOverview() {
  const { t, i18n } = useTranslation("common");
  const [data, setData] = useState({
    upcoming: [],
    requests: [],
    reviews: [],
    newTutors: [],
  });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const token = useMemo(
    () => localStorage.getItem("token") || localStorage.getItem("access_token"),
    []
  );
  const tutorId = useMemo(() => localStorage.getItem("userId"), []);

  useEffect(() => {
    const loadOverview = async () => {
      if (!token || !tutorId) {
        setError(t("auth.tutorOnly", { defaultValue: "Ta sekcja wymaga konta korepetytora." }));
        setLoading(false);
        return;
      }
      setLoading(true);
      setError("");
      try {
        const overview = await fetchTutorOverview(tutorId, token);
        setData({
          upcoming: overview.upcoming || [],
          requests: overview.requests || [],
          reviews: overview.reviews || [],
          newTutors: overview.newTutors || [],
        });
      } catch (err) {
        setError(err.message || "Failed to load overview");
      } finally {
        setLoading(false);
      }
    };
    loadOverview();
  }, [token, tutorId, t]);

  const renderUpcoming = () => {
    if (!data.upcoming.length) {
      return <div className="empty">{t("tutor.overview.noUpcoming")}</div>;
    }
    return data.upcoming.map((item) => (
      <div key={item.id} className="overview-item">
        <div className="overview-main">
          <strong>{item.studentName}</strong>
          <span className={`status status-${(item.status || "").toLowerCase()}`}>
            {t(`status.${item.status?.toLowerCase()}`, item.status)}
          </span>
        </div>
        <div className="overview-sub">
          {formatDate(item.start, i18n.language)} • {item.durationMinutes} {t("app.tutor.profile.affix.min")}
        </div>
      </div>
    ));
  };

  const renderRequests = () => {
    if (!data.requests.length) {
      return <div className="empty">{t("tutor.overview.noRequests")}</div>;
    }
    return data.requests.map((req) => (
      <div key={req.id} className="overview-item">
        <div className="overview-main">
          <strong>{req.studentName}</strong>
        </div>
        <div className="overview-sub">
          {formatDate(req.start, i18n.language)} • {req.durationMinutes} {t("app.tutor.profile.affix.min")}
        </div>
      </div>
    ));
  };

  const renderReviews = () => {
    if (!data.reviews.length) {
      return <div className="empty">{t("tutor.overview.noReviews")}</div>;
    }
    return data.reviews.map((review) => (
      <div key={review.id} className="overview-item">
        <div className="overview-main">
          <strong>{review.student ? `${review.student.firstName} ${review.student.lastName}` : t("tutor.overview.anonymous")}</strong>
          <span className="rating">{review.rating}★</span>
        </div>
        <div className="overview-sub">
          {formatDate(review.createdAt, i18n.language)}
        </div>
        {review.comment && (
          <div className="overview-comment">
            “{review.comment}”
          </div>
        )}
      </div>
    ));
  };

  const renderNewTutors = () => {
    if (!data.newTutors.length) {
      return <div className="empty">{t("tutor.overview.noNewTutors")}</div>;
    }
    return data.newTutors.map((tutor) => (
      <div key={tutor.id} className="overview-item">
        <div className="overview-main">
          <strong>{tutor.firstName} {tutor.lastName}</strong>
        </div>
        <div className="overview-sub">
          {formatDate(tutor.createdAt, i18n.language)}
          {tutor.subjects?.length ? ` • ${tutor.subjects.join(", ")}` : ""}
        </div>
      </div>
    ));
  };

  if (loading) {
    return <div className="card"><div className="empty">{t("loading", { defaultValue: "Ładowanie..." })}</div></div>;
  }

  if (error) {
    return <div className="card"><div className="empty" style={{ color: "#f87171" }}>{error}</div></div>;
  }

  return (
    <div className="overview-grid">
      <div className="card">
        <div className="card-head">
          <h3>{t("tutor.overview.upcomingTitle")}</h3>
        </div>
        {renderUpcoming()}
      </div>

      <div className="card">
        <div className="card-head">
          <h3>{t("tutor.overview.requestsTitle")}</h3>
          <Link to="/app/tutor/lessons" className="card-link">
            {t("tutor.overview.viewAll")}
          </Link>
        </div>
        {renderRequests()}
      </div>

      <div className="card">
        <div className="card-head">
          <h3>{t("tutor.overview.reviewsTitle")}</h3>
        </div>
        {renderReviews()}
      </div>

      <div className="card">
        <div className="card-head">
          <h3>{t("tutor.overview.newTutorsTitle")}</h3>
        </div>
        {renderNewTutors()}
      </div>
    </div>
  );
}
