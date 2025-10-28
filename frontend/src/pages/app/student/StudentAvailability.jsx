import { useState } from "react";
import { useTranslation } from "react-i18next";
import Button from "../../../components/ui/Button";
import Check from "../../../components/ui/Check";
import "./student-availability.css";

export default function StudentAvailability() {
  const { t } = useTranslation("common");

  const [form, setForm] = useState({
    preferredDays: { mon: true, tue: true, wed: true, thu: true, fri: true, sat: false, sun: false },
    availabilityNote: "",
    calendarUrl: "",
  });

  const [errors, setErrors] = useState({});
  const [successMessage, setSuccessMessage] = useState("");

  const onChange = (e) =>
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const onToggle = (group, key) =>
    setForm((f) => ({ ...f, [group]: { ...f[group], [key]: !f[group][key] } }));

  const dayLabels = {
    mon: t("app.student.profile.dayLabels.mon"),
    tue: t("app.student.profile.dayLabels.tue"),
    wed: t("app.student.profile.dayLabels.wed"),
    thu: t("app.student.profile.dayLabels.thu"),
    fri: t("app.student.profile.dayLabels.fri"),
    sat: t("app.student.profile.dayLabels.sat"),
    sun: t("app.student.profile.dayLabels.sun"),
  };

  const handleSave = async () => {
    // TODO: API call to save availability
    console.log("Saving availability:", form);
    setSuccessMessage(t("app.student.availability.success.saved"));
    setErrors({});
  };

  return (
    <div className="student-availability">
      <div className="availability-header">
        <h2>{t("app.student.availability.title")}</h2>
        {successMessage && (
          <div className="success-message">{successMessage}</div>
        )}
      </div>

      <div className="availability-section">
        <h3>{t("app.student.availability.sections.calendar")}</h3>
        
        <div className="form-group">
          <label>{t("app.student.availability.fields.calendarUrl")}</label>
          <input
            name="calendarUrl"
            type="url"
            value={form.calendarUrl}
            onChange={onChange}
            placeholder={t("app.student.availability.placeholders.calendarUrl")}
          />
          <div className="hint">
            {t("app.student.availability.hints.calendarUrl")}
          </div>
        </div>

        {form.calendarUrl && (
          <div className="calendar-preview">
            <h4>{t("app.student.availability.calendarPreview")}</h4>
            <iframe
              src={form.calendarUrl}
              style={{ width: '100%', height: '400px', border: 0 }}
              title="Google Calendar"
            />
          </div>
        )}
      </div>

      <div className="availability-section">
        <h3>{t("app.student.availability.sections.days")}</h3>
        
        <div className="form-group">
          <label>{t("app.student.profile.fields.preferredDays")}</label>
          <div className="checks">
            {Object.entries(dayLabels).map(([k, l]) => (
              <Check
                key={k}
                label={l}
                name={k}
                checked={form.preferredDays[k]}
                onChange={() => onToggle("preferredDays", k)}
              />
            ))}
          </div>
        </div>
      </div>

      <div className="availability-section">
        <h3>{t("app.student.availability.sections.notes")}</h3>
        
        <div className="form-group">
          <label>{t("app.student.profile.fields.availabilityNote")}</label>
          <textarea
            name="availabilityNote"
            rows={4}
            value={form.availabilityNote}
            onChange={onChange}
            placeholder={t("app.student.profile.placeholders.availabilityNote")}
          />
        </div>
      </div>

      <div className="availability-actions">
        <Button variant="primary" onClick={handleSave}>
          {t("app.student.availability.actions.save")}
        </Button>
      </div>
    </div>
  );
}
