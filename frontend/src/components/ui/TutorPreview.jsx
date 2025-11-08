import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import CalendarPro from "./CalendarPro";
import Button from "./Button";
import "./tutor-preview.css";

export default function TutorPreview({
  tutor,
  visible,
  onClose,
  loading = false,
  busyEvents = [],
  busyLoading = false,
  busyError = "",
  onBook,
  bookingState = { status: "idle" },
}) {
  const { t, i18n } = useTranslation("common");
  const tt = (key, params) => t(`app.student.tutors.detail.${key}`, params);
  const dayLabel = (key) => t(`app.tutor.profile.days.${key}`);

  const [date, setDate] = useState("");
  const [time, setTime] = useState("");
  const [notes, setNotes] = useState("");
  const [formError, setFormError] = useState("");

  const durationMinutes = tutor?.lessonDuration || 60;

  useEffect(() => {
    if (visible) {
      setDate("");
      setTime("");
      setNotes("");
      setFormError("");
    }
  }, [visible, tutor?.id]);

  const preferredDays = useMemo(() => {
    if (!tutor?.preferredDays || tutor.preferredDays.length === 0) {
      return [];
    }
    return tutor.preferredDays.map((d) => dayLabel(d) || d.toUpperCase());
  }, [tutor?.preferredDays, t]);

  const languages = useMemo(() => {
    if (!tutor?.teachingLanguages?.length) return "";
    return tutor.teachingLanguages.join(", ");
  }, [tutor?.teachingLanguages]);

  const subjects = useMemo(() => {
    if (!tutor?.subjects?.length) return "";
    return tutor.subjects.join(", ");
  }, [tutor?.subjects]);

  const lessonModes = useMemo(() => {
    if (!tutor?.lessonModes?.length) return "";
    return tutor.lessonModes
      .map((mode) =>
        t(`app.student.tutors.detail.lessonModeLabels.${mode}`, {
          defaultValue: t(`app.tutor.profile.modesLabels.${mode}`, { defaultValue: mode }),
        })
      )
      .filter(Boolean)
      .join(", ");
  }, [tutor?.lessonModes, t]);

  const scheduleMessage = useMemo(() => {
    if (busyLoading) {
      return tt("busyLoading");
    }
    if (busyError) {
      return tt("busyError");
    }
    if (!busyEvents.length) {
      return tt("scheduleEmpty");
    }
    return "";
  }, [busyLoading, busyError, busyEvents.length, tt]);

  const bookingMessage = useMemo(() => {
    if (bookingState?.status === "success") {
      return { tone: "success", text: tt("bookSuccess") };
    }
    if (bookingState?.status === "error") {
      return {
        tone: "error",
        text: tt("bookError", { error: bookingState.message ?? "" }),
      };
    }
    if (bookingState?.status === "unauthenticated") {
      return { tone: "warn", text: tt("requiresLogin") };
    }
    if (bookingState?.status === "loading") {
      return { tone: "info", text: tt("bookingSubmitting") };
    }
    return null;
  }, [bookingState, tt]);

  const disableBooking =
    bookingState?.status === "loading" || bookingState?.status === "success";

  const handleSubmit = (event) => {
    event.preventDefault();
    if (!date || !time) {
      setFormError(tt("formRequired"));
      return;
    }
    setFormError("");
    onBook?.({
      start: `${date}T${time}`,
      durationMinutes,
      notes: notes.trim(),
    });
  };

  if (!visible) {
    return null;
  }

  const calendarLocale = i18n.language === "pl" ? "pl" : "en";

  return (
    <div className="tutor-preview-backdrop" role="dialog" aria-modal="true">
      <div className="tutor-preview-card">
        <button
          className="tutor-preview-close"
          onClick={onClose}
          aria-label={tt("close")}
        >
          ×
        </button>

        <div className="tutor-preview-body">
          <div className="tutor-preview-column tutor-preview-column--info">
            <div className="tutor-preview-header">
              <div className="tutor-preview-photo">
                {tutor?.photoUrl ? (
                  <img src={tutor.photoUrl} alt={`${tutor.firstName} ${tutor.lastName}`} />
                ) : (
                  <div aria-hidden="true">{tutor?.firstName?.[0] ?? "?"}</div>
                )}
              </div>
              <div>
                <h2>
                  {tutor?.firstName} {tutor?.lastName}
                </h2>
                {tutor?.city && <p className="tutor-preview-city">📍 {tutor.city}</p>}
              </div>
            </div>

            {loading && (
              <p className="tutor-preview-status">{tt("loadingProfile")}</p>
            )}

            {tutor?.bio && <p className="tutor-preview-bio">{tutor.bio}</p>}

            <div className="tutor-preview-meta">
              {tutor?.hourlyRate && (
                <div>
                  <span>{tt("hourlyRate")}</span>
                  <strong>{Math.round(tutor.hourlyRate)} PLN</strong>
                </div>
              )}

              <div>
                <span>{tt("lessonDuration")}</span>
                <strong>{durationMinutes} min</strong>
              </div>

              {tutor?.experienceYears != null && (
                <div>
                  <span>{tt("experience")}</span>
                  <strong>
                    {tutor.experienceYears}{" "}
                    {tt("experienceUnit", { count: tutor.experienceYears })}
                  </strong>
                </div>
              )}
            </div>

            <dl className="tutor-preview-details">
              {subjects && (
                <>
                  <dt>{tt("subjects")}</dt>
                  <dd>{subjects}</dd>
                </>
              )}

              {languages && (
                <>
                  <dt>{tt("languages")}</dt>
                  <dd>{languages}</dd>
                </>
              )}

              {lessonModes && (
                <>
                  <dt>{tt("lessonModes")}</dt>
                  <dd>{lessonModes}</dd>
                </>
              )}

              {preferredDays.length > 0 && (
                <>
                  <dt>{tt("preferredDays")}</dt>
                  <dd>{preferredDays.join(", ")}</dd>
                </>
              )}

              {tutor?.teachingMethods && (
                <>
                  <dt>{tt("teachingMethods")}</dt>
                  <dd>{tutor.teachingMethods}</dd>
                </>
              )}
            </dl>

            <form className="tutor-preview-form" onSubmit={handleSubmit}>
              <h3>{tt("bookingTitle")}</h3>
              <p className="tutor-preview-form-lead">{tt("bookingSubtitle")}</p>

              <div className="tutor-preview-form-grid">
                <label>
                  {tt("date")}
                  <input
                    type="date"
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                  />
                </label>

                <label>
                  {tt("time")}
                  <input
                    type="time"
                    value={time}
                    onChange={(e) => setTime(e.target.value)}
                  />
                </label>
              </div>

              <label>
                {tt("notes")}
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  rows={3}
                  placeholder={tt("notesPlaceholder")}
                />
              </label>

              <p className="tutor-preview-duration">
                {tt("duration", { value: durationMinutes })}
              </p>

              {formError && (
                <p className="tutor-preview-message tutor-preview-message--error">
                  {formError}
                </p>
              )}

              {bookingMessage && (
                <p className={`tutor-preview-message tutor-preview-message--${bookingMessage.tone}`}>
                  {bookingMessage.text}
                </p>
              )}

              <Button
                type="submit"
                variant="primary"
                disabled={disableBooking}
              >
                {bookingState?.status === "loading" ? tt("bookingWaiting") : tt("bookCta")}
              </Button>
            </form>
          </div>

          <div className="tutor-preview-column tutor-preview-column--calendar">
            <h3>{tt("scheduleTitle")}</h3>
            {scheduleMessage ? (
              <p className="tutor-preview-status">{scheduleMessage}</p>
            ) : (
              <CalendarPro
                role="student"
                events={busyEvents}
                readOnly
                locale={calendarLocale}
              />
            )}
          </div>
        </div>
      </div>
    </div>
  );
}


