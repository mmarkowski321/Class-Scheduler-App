// src/pages/app/student/StudentTutors.jsx
import { useState } from "react";
import "./tutors.css";
import Button from "../../../components/ui/Button";
import { useTranslation } from "react-i18next";

export default function StudentTutors() {
  const { t } = useTranslation("common");
  const tt = (k, o) => t(`app.student.tutors.${k}`, o);
  const dayLabel = (d) => t(`app.tutor.profile.days.${d}`);

  const [filters, setFilters] = useState({
    q: "",
    subject: "",
    city: "",
    price: 160,
    days: { mon:false, tue:false, wed:false, thu:false, fri:false, sat:false, sun:false }
  });

  const onChange = (e) =>
    setFilters((f) => ({ ...f, [e.target.name]: e.target.value }));

  const onDay = (k) =>
    setFilters((f) => ({ ...f, days: { ...f.days, [k]: !f.days[k] } }));

  const reset = () =>
    setFilters({
      q: "",
      subject: "",
      city: "",
      price: 160,
      days: { mon:false, tue:false, wed:false, thu:false, fri:false, sat:false, sun:false }
    });

  return (
    <div className="tutors-card">
      <h3 style={{ marginBottom: 12 }}>{tt("title")}</h3>
      <p style={{ opacity: 0.85, marginBottom: 18 }}>{tt("lead")}</p>

      <div className="filters-grid">
        <div className="field">
          <span className="icon" aria-hidden="true" role="presentation">🔎</span>
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
          <span className="icon" aria-hidden="true" role="presentation">📘</span>
          <input
            className="input"
            name="subject"
            value={filters.subject}
            onChange={onChange}
            placeholder={tt("filters.subject")}
            aria-label={tt("filters.subject")}
            autoComplete="off"
          />
        </div>
        <div className="field">
          <span className="icon" aria-hidden="true" role="presentation">📍</span>
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

      <div style={{ marginTop: 16 }}>
        <div className="range-wrap">
          <strong style={{ minWidth: 120 }}>{tt("filters.maxPrice")}:</strong>
          <input
            type="range"
            className="range"
            min="40"
            max="300"
            step="10"
            value={filters.price}
            onChange={(e) => setFilters((f) => ({ ...f, price: +e.target.value }))}
            aria-label={tt("filters.maxPrice")}
          />
          <span style={{ opacity: 0.9 }}>{filters.price} PLN</span>
        </div>
      </div>

      <div style={{ display: "grid", gap: 10, marginTop: 16 }}>
        {["mon","tue","wed","thu","fri","sat","sun"].map((k) => (
          <label key={k} className="day-item">
            <input
              type="checkbox"
              checked={filters.days[k]}
              onChange={() => onDay(k)}
              aria-label={dayLabel(k)}
            />
            <span>{dayLabel(k)}</span>
          </label>
        ))}
      </div>

      <div className="filters-actions">
        <button className="btn-reset" type="button" onClick={reset}>
          {tt("filters.reset")}
        </button>
        <Button variant="primary">{tt("apply")}</Button>
      </div>
    </div>
  );
}
