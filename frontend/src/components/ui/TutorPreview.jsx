import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import CalendarPro from "./CalendarPro";
import Button from "./Button";
import Alert from "./Alert";
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
  const toList = (input) => {
    if (!input) return [];
    if (Array.isArray(input)) {
      return input.map((item) => (item ?? "").toString().trim()).filter(Boolean);
    }
    return String(input)
      .split(/[\n,;]/)
      .map((item) => item.trim())
      .filter(Boolean);
  };

  const [date, setDate] = useState("");
  const [time, setTime] = useState("");
  const [notes, setNotes] = useState("");
  const [durationMinutes, setDurationMinutes] = useState(60);
  const [formError, setFormError] = useState("");
  const [deliveryMode, setDeliveryMode] = useState("ONLINE");
  const [onsiteCity, setOnsiteCity] = useState("");
  const [onsitePostalCode, setOnsitePostalCode] = useState("");
  const [onsiteStreet, setOnsiteStreet] = useState("");
  const [onsiteBuilding, setOnsiteBuilding] = useState("");
  const [onsiteApartment, setOnsiteApartment] = useState("");

  const durationOptions = useMemo(() => {
    const base = [30, 60, 90, 120];
    const fallback = tutor?.lessonDuration;
    if (fallback && !base.includes(fallback)) {
      return [...base, fallback].sort((a, b) => a - b);
    }
    return base;
  }, [tutor?.lessonDuration]);

  const availableModes = useMemo(() => {
    const list = toList(tutor?.lessonModes).map((item) => item.toLowerCase());
    const supportsOnline = list.some((mode) =>
      ["online", "zdal", "remote", "hybrid", "hybryd"].some((needle) => mode.includes(needle))
    );
    const supportsOnsite = list.some((mode) =>
      ["onsite", "on-site", "stacjon", "stationary", "hybrid", "hybryd"].some((needle) => mode.includes(needle))
    );
    const result = [];
    if (supportsOnline) result.push("ONLINE");
    if (supportsOnsite) result.push("ONSITE");
    if (!result.length) {
      result.push("ONLINE");
    }
    return result;
  }, [tutor?.lessonModes]);

  useEffect(() => {
    if (visible) {
      setDate("");
      setTime("");
      setNotes("");
      setFormError("");
      const initial = tutor?.lessonDuration;
      if (initial && durationOptions.includes(initial)) {
        setDurationMinutes(initial);
      } else {
        setDurationMinutes(60);
      }
      const defaultMode = availableModes[0] || "ONLINE";
      setDeliveryMode(defaultMode);
      setOnsiteCity(tutor?.city || "");
      setOnsitePostalCode("");
      setOnsiteStreet("");
      setOnsiteBuilding("");
      setOnsiteApartment("");
    }
  }, [visible, tutor?.id, tutor?.lessonDuration, durationOptions, availableModes, tutor?.city]);

  const preferredDays = useMemo(() => {
    if (!tutor?.preferredDays || tutor.preferredDays.length === 0) {
      return [];
    }
    return tutor.preferredDays.map((d) => dayLabel(d) || d.toUpperCase());
  }, [tutor?.preferredDays, t]);

  const languages = useMemo(() => {
    const list = toList(tutor?.teachingLanguages);
    return list.length ? list.join(", ") : "";
  }, [tutor?.teachingLanguages]);

  const subjects = useMemo(() => {
    const list = toList(tutor?.subjects);
    return list.length ? list.join(", ") : "";
  }, [tutor?.subjects]);

  const lessonModes = useMemo(() => {
    const list = toList(tutor?.lessonModes);
    if (!list.length) return "";
    return list
      .map((mode) =>
        t(`app.student.tutors.detail.lessonModeLabels.${mode}`, {
          defaultValue: t(`app.tutor.profile.modesLabels.${mode}`, { defaultValue: mode }),
        })
      )
      .filter(Boolean)
      .join(", ");
  }, [tutor?.lessonModes, t]);

  const hasModeChoice = availableModes.length > 1;
  const showOnsiteFields = deliveryMode === "ONSITE";

  const examResults = useMemo(() => toList(tutor?.examResults), [tutor?.examResults]);
  const certificates = useMemo(() => toList(tutor?.certificates), [tutor?.certificates]);

  const website = useMemo(() => {
    const raw = tutor?.website?.trim();
    if (!raw) return null;
    if (/^https?:\/\//i.test(raw)) return raw;
    return `https://${raw}`;
  }, [tutor?.website]);

  const linkedIn = useMemo(() => {
    const raw = tutor?.linkedIn?.trim();
    if (!raw) return null;
    if (/^https?:\/\//i.test(raw)) return raw;
    return `https://${raw}`;
  }, [tutor?.linkedIn]);

  const travelRadiusLabel = useMemo(() => {
    if (tutor?.travelRadius == null) return "";
    return tt("travelRadiusValue", { value: tutor.travelRadius });
  }, [tutor?.travelRadius, tt]);

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
    if (!deliveryMode) {
      setFormError(tt("deliveryModeRequired"));
      return;
    }
    if (
      deliveryMode === "ONSITE" &&
      (!onsiteCity.trim() ||
        !onsitePostalCode.trim() ||
        !onsiteStreet.trim() ||
        !onsiteBuilding.trim())
    ) {
      setFormError(tt("onsiteAddressRequired"));
      return;
    }
    setFormError("");
    const payload = {
      start: `${date}T${time}`,
      durationMinutes,
      notes: notes.trim(),
      deliveryMode,
    };
    if (deliveryMode === "ONSITE") {
      payload.onsiteCity = onsiteCity.trim();
      payload.onsitePostalCode = onsitePostalCode.trim();
      payload.onsiteStreet = onsiteStreet.trim();
      payload.onsiteBuilding = onsiteBuilding.trim();
      const apartment = onsiteApartment.trim();
      if (apartment) {
        payload.onsiteApartment = apartment;
      }
    }
    onBook?.(payload);
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

            {loading && <Alert variant="info">{tt("loadingProfile")}</Alert>}

            {tutor?.bio && <p className="tutor-preview-bio">{tutor.bio}</p>}

            <div className="tutor-preview-meta">
              {tutor?.hourlyRate != null && (
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
                  <strong>{tt("experienceValue", { count: tutor.experienceYears })}</strong>
                </div>
              )}
            </div>

            <dl className="tutor-preview-details">
              {tutor?.education && (
                <>
                  <dt>{tt("education")}</dt>
                  <dd>{tutor.education}</dd>
                </>
              )}

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

              {examResults.length > 0 && (
                <>
                  <dt>{tt("examResults")}</dt>
                  <dd>
                    <ul className="tutor-preview-list">
                      {examResults.map((item, idx) => (
                        <li key={`${item}-${idx}`}>{item}</li>
                      ))}
                    </ul>
                  </dd>
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

              {travelRadiusLabel && (
                <>
                  <dt>{tt("travelRadius")}</dt>
                  <dd>{travelRadiusLabel}</dd>
                </>
              )}

              {certificates.length > 0 && (
                <>
                  <dt>{tt("certificates")}</dt>
                  <dd>
                    <ul className="tutor-preview-list">
                      {certificates.map((item, idx) => (
                        <li key={`${item}-${idx}`}>{item}</li>
                      ))}
                    </ul>
                  </dd>
                </>
              )}

              {website && (
                <>
                  <dt>{tt("website")}</dt>
                  <dd>
                    <a href={website} target="_blank" rel="noopener noreferrer">
                      {tt("openLink")}
                    </a>
                  </dd>
                </>
              )}

              {linkedIn && (
                <>
                  <dt>{tt("linkedin")}</dt>
                  <dd>
                    <a href={linkedIn} target="_blank" rel="noopener noreferrer">
                      {tt("openLink")}
                    </a>
                  </dd>
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

                <label>
                  {tt("durationLabel")}
                  <select
                    value={durationMinutes}
                    onChange={(e) => setDurationMinutes(Number(e.target.value))}
                  >
                    {durationOptions.map((option) => (
                      <option key={option} value={option}>
                        {option} {tt("minutesSuffix")}
                      </option>
                    ))}
                  </select>
                </label>
              </div>

            <label>
              {tt("deliveryModeLabel")}
              {hasModeChoice ? (
                <select value={deliveryMode} onChange={(e) => setDeliveryMode(e.target.value)}>
                  {availableModes.map((mode) => (
                    <option key={mode} value={mode}>
                      {tt(`deliveryModeOptions.${mode.toLowerCase()}`, {
                        defaultValue: mode,
                      })}
                    </option>
                  ))}
                </select>
              ) : (
                <div className="tutor-preview-mode-static">
                  {tt(`deliveryModeOptions.${availableModes[0].toLowerCase()}`, {
                    defaultValue: availableModes[0],
                  })}
                </div>
              )}
            </label>

            {showOnsiteFields && (
              <div className="tutor-preview-onsite">
                <p className="tutor-preview-onsite-lead">{tt("onsiteLead")}</p>
                <div className="tutor-preview-onsite-grid">
                  <label>
                    {tt("onsiteCity")}
                    <input
                      value={onsiteCity}
                      onChange={(e) => setOnsiteCity(e.target.value)}
                      placeholder={tt("onsiteCityPlaceholder")}
                    />
                  </label>
                  <label>
                    {tt("onsitePostalCode")}
                    <input
                      value={onsitePostalCode}
                      onChange={(e) => setOnsitePostalCode(e.target.value)}
                      placeholder={tt("onsitePostalPlaceholder")}
                    />
                  </label>
                  <label>
                    {tt("onsiteStreet")}
                    <input
                      value={onsiteStreet}
                      onChange={(e) => setOnsiteStreet(e.target.value)}
                      placeholder={tt("onsiteStreetPlaceholder")}
                    />
                  </label>
                  <label>
                    {tt("onsiteBuilding")}
                    <input
                      value={onsiteBuilding}
                      onChange={(e) => setOnsiteBuilding(e.target.value)}
                      placeholder={tt("onsiteBuildingPlaceholder")}
                    />
                  </label>
                  <label>
                    {tt("onsiteApartment")}
                    <input
                      value={onsiteApartment}
                      onChange={(e) => setOnsiteApartment(e.target.value)}
                      placeholder={tt("onsiteApartmentPlaceholder")}
                    />
                  </label>
                </div>
              </div>
            )}

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
                <Alert variant="error">{formError}</Alert>
              )}

              {bookingMessage && (
                <Alert
                  variant={
                    bookingMessage.tone === "success"
                      ? "success"
                      : bookingMessage.tone === "error"
                      ? "error"
                      : bookingMessage.tone === "warn"
                      ? "warning"
                      : "info"
                  }
                >
                  {bookingMessage.text}
                </Alert>
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
              <Alert variant="info">{scheduleMessage}</Alert>
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


