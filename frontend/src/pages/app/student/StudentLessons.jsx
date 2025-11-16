import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import Button from "../../../components/ui/Button";
import ReviewForm from "../../../components/ui/ReviewForm";
import Alert from "../../../components/ui/Alert";
import {
  fetchStudentLessons,
  cancelStudentLesson,
  rescheduleStudentLesson,
} from "../../../services/lessons";
import "./student-lessons.css";

const DURATION_OPTIONS = [30, 60, 90, 120];

const isCompleted = (lesson) => {
  if (!lesson?.end && !lesson?.start) return false;
  const end = lesson?.end ? new Date(lesson.end).getTime() : new Date(lesson.start).getTime();
  return Date.now() > end;
};

const getPartsFromIso = (iso) => {
  if (!iso) return { date: "", time: "" };
  const clean = iso.replace("Z", "");
  const [datePart, timePart = ""] = clean.split("T");
  return {
    date: datePart || "",
    time: timePart.slice(0, 5),
  };
};

const sortByStartAsc = (list = []) =>
  [...list].sort((a, b) => new Date(a.start).getTime() - new Date(b.start).getTime());
const sortByStartDesc = (list = []) =>
  [...list].sort((a, b) => new Date(b.start).getTime() - new Date(a.start).getTime());

export default function StudentLessons() {
  const { t, i18n } = useTranslation("common");
  const [showReviewForm, setShowReviewForm] = useState(null);
  const [lessons, setLessons] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState(null);
  const [rescheduleForm, setRescheduleForm] = useState(null);

  // Per-section search and sort
  const [search, setSearch] = useState({
    awaiting: "",
    inProgress: "",
    scheduled: "",
    rescheduled: "",
    completed: "",
    reviewed: "",
    cancelled: ""
  });
  const [order, setOrder] = useState({
    awaiting: "newest",
    inProgress: "newest",
    scheduled: "newest",
    rescheduled: "newest",
    completed: "newest",
    reviewed: "newest",
    cancelled: "newest"
  });

  const token = useMemo(
    () => localStorage.getItem("token") || localStorage.getItem("access_token"),
    []
  );

  const locale = i18n.language === "pl" ? "pl-PL" : "en-US";

  const loadLessons = async () => {
    if (!token) return;
    setLoading(true);
    setError("");
    try {
      const data = await fetchStudentLessons(token, i18n.language);
      setLessons(data.lessons || []);
    } catch (err) {
      setError(err.message || t("app.student.lessons.rescheduleError"));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLessons();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, i18n.language]);

  const groups = useMemo(() => {
    const awaiting = lessons.filter((l) => l.status === "REQUESTED");
    const inProgress = lessons.filter((l) => l.status === "IN_PROGRESS");
    const scheduled = lessons.filter((l) => l.status === "SCHEDULED");
    const rescheduled = lessons.filter((l) => l.status === "RESCHEDULED");
    const cancelled = lessons.filter((l) => l.status === "CANCELLED");
    const reviewed = lessons.filter((l) => l.status === "COMPLETED" && l.studentReviewSubmitted);
    const completed = lessons.filter((l) => l.status === "COMPLETED" && !l.studentReviewSubmitted);
    return { awaiting, inProgress, scheduled, rescheduled, completed, reviewed, cancelled };
  }, [lessons]);

  const applyView = (items, section) => {
    const term = (search[section] || "").toLowerCase();
    const filtered = term
      ? items.filter((l) => (l.tutorName || "").toLowerCase().includes(term))
      : items;
    const desc = order[section] === "newest";
    return desc ? sortByStartDesc(filtered) : sortByStartAsc(filtered);
  };

  const handleReviewSubmit = async (reviewData) => {
    try {
      const tokenValue = localStorage.getItem("token");
      const userId = localStorage.getItem("userId");

      const response = await fetch("/api/reviews", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${tokenValue}`,
        },
        body: JSON.stringify({
          lessonId: showReviewForm.lessonId,
          studentId: userId,
          tutorRating: reviewData.rating,
          platformRating: reviewData.platformRating,
          comment: reviewData.comment,
        }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        console.error("Failed to submit review:", errorData);
        setFeedback({ type: "error", message: t("app.student.lessons.reviewError") });
        return;
      }

      setShowReviewForm(null);
      setFeedback({ type: "success", message: t("app.student.lessons.reviewSuccess") });
      loadLessons();
    } catch (error) {
      console.error("Error submitting review:", error);
      setFeedback({ type: "error", message: t("app.student.lessons.reviewError") });
    }
  };

  const handleCancelLesson = async (lesson) => {
    if (!token) return;
    if (!window.confirm(t("app.student.lessons.cancelConfirm"))) {
      return;
    }
    try {
      await cancelStudentLesson(lesson.id, token, {}, i18n.language);
      setFeedback({ type: "success", message: t("app.student.lessons.cancelSuccess") });
      loadLessons();
    } catch (err) {
      setFeedback({ type: "error", message: err.message || t("app.student.lessons.cancelError") });
    }
  };

  const openRescheduleForm = (lesson) => {
    const { date, time } = getPartsFromIso(lesson.start);
    setRescheduleForm({
      lessonId: lesson.id,
      date,
      time,
      duration: lesson.durationMinutes || 60,
      status: lesson.status,
      tutorName: lesson.tutorName,
    });
  };

  const handleRescheduleSubmit = async (event) => {
    event.preventDefault();
    if (!token || !rescheduleForm) return;
    if (!rescheduleForm.date || !rescheduleForm.time) {
      setFeedback({ type: "error", message: t("app.student.lessons.rescheduleError") });
      return;
    }
    try {
      await rescheduleStudentLesson(
        rescheduleForm.lessonId,
        {
          start: `${rescheduleForm.date}T${rescheduleForm.time}`,
          durationMinutes: Number(rescheduleForm.duration),
        },
        token,
        i18n.language
      );
      setFeedback({ type: "success", message: t("app.student.lessons.rescheduleSuccess") });
      setRescheduleForm(null);
      loadLessons();
    } catch (err) {
      setFeedback({ type: "error", message: err.message || t("app.student.lessons.rescheduleError") });
    }
  };

  const formatDate = (iso) => {
    if (!iso) return "";
    return new Date(iso).toLocaleString(locale, {
      year: "numeric",
      month: "long",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const statusLabel = (lesson) => {
    if (!lesson?.status) return "";
    const key = lesson.status.toLowerCase();
    return t(`status.${key}`, lesson.status);
  };

  const renderControls = (sectionKey) => (
    <div className="filters-grid" style={{ marginBottom: 8 }}>
      <div className="field">
        <span className="icon" aria-hidden="true" role="presentation">🔎</span>
        <input
          className="input"
          type="text"
          placeholder={t("app.student.lessons.list.searchPlaceholder")}
          value={search[sectionKey]}
          onChange={(e) => setSearch((s) => ({ ...s, [sectionKey]: e.target.value }))}
          style={{ flex: 1 }}
        />
      </div>
      <div className="field" style={{ maxWidth: 220 }}>
        <span className="icon" aria-hidden="true" role="presentation">↕️</span>
        <select
          className="input"
          value={order[sectionKey]}
          onChange={(e) => setOrder((o) => ({ ...o, [sectionKey]: e.target.value }))}
        >
          <option value="newest">{t("app.student.lessons.list.sortNewest")}</option>
          <option value="oldest">{t("app.student.lessons.list.sortOldest")}</option>
        </select>
      </div>
    </div>
  );

  const renderRow = (lesson) => {
    const completed = isCompleted(lesson);
    const hasMeetingLink = !!lesson.meetingLink;
    const rescheduling = rescheduleForm?.lessonId === lesson.id;
    const deliveryMode = lesson.deliveryMode;
    const isOnline = deliveryMode === "ONLINE";
    const showJoinButton = isOnline && hasMeetingLink;
    const alreadyReviewed = lesson.studentReviewSubmitted;
    const locationLine = [lesson.onsiteStreet, lesson.onsiteBuilding, lesson.onsiteApartment]
      .filter(Boolean)
      .join(" ");
    const cityLine = [lesson.onsitePostalCode, lesson.onsiteCity].filter(Boolean).join(" ");

    return (
      <div key={lesson.id} className="student-lesson-row">
        <div className="student-lesson-main">
          <div className="student-lesson-title">
            <strong>{lesson.tutorName}</strong>
            {lesson.durationMinutes && (
              <span className="student-lesson-duration">
                {lesson.durationMinutes} {t("app.student.tutors.detail.minutesSuffix", { defaultValue: "min" })}
              </span>
            )}
            {lesson.status && <span className={`student-lesson-status status-${lesson.status.toLowerCase()}`}>{statusLabel(lesson)}</span>}
          </div>
          <div className="student-lesson-subtitle">
            {formatDate(lesson.start)}
          </div>
          {deliveryMode && (
            <div className={`student-lesson-mode student-lesson-mode--${deliveryMode.toLowerCase()}`}>
              {isOnline
                ? t("app.student.lessons.mode.online")
                : t("app.student.lessons.mode.onsite")}
            </div>
          )}
          {!isOnline && (locationLine || cityLine) && (
            <div className="student-lesson-location">
              <span className="student-lesson-location-label">
                {t("app.student.lessons.locationLabel")}:
              </span>
              <span className="student-lesson-location-value">
                {[locationLine, cityLine].filter(Boolean).join(", ")}
              </span>
            </div>
          )}
          {lesson.notes && (
            <div className="student-lesson-notes">
              {lesson.notes}
            </div>
          )}
        </div>

        <div className="student-lesson-actions">
          {showJoinButton ? (
            <a
              href={lesson.meetingLink}
              target="_blank"
              rel="noopener noreferrer"
              className="lesson-join-btn"
            >
              🔗 {t("app.student.lessons.joinMeeting")}
            </a>
          ) : (
            <span className="lesson-join-placeholder">
              {isOnline
                ? t("app.student.lessons.joinUnavailable")
                : t("app.student.lessons.joinInPerson")}
            </span>
          )}

          {completed && !alreadyReviewed ? (
            <Button
              size="small"
              variant="primary"
              onClick={() =>
                setShowReviewForm({
                  lessonId: lesson.id,
                  tutorName: lesson.tutorName,
                  lessonDate: formatDate(lesson.start),
                })
              }
            >
              ⭐ {t("app.student.lessons.leaveReview")}
            </Button>
          ) : completed && alreadyReviewed ? (
            <span className="lesson-join-placeholder">
              {t("app.student.lessons.reviewSubmitted")}
            </span>
          ) : (
            <>
              <Button
                size="small"
                variant="secondary"
                onClick={() => openRescheduleForm(lesson)}
              >
                {t("app.student.lessons.reschedule")}
              </Button>
              <Button
                size="small"
                variant="danger"
                onClick={() => handleCancelLesson(lesson)}
              >
                {t("app.student.lessons.cancel")}
              </Button>
            </>
          )}
        </div>

        {rescheduling && (
          <form className="reschedule-form" onSubmit={handleRescheduleSubmit}>
            <h4>{t("app.student.lessons.rescheduleFormTitle")}</h4>
            <div className="reschedule-grid">
              <label>
                {t("app.student.lessons.rescheduleDate")}
                <input
                  type="date"
                  value={rescheduleForm.date}
                  onChange={(e) =>
                    setRescheduleForm((prev) => ({ ...prev, date: e.target.value }))
                  }
                />
              </label>
              <label>
                {t("app.student.lessons.rescheduleTime")}
                <input
                  type="time"
                  value={rescheduleForm.time}
                  onChange={(e) =>
                    setRescheduleForm((prev) => ({ ...prev, time: e.target.value }))
                  }
                />
              </label>
              <label>
                {t("app.student.lessons.rescheduleDuration")}
                <select
                  value={rescheduleForm.duration}
                  onChange={(e) =>
                    setRescheduleForm((prev) => ({
                      ...prev,
                      duration: Number(e.target.value),
                    }))
                  }
                >
                  {[...new Set([...DURATION_OPTIONS, rescheduleForm.duration])].map((option) => (
                    <option key={option} value={option}>
                      {option} {t("app.student.tutors.detail.minutesSuffix", { defaultValue: "min" })}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <div className="reschedule-actions">
              <Button type="submit" variant="primary" size="small">
                {t("app.student.lessons.rescheduleSave")}
              </Button>
              <Button
                type="button"
                size="small"
                variant="secondary"
                onClick={() => setRescheduleForm(null)}
              >
                {t("app.student.lessons.rescheduleCancel")}
              </Button>
            </div>
          </form>
        )}
      </div>
    );
  };

  return (
    <>
      <div className="card student-lessons-card">
        <h3>{t("app.student.lessons.section.awaiting")}</h3>
        {renderControls("awaiting")}
        {feedback && (
          <Alert variant={feedback.type === "success" ? "success" : "error"}>
            {feedback.message}
          </Alert>
        )}
        {loading && <Alert variant="info">{t("loading", { defaultValue: "Loading..." })}</Alert>}
        {error && <Alert variant="error">{error}</Alert>}
        {!loading && !groups.awaiting.length && (
          <div className="student-lessons-empty">{t("app.student.lessons.noLessons")}</div>
        )}
        <div className="student-lessons-list">
          {applyView(groups.awaiting, "awaiting").map(renderRow)}
        </div>
      </div>

      <div className="card student-lessons-card">
        <h3>{t("app.student.lessons.section.inProgress")}</h3>
        {renderControls("inProgress")}
        {!groups.inProgress.length && <Alert variant="info">{t("empty", { defaultValue: "Empty" })}</Alert>}
        <div className="student-lessons-list">
          {applyView(groups.inProgress, "inProgress").map(renderRow)}
        </div>
      </div>

      <div className="card student-lessons-card">
        <h3>{t("app.student.lessons.section.scheduled")}</h3>
        {renderControls("scheduled")}
        {!groups.scheduled.length && <Alert variant="info">{t("empty", { defaultValue: "Empty" })}</Alert>}
        <div className="student-lessons-list">
          {applyView(groups.scheduled, "scheduled").map(renderRow)}
        </div>
      </div>

      <div className="card student-lessons-card">
        <h3>{t("app.student.lessons.section.rescheduled")}</h3>
        {renderControls("rescheduled")}
        {!groups.rescheduled.length && <Alert variant="info">{t("empty", { defaultValue: "Empty" })}</Alert>}
        <div className="student-lessons-list">
          {applyView(groups.rescheduled, "rescheduled").map(renderRow)}
        </div>
      </div>

      <div className="card student-lessons-card">
        <h3>{t("app.student.lessons.section.completed")}</h3>
        {renderControls("completed")}
        {!groups.completed.length && <Alert variant="info">{t("empty", { defaultValue: "Empty" })}</Alert>}
        <div className="student-lessons-list">
          {applyView(groups.completed, "completed").map(renderRow)}
        </div>
      </div>

      <div className="card student-lessons-card">
        <h3>{t("app.student.lessons.section.reviewed")}</h3>
        {renderControls("reviewed")}
        {!groups.reviewed.length && <Alert variant="info">{t("empty", { defaultValue: "Empty" })}</Alert>}
        <div className="student-lessons-list">
          {applyView(groups.reviewed, "reviewed").map(renderRow)}
        </div>
      </div>

      <div className="card student-lessons-card">
        <h3>{t("app.student.lessons.section.cancelled")}</h3>
        {renderControls("cancelled")}
        {!groups.cancelled.length && <Alert variant="info">{t("empty", { defaultValue: "Empty" })}</Alert>}
        <div className="student-lessons-list">
          {applyView(groups.cancelled, "cancelled").map(renderRow)}
        </div>
      </div>

      {showReviewForm && (
        <ReviewForm
          tutorName={showReviewForm.tutorName}
          lessonDate={showReviewForm.lessonDate}
          onClose={() => setShowReviewForm(null)}
          onSubmit={handleReviewSubmit}
        />
      )}
    </>
  );
}

