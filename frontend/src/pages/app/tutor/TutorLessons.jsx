import { useEffect, useMemo, useState } from "react";
import Button from "../../../components/ui/Button";
import Alert from "../../../components/ui/Alert";
import TutorReviewForm from "../../../components/ui/TutorReviewForm";
import { useTranslation } from "react-i18next";
import {
  fetchTutorBookings,
  confirmTutorBooking,
  declineTutorBooking,
  proposeTutorBooking,
  submitTutorReview,
} from "../../../services/lessons";
import "./tutor-lessons.css";

const DURATION_OPTIONS = [30, 60, 90, 120];

const sortByStartAsc = (list = []) =>
  [...list].sort(
    (a, b) => new Date(a.start).getTime() - new Date(b.start).getTime()
  );
const sortByStartDesc = (list = []) =>
  [...list].sort(
    (a, b) => new Date(b.start).getTime() - new Date(a.start).getTime()
  );

const getPartsFromIso = (iso) => {
  if (!iso) return { date: "", time: "" };
  const clean = iso.replace("Z", "");
  const [datePart, timePart = ""] = clean.split("T");
  return {
    date: datePart || "",
    time: timePart.slice(0, 5),
  };
};

export default function TutorLessons() {
  const { t, i18n } = useTranslation("common");
  const [requests, setRequests] = useState([]);
  const [confirmed, setConfirmed] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionLoading, setActionLoading] = useState(false);
  const [feedback, setFeedback] = useState(null);
  const [proposal, setProposal] = useState(null);
  const [reviewLesson, setReviewLesson] = useState(null);

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
  const tutorId = useMemo(() => localStorage.getItem("userId"), []);

  const locale = i18n.language === "pl" ? "pl-PL" : "en-US";

  const fmt = (iso) =>
    new Date(iso).toLocaleString(locale, {
      hour: "2-digit",
      minute: "2-digit",
      day: "2-digit",
      month: "2-digit",
      year: "numeric",
    });

  const statusLabel = (lesson) => {
    if (!lesson?.status) return "";
    return t(`status.${lesson.status.toLowerCase()}`, lesson.status);
  };

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
      setRequests(sortByStartAsc(data.requests || []));
      setConfirmed(sortByStartAsc(data.confirmed || []));
    } catch (err) {
      setError(err.message || "Failed to load bookings");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadBookings();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const groups = useMemo(() => {
    const inProgress = confirmed.filter((l) => l.status === "IN_PROGRESS");
    const rescheduled = confirmed.filter((l) => l.status === "RESCHEDULED");
    const cancelled = confirmed.filter((l) => l.status === "CANCELLED");
    const completedReviewed = confirmed.filter((l) => l.status === "COMPLETED" && l.tutorReviewSubmitted);
    const completed = confirmed.filter((l) => l.status === "COMPLETED" && !l.tutorReviewSubmitted);
    const scheduled = confirmed.filter((l) => l.status === "SCHEDULED");
    return { inProgress, rescheduled, cancelled, completedReviewed, completed, scheduled };
  }, [confirmed]);

  const applyView = (items, section) => {
    const term = (search[section] || "").toLowerCase();
    const filtered = term
      ? items.filter((l) => {
          const s = l.student || {};
          const name = `${s.firstName || ""} ${s.lastName || ""}`.toLowerCase();
          const mail = (s.email || "").toLowerCase();
          return name.includes(term) || mail.includes(term);
        })
      : items;
    const desc = order[section] === "newest";
    return desc ? sortByStartDesc(filtered) : sortByStartAsc(filtered);
  };

  const handleConfirm = async (lessonId) => {
    if (!token || !tutorId) return;
    setActionLoading(true);
    setFeedback(null);
    try {
      await confirmTutorBooking(tutorId, lessonId, token);
      setFeedback({ type: "success", message: t("app.tutor.lessons.confirmSuccess") });
      setProposal((prev) => (prev?.lessonId === lessonId ? null : prev));
      await loadBookings();
    } catch (err) {
      setFeedback({ type: "error", message: err.message || t("app.tutor.lessons.confirmError") });
    } finally {
      setActionLoading(false);
    }
  };

  const handleDecline = async (lesson) => {
    if (!token || !tutorId) return;
    const reason = window.prompt(t("app.tutor.lessons.declinePrompt"), "");
    if (reason === null) {
      return;
    }
    setActionLoading(true);
    setFeedback(null);
    try {
      await declineTutorBooking(
        tutorId,
        lesson.id,
        token,
        reason ? { reason } : {},
        i18n.language
      );
      setFeedback({ type: "success", message: t("app.tutor.lessons.declineSuccess") });
      setProposal((prev) => (prev?.lessonId === lesson.id ? null : prev));
      await loadBookings();
    } catch (err) {
      setFeedback({ type: "error", message: err.message || t("app.tutor.lessons.declineError") });
    } finally {
      setActionLoading(false);
    }
  };

  const openProposal = (lesson) => {
    const { date, time } = getPartsFromIso(lesson.start);
    if (proposal?.lessonId === lesson.id) {
      setProposal(null);
      return;
    }
    setProposal({
      lessonId: lesson.id,
      date,
      time,
      duration: Number(lesson.durationMinutes) || 60,
      note: "",
      studentName: `${lesson.student?.firstName || ""} ${lesson.student?.lastName || ""}`.trim(),
    });
  };

  const handleProposalSubmit = async (event) => {
    event.preventDefault();
    if (!token || !tutorId || !proposal) return;
    if (!proposal.date || !proposal.time) {
      setFeedback({ type: "error", message: t("app.tutor.lessons.proposeError") });
      return;
    }

    setActionLoading(true);
    setFeedback(null);
    try {
      await proposeTutorBooking(
        tutorId,
        proposal.lessonId,
        {
          start: `${proposal.date}T${proposal.time}`,
          durationMinutes: Number(proposal.duration),
          note: proposal.note,
        },
        token,
        i18n.language
      );
      setFeedback({ type: "success", message: t("app.tutor.lessons.proposeSuccess") });
      setProposal(null);
      await loadBookings();
    } catch (err) {
      setFeedback({ type: "error", message: err.message || t("app.tutor.lessons.proposeError") });
    } finally {
      setActionLoading(false);
    }
  };

  const openTutorReview = (lesson) => {
    const student = lesson.student || {};
    setReviewLesson({
      id: lesson.id,
      studentName: `${student.firstName || ""} ${student.lastName || ""}`.trim(),
      lessonDate: fmt(lesson.start),
    });
  };

  const handleTutorReviewSubmit = async ({ studentRating, platformRating, comment }) => {
    if (!reviewLesson || !token || !tutorId) return;
    setActionLoading(true);
    setFeedback(null);
    try {
      await submitTutorReview(
        reviewLesson.id,
        {
          tutorId,
          studentRating,
          platformRating,
          comment,
        },
        token,
        i18n.language
      );
      setFeedback({ type: "success", message: t("app.tutor.lessons.reviewSuccess") });
      setReviewLesson(null);
      await loadBookings();
    } catch (err) {
      setFeedback({ type: "error", message: err.message || t("app.tutor.lessons.reviewError") });
    } finally {
      setActionLoading(false);
    }
  };

  const renderControls = (sectionKey) => (
    <div className="filters-grid" style={{ marginBottom: 8 }}>
      <div className="field">
        <span className="icon" aria-hidden="true" role="presentation">🔎</span>
        <input
          className="input"
          type="text"
          placeholder={t("app.tutor.lessons.list.searchPlaceholder")}
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
          <option value="newest">{t("app.tutor.lessons.list.sortNewest")}</option>
          <option value="oldest">{t("app.tutor.lessons.list.sortOldest")}</option>
        </select>
      </div>
    </div>
  );

  const renderRequest = (lesson) => {
    const student = lesson.student || {};
    const isProposing = proposal?.lessonId === lesson.id;
    const deliveryMode = lesson.deliveryMode;
    const isOnline = deliveryMode === "ONLINE";
    const locationLine = [lesson.onsiteStreet, lesson.onsiteBuilding, lesson.onsiteApartment]
      .filter(Boolean)
      .join(" ");
    const cityLine = [lesson.onsitePostalCode, lesson.onsiteCity].filter(Boolean).join(" ");

    return (
      <div key={lesson.id} className="tutor-lesson-row">
        <div className="tutor-lesson-header">
          <strong>
            {student.firstName} {student.lastName}
          </strong>
          <span>{fmt(lesson.start)}</span>
          {lesson.status && (
            <span className={`tutor-lesson-status status-${lesson.status.toLowerCase()}`}>
              {statusLabel(lesson)}
            </span>
          )}
        </div>
        <div className="tutor-lesson-meta">{student.email}</div>
        <div className="tutor-lesson-meta">
          {t("app.tutor.lessons.lessonLength")}: {lesson.durationMinutes}{" "}
          {t("app.tutor.profile.affix.min", { defaultValue: "min" })}
        </div>
        {deliveryMode && (
          <div className="tutor-lesson-meta">
            {t(`app.tutor.lessons.mode.${deliveryMode.toLowerCase()}`, deliveryMode)}
          </div>
        )}
        {!isOnline && (locationLine || cityLine) && (
          <div className="tutor-lesson-location">
            <strong>{t("app.tutor.lessons.locationLabel")}:</strong>{" "}
            {[locationLine, cityLine].filter(Boolean).join(", ")}
          </div>
        )}
        {lesson.notes && (
          <div className="tutor-lesson-notes">
            <div style={{ fontWeight: 600, marginBottom: 4 }}>
              {t("app.tutor.lessons.studentMessage")}
            </div>
            {lesson.notes}
          </div>
        )}
        <div className="tutor-lesson-actions">
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
            variant="danger"
            disabled={actionLoading}
            onClick={() => handleDecline(lesson)}
          >
            {t("app.tutor.lessons.actions.decline")}
          </Button>
          <Button
            size="small"
            variant="secondary"
            disabled={actionLoading}
            onClick={() => openProposal(lesson)}
          >
            {t("app.tutor.lessons.propose")}
          </Button>
        </div>

        {isProposing && (
          <form className="proposal-form" onSubmit={handleProposalSubmit}>
            <h4>{t("app.tutor.lessons.proposeFormTitle", { student: proposal.studentName })}</h4>
            <div className="proposal-grid">
              <label>
                {t("app.tutor.lessons.proposeDate")}
                <input
                  type="date"
                  value={proposal.date}
                  onChange={(e) =>
                    setProposal((prev) => ({ ...prev, date: e.target.value }))
                  }
                />
              </label>
              <label>
                {t("app.tutor.lessons.proposeTime")}
                <input
                  type="time"
                  value={proposal.time}
                  onChange={(e) =>
                    setProposal((prev) => ({ ...prev, time: e.target.value }))
                  }
                />
              </label>
              <label>
                {t("app.tutor.lessons.proposeDuration")}
                <select
                  value={proposal.duration}
                  onChange={(e) =>
                    setProposal((prev) => ({
                      ...prev,
                      duration: Number(e.target.value),
                    }))
                  }
                >
                  {[...new Set([...DURATION_OPTIONS, proposal.duration])].map((option) => (
                    <option key={option} value={option}>
                      {option} {t("app.student.tutors.detail.minutesSuffix", { defaultValue: "min" })}
                    </option>
                  ))}
                </select>
              </label>
              <label style={{ gridColumn: "1 / -1" }}>
                {t("app.tutor.lessons.proposeNote")}
                <textarea
                  value={proposal.note}
                  onChange={(e) =>
                    setProposal((prev) => ({ ...prev, note: e.target.value }))
                  }
                  rows={3}
                />
              </label>
            </div>
            <div className="proposal-actions">
              <Button type="submit" size="small" variant="primary" disabled={actionLoading}>
                {t("app.tutor.lessons.proposeSubmit")}
              </Button>
              <Button
                type="button"
                size="small"
                variant="secondary"
                onClick={() => setProposal(null)}
              >
                {t("app.tutor.lessons.proposeCancel")}
              </Button>
            </div>
          </form>
        )}
      </div>
    );
  };

  const renderConfirmed = (lesson) => {
    const student = lesson.student || {};
    const isProposing = proposal?.lessonId === lesson.id;
    const deliveryMode = lesson.deliveryMode;
    const isOnline = deliveryMode === "ONLINE";
    const locationLine = [lesson.onsiteStreet, lesson.onsiteBuilding, lesson.onsiteApartment]
      .filter(Boolean)
      .join(" ");
    const cityLine = [lesson.onsitePostalCode, lesson.onsiteCity].filter(Boolean).join(" ");
    const hasMeetingLink = !!lesson.meetingLink;

    return (
      <div key={lesson.id} className="tutor-lesson-row">
        <div className="tutor-lesson-header">
          <strong>
            {student.firstName} {student.lastName}
          </strong>
          <span>{fmt(lesson.start)}</span>
          {lesson.status && (
            <span className={`tutor-lesson-status status-${lesson.status.toLowerCase()}`}>
              {statusLabel(lesson)}
            </span>
          )}
        </div>
        <div className="tutor-lesson-meta">{student.email}</div>
        <div className="tutor-lesson-meta">
          {t("app.tutor.lessons.lessonLength")}: {lesson.durationMinutes}{" "}
          {t("app.tutor.profile.affix.min", { defaultValue: "min" })}
        </div>
        {deliveryMode && (
          <div className="tutor-lesson-meta">
            {t(`app.tutor.lessons.mode.${deliveryMode.toLowerCase()}`, deliveryMode)}
          </div>
        )}
        {!isOnline && (locationLine || cityLine) && (
          <div className="tutor-lesson-location">
            <strong>{t("app.tutor.lessons.locationLabel")}:</strong>{" "}
            {[locationLine, cityLine].filter(Boolean).join(", ")}
          </div>
        )}
        {lesson.notes && (
          <div className="tutor-lesson-notes">
            {lesson.notes}
          </div>
        )}
        <div className="tutor-lesson-actions">
          {hasMeetingLink ? (
            <a
              href={lesson.meetingLink}
              target="_blank"
              rel="noopener noreferrer"
              className="tutor-lesson-join"
            >
              🔗 {t("app.tutor.lessons.joinMeeting")}
            </a>
          ) : (
            <span className="tutor-lesson-join-disabled">
              {t("app.tutor.lessons.joinUnavailableHost")}
            </span>
          )}
          <Button
            size="small"
            variant="secondary"
            disabled={actionLoading}
            onClick={() => openProposal(lesson)}
          >
            {t("app.tutor.lessons.propose")}
          </Button>
          <Button
            size="small"
            variant="danger"
            disabled={actionLoading}
            onClick={() => handleDecline(lesson)}
          >
            {t("app.tutor.lessons.actions.cancel")}
          </Button>
        </div>

        {isProposing && (
          <form className="proposal-form" onSubmit={handleProposalSubmit}>
            <h4>{t("app.tutor.lessons.proposeFormTitle", { student: proposal.studentName })}</h4>
            <div className="proposal-grid">
              <label>
                {t("app.tutor.lessons.proposeDate")}
                <input
                  type="date"
                  value={proposal.date}
                  onChange={(e) =>
                    setProposal((prev) => ({ ...prev, date: e.target.value }))
                  }
                />
              </label>
              <label>
                {t("app.tutor.lessons.proposeTime")}
                <input
                  type="time"
                  value={proposal.time}
                  onChange={(e) =>
                    setProposal((prev) => ({ ...prev, time: e.target.value }))
                  }
                />
              </label>
              <label>
                {t("app.tutor.lessons.proposeDuration")}
                <select
                  value={proposal.duration}
                  onChange={(e) =>
                    setProposal((prev) => ({
                      ...prev,
                      duration: Number(e.target.value),
                    }))
                  }
                >
                  {[...new Set([...DURATION_OPTIONS, proposal.duration])].map((option) => (
                    <option key={option} value={option}>
                      {option} {t("app.student.tutors.detail.minutesSuffix", { defaultValue: "min" })}
                    </option>
                  ))}
                </select>
              </label>
              <label style={{ gridColumn: "1 / -1" }}>
                {t("app.tutor.lessons.proposeNote")}
                <textarea
                  value={proposal.note}
                  onChange={(e) =>
                    setProposal((prev) => ({ ...prev, note: e.target.value }))
                  }
                  rows={3}
                />
              </label>
            </div>
            <div className="proposal-actions">
              <Button type="submit" size="small" variant="primary" disabled={actionLoading}>
                {t("app.tutor.lessons.proposeSubmit")}
              </Button>
              <Button
                type="button"
                size="small"
                variant="secondary"
                onClick={() => setProposal(null)}
              >
                {t("app.tutor.lessons.proposeCancel")}
              </Button>
            </div>
          </form>
        )}
      </div>
    );
  };

  return (
    <>
      <div className="card tutor-lessons-card">
        <h3>{t("app.tutor.lessons.section.awaiting")}</h3>
        {renderControls("awaiting")}
        {feedback && <Alert variant={feedback.type === "success" ? "success" : "error"}>{feedback.message}</Alert>}
        {loading && <Alert variant="info">{t("loading", { defaultValue: "Loading..." })}</Alert>}
        {error && !loading && <Alert variant="error">{error}</Alert>}
        {!loading && !requests.length && !error && (
          <Alert variant="info">{t("app.tutor.lessons.noRequests")}</Alert>
        )}
        {applyView(requests, "awaiting").map(renderRequest)}
      </div>

      <div className="card tutor-lessons-card">
        <h3>{t("app.tutor.lessons.section.inProgress")}</h3>
        {renderControls("inProgress")}
        {!groups.inProgress.length && <Alert variant="info">{t("app.tutor.lessons.empty")}</Alert>}
        {applyView(groups.inProgress, "inProgress").map(renderConfirmed)}
      </div>

      <div className="card tutor-lessons-card">
        <h3>{t("app.tutor.lessons.section.scheduled")}</h3>
        {renderControls("scheduled")}
        {!groups.scheduled.length && <Alert variant="info">{t("app.tutor.lessons.empty")}</Alert>}
        {applyView(groups.scheduled, "scheduled").map(renderConfirmed)}
      </div>

      <div className="card tutor-lessons-card">
        <h3>{t("app.tutor.lessons.section.rescheduled")}</h3>
        {renderControls("rescheduled")}
        {!groups.rescheduled.length && <Alert variant="info">{t("app.tutor.lessons.empty")}</Alert>}
        {applyView(groups.rescheduled, "rescheduled").map(renderConfirmed)}
      </div>

      <div className="card tutor-lessons-card">
        <h3>{t("app.tutor.lessons.section.completed")}</h3>
        {renderControls("completed")}
        {!groups.completed.length && <Alert variant="info">{t("app.tutor.lessons.empty")}</Alert>}
        {applyView(groups.completed, "completed").map(renderConfirmed)}
      </div>

      <div className="card tutor-lessons-card">
        <h3>{t("app.tutor.lessons.section.reviewed")}</h3>
        {renderControls("reviewed")}
        {!groups.completedReviewed.length && <Alert variant="info">{t("app.tutor.lessons.empty")}</Alert>}
        {applyView(groups.completedReviewed, "reviewed").map(renderConfirmed)}
      </div>

      <div className="card tutor-lessons-card">
        <h3>{t("app.tutor.lessons.section.cancelled")}</h3>
        {renderControls("cancelled")}
        {!groups.cancelled.length && <Alert variant="info">{t("app.tutor.lessons.empty")}</Alert>}
        {applyView(groups.cancelled, "cancelled").map(renderConfirmed)}
      </div>

      {reviewLesson && (
        <TutorReviewForm
          studentName={reviewLesson.studentName}
          lessonDate={reviewLesson.lessonDate}
          onClose={() => setReviewLesson(null)}
          onSubmit={handleTutorReviewSubmit}
        />
      )}
    </>
  );
}
