import { useState } from "react";
import { useTranslation } from "react-i18next";
import Button from "../../../components/ui/Button";
import Check from "../../../components/ui/Check";
import "./tutor-availability.css";

export default function TutorAvailability() {
  const { t } = useTranslation("common");

  const [form, setForm] = useState({
    maxLessonsPerDay: "",
    bufferMin: "10",
    calendarUrl: "",
    preferredDays: {
      mon: true, tue: true, wed: true, thu: true, fri: true, sat: false, sun: false,
    },
  });

  const [errors, setErrors] = useState({});
  const [successMessage, setSuccessMessage] = useState("");

  const onChange = (e) =>
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const onToggle = (group, key) =>
    setForm((f) => ({ ...f, [group]: { ...f[group], [key]: !f[group][key] } }));

  const dayLabels = {
    mon: t("app.tutor.profile.days.mon"),
    tue: t("app.tutor.profile.days.tue"),
    wed: t("app.tutor.profile.days.wed"),
    thu: t("app.tutor.profile.days.thu"),
    fri: t("app.tutor.profile.days.fri"),
    sat: t("app.tutor.profile.days.sat"),
    sun: t("app.tutor.profile.days.sun"),
  };

  const handleSave = async () => {
    const newErrors = {};

    if (!form.maxLessonsPerDay.trim()) {
      newErrors.maxLessonsPerDay = t("app.tutor.availability.errors.maxLessonsRequired");
    } else if (isNaN(form.maxLessonsPerDay) || parseInt(form.maxLessonsPerDay) < 1) {
      newErrors.maxLessonsPerDay = t("app.tutor.availability.errors.maxLessonsInvalid");
    }

    if (!form.bufferMin.trim()) {
      newErrors.bufferMin = t("app.tutor.availability.errors.bufferRequired");
    } else if (isNaN(form.bufferMin) || parseInt(form.bufferMin) < 0) {
      newErrors.bufferMin = t("app.tutor.availability.errors.bufferInvalid");
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    // TODO: API call to save availability
    console.log("Saving availability:", form);
    setSuccessMessage(t("app.tutor.availability.success.saved"));
    setErrors({});
  };

  return (
    <div className="tutor-availability">
      <div className="availability-header">
        <h2>{t("app.tutor.availability.title")}</h2>
        {successMessage && (
          <div className="success-message">{successMessage}</div>
        )}
      </div>

      <div className="availability-section">
        <h3>{t("app.tutor.availability.sections.schedule")}</h3>
        
        <div className="form-group">
          <label>{t("app.tutor.profile.maxPerDay")}</label>
          <input
            name="maxLessonsPerDay"
            inputMode="numeric"
            value={form.maxLessonsPerDay}
            onChange={onChange}
            placeholder={t("app.tutor.profile.placeholders.maxPerDay")}
          />
          {errors.maxLessonsPerDay && <div className="error-message">{errors.maxLessonsPerDay}</div>}
        </div>

        <div className="form-group">
          <label>{t("app.tutor.profile.buffer")}</label>
          <input
            name="bufferMin"
            inputMode="numeric"
            value={form.bufferMin}
            onChange={onChange}
            placeholder={t("app.tutor.profile.placeholders.buffer")}
          />
          {errors.bufferMin && <div className="error-message">{errors.bufferMin}</div>}
        </div>
      </div>

      <div className="availability-section">
        <h3>{t("app.tutor.availability.sections.calendar")}</h3>
        
        <div className="form-group">
          <label>{t("app.tutor.availability.fields.calendarUrl")}</label>
          <input
            name="calendarUrl"
            type="url"
            value={form.calendarUrl}
            onChange={onChange}
            placeholder={t("app.tutor.availability.placeholders.calendarUrl")}
          />
          <div className="hint">
            {t("app.tutor.availability.hints.calendarUrl")}
          </div>
        </div>

        {form.calendarUrl && (
          <div className="calendar-preview">
            <h4>{t("app.tutor.availability.calendarPreview")}</h4>
            <iframe
              src={form.calendarUrl}
              style={{ width: '100%', height: '400px', border: 0 }}
              title="Google Calendar"
            />
          </div>
        )}
      </div>

      <div className="availability-section">
        <h3>{t("app.tutor.availability.sections.days")}</h3>
        
        <div className="form-group">
          <label>{t("app.tutor.profile.preferredDays")}</label>
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

      <div className="availability-actions">
        <Button variant="primary" onClick={handleSave}>
          {t("app.tutor.availability.actions.save")}
        </Button>
      </div>
    </div>
  );
}
