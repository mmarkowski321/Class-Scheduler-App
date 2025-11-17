// src/pages/app/student/StudentTutors.jsx
import { useCallback, useEffect, useMemo, useState } from "react";
import "./tutors.css";
import Button from "../../../components/ui/Button";
import Alert from "../../../components/ui/Alert";
import { useTranslation } from "react-i18next";
import TutorPreview from "../../../components/ui/TutorPreview";
import {
  fetchTutors,
  fetchTutor,
  fetchTutorBusyTimes,
  bookTutor,
} from "../../../services/tutors";
import { SUBJECTS, normalizeSubject } from "../../../data/subjects";
import { ensureRoleInStorage } from "../../../utils/auth";

const DAY_KEYS = ["mon", "tue", "wed", "thu", "fri", "sat", "sun"];
const createInitialFilters = () => ({
  q: "",
  subject: "",
  city: "",
  price: 160,
  days: DAY_KEYS.reduce(
    (acc, key) => ({ ...acc, [key]: false }),
    {}
  ),
});

export default function StudentTutors() {
  const { t, i18n } = useTranslation("common");
  const tt = (k, o) => t(`app.student.tutors.${k}`, o);
  const dayLabel = (d) => t(`app.tutor.profile.days.${d}`);
  const busyLabel = tt("detail.busySlot");

  const [filters, setFilters] = useState(createInitialFilters);
  const [tutors, setTutors] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [overlayOpen, setOverlayOpen] = useState(false);
  const [selectedTutor, setSelectedTutor] = useState(null);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [busyEvents, setBusyEvents] = useState([]);
  const [busyLoading, setBusyLoading] = useState(false);
  const [busyError, setBusyError] = useState("");
  const [bookingState, setBookingState] = useState({ status: "idle" });

  const selectedDays = useMemo(
    () => DAY_KEYS.filter((key) => filters.days[key]),
    [filters.days]
  );

  const subjectOptions = useMemo(
    () =>
      SUBJECTS.map((entry) => ({
        value: entry.value,
        label: i18n.language === "en" ? entry.labelEn : entry.label,
      })),
    [i18n.language]
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

  const buildQuery = useCallback(
    (source) => {
      const query = {
        q: source.q,
        city: source.city,
        maxPrice: source.price,
      };
      const normalizedSubject = normalizeSubject(source.subject);
      if (normalizedSubject) {
        query.subject = normalizedSubject;
      } else if (source.subject && source.subject.trim().length > 0) {
        query.subject = source.subject.trim();
      }
      const days = DAY_KEYS.filter((key) => source.days?.[key]);
      if (days.length) {
        query.days = days;
      }
      return query;
    },
    []
  );

  const loadTutors = useCallback(
    async (sourceFilters) => {
      setLoading(true);
      setError("");
      try {
        const response = await fetchTutors(buildQuery(sourceFilters));
        const mapped = (response?.tutors ?? []).map((tutor) => ({
          ...tutor,
          subjects: (tutor.subjects || []).map(
            (s) => subjectLabelMap.get(s) || s
          ),
        }));
        setTutors(mapped);
      } catch (err) {
        setError(err.message || t("app.student.tutors.errors.list"));
      } finally {
        setLoading(false);
      }
    },
    [buildQuery, t, subjectLabelMap]
  );

  const getToken = () =>
    localStorage.getItem("token") || localStorage.getItem("access_token");

  const getRole = useCallback(() => {
    let role = localStorage.getItem("role");
    if (role) return role;
    const rawToken = getToken();
    if (!rawToken) return null;
    return ensureRoleInStorage(rawToken);
  }, []);

  useEffect(() => {
    loadTutors(createInitialFilters());
  }, [loadTutors]);

  const onChange = (e) =>
    setFilters((f) => ({ ...f, [e.target.name]: e.target.value }));

  const onDay = (key) =>
    setFilters((f) => ({ ...f, days: { ...f.days, [key]: !f.days[key] } }));

  const handleReset = () => {
  const subjectOptions = useMemo(
    () =>
      SUBJECTS.map((entry) => ({
        value: entry.value,
        label: i18n.language === "en" ? entry.labelEn : entry.label,
      })),
    [i18n.language]
  );
    const next = createInitialFilters();
    setFilters(next);
    loadTutors(next);
  };

  const handleTutorOpen = async (baseTutor) => {
    if (!baseTutor) return;
    setOverlayOpen(true);
    setSelectedTutor(baseTutor);
    setDetailsLoading(true);
    setBusyLoading(true);
    setBusyError("");
    setBookingState({ status: "idle" });

    try {
      const detail = await fetchTutor(baseTutor.id);
      setSelectedTutor(detail);
    } catch (err) {
      setBookingState({
        status: "error",
        message: err.message || tt("errors.profile"),
      });
    } finally {
      setDetailsLoading(false);
    }

    try {
      const busy = await fetchTutorBusyTimes(baseTutor.id);
      const events = (busy?.busyTimes ?? []).map((slot, idx) => ({
        id: `busy-${idx}`,
        type: "busy",
        title: busyLabel,
        start: slot.start,
        end: slot.end,
      }));
      setBusyEvents(events);
      setBusyError("");
    } catch (err) {
      setBusyEvents([]);
      setBusyError(err.message || tt("errors.schedule"));
    } finally {
      setBusyLoading(false);
    }
  };

  const handleTutorClose = () => {
    setOverlayOpen(false);
    setSelectedTutor(null);
    setBusyEvents([]);
    setBusyLoading(false);
    setBusyError("");
    setBookingState({ status: "idle" });
  };

  const handleBook = async ({
    start,
    durationMinutes,
    notes,
    deliveryMode,
    onsiteCity,
    onsitePostalCode,
    onsiteStreet,
    onsiteBuilding,
    onsiteApartment,
  }) => {
    if (!selectedTutor) return;
    const token = getToken();
    const role = getRole();
    if (!token || role !== "STUDENT") {
      setBookingState({ status: "unauthenticated" });
      return;
    }

    setBookingState({ status: "loading" });
    try {
      const payload = {
        start,
        notes,
        deliveryMode,
      };
      if (durationMinutes) {
        payload.durationMinutes = durationMinutes;
      }
      if (deliveryMode === "ONSITE") {
        payload.onsiteCity = onsiteCity;
        payload.onsitePostalCode = onsitePostalCode;
        payload.onsiteStreet = onsiteStreet;
        payload.onsiteBuilding = onsiteBuilding;
        if (onsiteApartment) {
          payload.onsiteApartment = onsiteApartment;
        }
      }
      await bookTutor(selectedTutor.id, payload, token);
      setBookingState({ status: "success" });
      await loadTutors();
    } catch (err) {
      setBookingState({
        status: "error",
        message: err.message || tt("errors.booking"),
      });
    }
  };

  return (
    <div className="tutors-layout">
      <div className="tutors-card">
        <h3 className="tutors-title">{tt("title")}</h3>
        <p className="tutors-lead">{tt("lead")}</p>

        <div className="filters-grid">
          <div className="field">
            <span className="icon" aria-hidden="true" role="presentation">
              🔎
            </span>
            <input
              className="input"
              name="q"
              value={filters.q}
              onChange={onChange}
              placeholder={tt("filters.search")}
              aria-label={tt("filters.search")}
              autoComplete="off"
            />
          </div>
          <div className="field">
            <span className="icon" aria-hidden="true" role="presentation">
              📘
            </span>
            <input
              className="input"
              name="subject"
              value={filters.subject}
              onChange={onChange}
              placeholder={tt("filters.subject")}
              aria-label={tt("filters.subject")}
              autoComplete="off"
              list="subject-options"
            />
            <datalist id="subject-options">
              {subjectOptions.map((option) => (
                <option key={option.value} value={option.label} />
              ))}
            </datalist>
          </div>
          <div className="field">
            <span className="icon" aria-hidden="true" role="presentation">
              📍
            </span>
            <input
              className="input"
              name="city"
              value={filters.city}
              onChange={onChange}
              placeholder={tt("filters.city")}
              aria-label={tt("filters.city")}
              autoComplete="off"
            />
          </div>
        </div>

        <div className="range-section">
          <div className="range-wrap">
            <strong>{tt("filters.maxPrice")}:</strong>
            <input
              type="range"
              className="range"
              min="40"
              max="300"
              step="10"
              value={filters.price}
              onChange={(e) =>
                setFilters((f) => ({ ...f, price: Number(e.target.value) }))
              }
              aria-label={tt("filters.maxPrice")}
            />
            <span>{filters.price} PLN</span>
          </div>
        </div>

        <div className="days-grid">
          {DAY_KEYS.map((key) => (
            <label key={key} className="day-item">
              <input
                type="checkbox"
                checked={filters.days[key]}
                onChange={() => onDay(key)}
                aria-label={dayLabel(key)}
              />
              <span>{dayLabel(key)}</span>
            </label>
          ))}
        </div>

        <div className="filters-actions">
          <button className="btn-reset" type="button" onClick={handleReset}>
            {tt("filters.reset")}
          </button>
          <Button type="button" variant="primary" onClick={() => loadTutors(filters)}>
            {tt("apply")}
          </Button>
        </div>
      </div>

      <div className="tutors-results">
        <div className="tutors-results-head">
          <div>
            <h4>{tt("results.title")}</h4>
            <p className="tutors-results-meta">
              {tt("results.meta", { count: tutors.length })}
              {selectedDays.length > 0 && (
                <span> • {tt("results.days", { count: selectedDays.length })}</span>
              )}
            </p>
          </div>
          <div className="tutors-results-meta">
            {loading && <span>{tt("loading")}</span>}
          </div>
        </div>

        {error && (
          <Alert variant="error" key="list-error">
            {error}
          </Alert>
        )}

        {!loading && !error && tutors.length === 0 && (
          <Alert variant="info">{tt("empty")}</Alert>
        )}

        <div className="tutor-grid">
          {tutors.map((tutor) => (
            <button
              key={tutor.id}
              className="tutor-card"
              type="button"
              onClick={() => handleTutorOpen(tutor)}
            >
              <div className="tutor-card-photo">
                {tutor.photoUrl ? (
                  <img src={tutor.photoUrl} alt="" />
                ) : (
                  <span aria-hidden="true">{tutor.firstName?.[0] ?? "?"}</span>
                )}
              </div>
              <div className="tutor-card-body">
                <div className="tutor-card-head">
                  <strong>
                    {tutor.firstName} {tutor.lastName}
                  </strong>
                  {tutor.city && <span className="tutor-card-city">📍 {tutor.city}</span>}
                </div>
                {tutor.subjects?.length > 0 && (
                  <p className="tutor-card-subjects">
                    {tutor.subjects.slice(0, 3).join(", ")}
                    {tutor.subjects.length > 3 ? "…" : ""}
                  </p>
                )}
                <div className="tutor-card-meta">
                  {tutor.hourlyRate != null && (
                    <span>{tt("card.price", { value: Math.round(tutor.hourlyRate) })}</span>
                  )}
                  {tutor.lessonDuration && (
                    <span>{tt("card.duration", { value: tutor.lessonDuration })}</span>
                  )}
                </div>
              </div>
              <span className="tutor-card-cta">{tt("viewProfile")} →</span>
            </button>
          ))}
        </div>
      </div>

      <TutorPreview
        visible={overlayOpen && !!selectedTutor}
        tutor={selectedTutor}
        loading={detailsLoading}
        onClose={handleTutorClose}
        busyEvents={busyEvents}
        busyLoading={busyLoading}
        busyError={busyError}
        onBook={handleBook}
        bookingState={bookingState}
      />
    </div>
  );
}
