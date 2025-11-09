import { useEffect, useMemo, useState } from "react";
import Button from "../../../components/ui/Button";
import { useTranslation } from "react-i18next";
import {
  fetchTutorBookings,
  confirmTutorBooking,
  declineTutorBooking
} from "../../../services/lessons";

const canModify = (startISO) => {
  const start = new Date(startISO).getTime();
  return (start - Date.now()) / 36e5 >= 24;
};

const sortByStart = (list = []) =>
  [...list].sort(
    (a, b) => new Date(a.start).getTime() - new Date(b.start).getTime()
  );

export default function TutorLessons() {
  const { t, i18n } = useTranslation("common");
  const [requests, setRequests] = useState([]);
  const [confirmed, setConfirmed] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [actionLoading, setActionLoading] = useState(false);

  const token = useMemo(
    () => localStorage.getItem("token") || localStorage.getItem("access_token"),
    []
  );
  const tutorId = useMemo(() => localStorage.getItem("userId"), []);

  const fmt = (iso) =>
    new Date(iso).toLocaleString(i18n.language === "pl" ? "pl-PL" : "en-US", {
      hour: "2-digit",
      minute: "2-digit",
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });

  const loadBookings = async () => {
    if (!token || !tutorId) {
      setError(t("auth.tutorOnly", { defaultValue: "Musisz być zalogowany jako korepetytor." }));
      setLoading(false);
      return;
    }

    setLoading(true);
    setError("");
    try {
      const data = await fetchTutorBookings(tutorId, token);
      setRequests(sortByStart(data.requests || []));
      setConfirmed(sortByStart(data.confirmed || []));
    } catch (err) {
      setError(err.message || "Failed to load bookings");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBookings();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const handleConfirm = async (lessonId) => {
    if (!token || !tutorId) return;
    setActionLoading(true);
    setActionError("");
    try {
      const updated = await confirmTutorBooking(tutorId, lessonId, token);
      setRequests((prev) => prev.filter((item) => item.id !== lessonId));
      setConfirmed((prev) => sortByStart([...prev.filter((item) => item.id !== lessonId), updated]));
    } catch (err) {
      setActionError(err.message || "Failed to confirm booking");
    } finally {
      setActionLoading(false);
    }
  };

  const handleDecline = async (lessonId) => {
    if (!token || !tutorId) return;
    setActionLoading(true);
    setActionError("");
    try {
      await declineTutorBooking(tutorId, lessonId, token);
      setRequests((prev) => prev.filter((item) => item.id !== lessonId));
    } catch (err) {
      setActionError(err.message || "Failed to decline booking");
    } finally {
      setActionLoading(false);
    }
  };

  const renderRequest = (lesson) => {
    const student = lesson.student || {};
    return (
      <div
        key={lesson.id}
        className="item"
        style={{ padding: "12px 0", borderBottom: "1px solid rgba(255,255,255,.12)" }}
      >
        <div>
          <strong>
            {student.firstName} {student.lastName}
          </strong>{" "}
          — {fmt(lesson.start)}
        </div>
        <div style={{ fontSize: 13, opacity: 0.85, marginTop: 4 }}>
          {student.email}
        </div>
        <div style={{ fontSize: 13, opacity: 0.75, marginTop: 4 }}>
          {t("app.tutor.lessons.lessonLength", {
            defaultValue: "Duration",
          })}: {lesson.durationMinutes} {t("app.tutor.profile.affix.min", { defaultValue: "min" })}
        </div>
        {lesson.notes && (
          <div style={{ marginTop: 6, fontSize: 13, opacity: 0.78 }}>
            {lesson.notes}
          </div>
        )}
        <div className="row" style={{ marginTop: 10, gap: 8 }}>
          <Button
            size="small"
            variant="primary"
            disabled={actionLoading}
            onClick={() => handleConfirm(lesson.id)}
          >
            {t("app.tutor.lessons.actions.confirm")}
          </Button>
          <Button
            size="small"
            disabled={actionLoading}
            onClick={() => handleDecline(lesson.id)}
          >
            {t("app.tutor.lessons.actions.decline")}
          </Button>
        </div>
      </div>
    );
  };

  const renderConfirmed = (lesson) => {
    const student = lesson.student || {};
    return (
      <div
        key={lesson.id}
        className="item"
        style={{ padding: "12px 0", borderBottom: "1px solid rgba(255,255,255,.12)" }}
      >
        <div>
          <strong>
            {student.firstName} {student.lastName}
          </strong>{" "}
          — {fmt(lesson.start)}
        </div>
        <div style={{ fontSize: 13, opacity: 0.85, marginTop: 4 }}>
          {student.email}
        </div>
        <div style={{ fontSize: 13, opacity: 0.75, marginTop: 4 }}>
          {t("app.tutor.lessons.lessonLength", {
            defaultValue: "Duration",
          })}: {lesson.durationMinutes} {t("app.tutor.profile.affix.min", { defaultValue: "min" })}
        </div>
        <div className="row" style={{ marginTop: 8, gap: 8 }}>
          <Button
            size="small"
            disabled={!canModify(lesson.start)}
            onClick={() => {}}
          >
            {t("app.tutor.lessons.actions.reschedule")}
          </Button>
          <Button size="small" onClick={() => {}}>
            {t("app.tutor.lessons.actions.cancel")}
          </Button>
        </div>
      </div>
    );
  };

  return (
    <>
      <div className="card">
        <h3>{t("app.tutor.lessons.requestsTitle")}</h3>
        {loading && <div className="empty">{t("loading", { defaultValue: "Loading..." })}</div>}
        {error && !loading && <div className="empty" style={{ color: "#f87171" }}>{error}</div>}
        {actionError && (
          <div className="empty" style={{ color: "#f87171" }}>
            {actionError}
          </div>
        )}
        {!loading && !requests.length && !error && (
          <div className="empty">{t("app.tutor.lessons.noRequests")}</div>
        )}
        {requests.map(renderRequest)}
      </div>

      <div className="card">
        <h3>{t("app.tutor.lessons.confirmedTitle")}</h3>
        {loading && <div className="empty">{t("loading", { defaultValue: "Loading..." })}</div>}
        {error && !loading && <div className="empty" style={{ color: "#f87171" }}>{error}</div>}
        {!loading && !confirmed.length && !error && (
          <div className="empty">{t("app.tutor.lessons.empty")}</div>
        )}
        {confirmed.map(renderConfirmed)}
      </div>
    </>
  );
}
