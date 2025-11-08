import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import Button from "../../../components/ui/Button";
import Check from "../../../components/ui/Check";
import "./tutor-availability.css";

export default function TutorAvailability() {
  const { t } = useTranslation("common");
  const token = localStorage.getItem("token");
  const userId = localStorage.getItem("userId");

  const [form, setForm] = useState({
    maxLessonsPerDay: "",
    bufferMin: "10",
    preferredDays: {
      mon: true, tue: true, wed: true, thu: true, fri: true, sat: false, sun: false,
    },
  });

  const [calendars, setCalendars] = useState([]); // List of calendars
  const [newCalendarUrl, setNewCalendarUrl] = useState("");
  const [newCalendarName, setNewCalendarName] = useState("");
  const [errors, setErrors] = useState({});
  const [successMessage, setSuccessMessage] = useState("");
  const [busyTimes, setBusyTimes] = useState([]);
  const [loadingCalendar, setLoadingCalendar] = useState(false);
  const [calendarSynced, setCalendarSynced] = useState(false);

  const onChange = (e) =>
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const onToggle = (group, key) =>
    setForm((f) => ({ ...f, [group]: { ...f[group], [key]: !f[group][key] } }));

  // Load user data and calendars
  useEffect(() => {
    const loadUserData = async () => {
      if (!token || !userId) return;
      try {
        // Load profile
        const res = await fetch(`/api/profile/${userId}`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        if (res.ok) {
          const user = await res.json();
          setForm((f) => ({
            ...f,
            maxLessonsPerDay: user.maxLessonsPerDay || "",
            bufferMin: user.bufferTime || "10",
            preferredDays: user.preferredDays ? JSON.parse(user.preferredDays) : f.preferredDays,
          }));
        }

        // Load calendars
        const calendarsRes = await fetch(`/api/profile/${userId}/calendars`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        if (calendarsRes.ok) {
          const calendarsData = await calendarsRes.json();
          console.log("Loaded calendars:", calendarsData); // Debug log
          setCalendars(calendarsData.calendars || []);
          
          // Auto-sync all calendars if any exist
          if (calendarsData.calendars && calendarsData.calendars.length > 0) {
            setTimeout(() => {
              syncAllCalendars();
            }, 500);
          }
        } else {
          const errorText = await calendarsRes.text();
          console.error("Failed to load calendars:", calendarsRes.status, errorText);
          setErrors({ calendar: `Failed to load calendars: ${calendarsRes.status}` });
        }
      } catch (error) {
        console.error("Failed to load user data:", error);
      }
    };
    loadUserData();
  }, [token, userId]);

  const syncAllCalendars = async () => {
    if (!token || !userId) return;
    
    setLoadingCalendar(true);
    try {
      const response = await fetch(`/api/calendar/sync/${userId}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      
      if (response.ok) {
        const data = await response.json();
        setBusyTimes(data.busyTimes || []);
        setCalendarSynced(true);
        
        if (data.warning) {
          setErrors({ calendar: data.warning });
        } else if (data.count > 0) {
          setSuccessMessage(t("app.tutor.availability.success.calendarSynced", { count: data.count || 0 }));
        }
      }
    } catch (error) {
      console.error("Failed to sync calendars:", error);
    } finally {
      setLoadingCalendar(false);
    }
  };

  const addCalendar = async () => {
    if (!newCalendarUrl || !newCalendarUrl.trim()) {
      setErrors({ calendar: "Calendar URL is required" });
      return;
    }

    try {
      const response = await fetch(`/api/profile/${userId}/calendars`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          calendarUrl: newCalendarUrl.trim(),
          name: newCalendarName.trim() || null,
        }),
      });

      if (response.ok) {
        const newCalendar = await response.json();
        setCalendars([...calendars, newCalendar]);
        setNewCalendarUrl("");
        setNewCalendarName("");
        setErrors({});
        setSuccessMessage("Calendar added successfully");
        
        // Sync all calendars after adding
        setTimeout(() => {
          syncAllCalendars();
        }, 500);
      } else {
        const errorData = await response.json();
        setErrors({ calendar: errorData.error || "Failed to add calendar" });
      }
    } catch (error) {
      console.error("Failed to add calendar:", error);
      setErrors({ calendar: "Failed to add calendar" });
    }
  };

  const deleteCalendar = async (calendarId) => {
    if (!confirm("Are you sure you want to remove this calendar?")) {
      return;
    }

    try {
      const response = await fetch(`/api/profile/${userId}/calendars/${calendarId}`, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (response.ok) {
        setCalendars(calendars.filter((cal) => cal.id !== calendarId));
        setSuccessMessage("Calendar removed successfully");
        
        // Sync remaining calendars
        setTimeout(() => {
          syncAllCalendars();
        }, 500);
      } else {
        const errorData = await response.json();
        setErrors({ calendar: errorData.error || "Failed to remove calendar" });
      }
    } catch (error) {
      console.error("Failed to remove calendar:", error);
      setErrors({ calendar: "Failed to remove calendar" });
    }
  };

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

    try {
      const response = await fetch(`/api/profile/tutor/${userId}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          maxLessonsPerDay: parseInt(form.maxLessonsPerDay),
          bufferTime: parseInt(form.bufferMin),
          preferredDays: JSON.stringify(form.preferredDays),
        }),
      });

      if (response.ok) {
        setSuccessMessage(t("app.tutor.availability.success.saved"));
        setErrors({});
      } else {
        const errorData = await response.json();
        setErrors({ form: errorData.error || "Failed to save availability" });
      }
    } catch (error) {
      console.error("Failed to save availability:", error);
      setErrors({ form: "Failed to save availability" });
    }
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
        
        {/* Add new calendar */}
        <div className="form-group">
          <label>{t("app.tutor.availability.fields.calendarUrl")}</label>
          <div style={{ display: "flex", gap: "8px", marginBottom: "8px" }}>
            <input
              type="text"
              placeholder={t("app.tutor.availability.placeholders.calendarName") || "Calendar name (optional)"}
              value={newCalendarName}
              onChange={(e) => setNewCalendarName(e.target.value)}
              style={{ flex: "0 0 200px" }}
            />
            <input
              type="url"
              placeholder={t("app.tutor.availability.placeholders.calendarUrl")}
              value={newCalendarUrl}
              onChange={(e) => setNewCalendarUrl(e.target.value)}
              style={{ flex: "1" }}
            />
            <Button
              variant="secondary"
              size="small"
              onClick={addCalendar}
              disabled={!newCalendarUrl || !newCalendarUrl.trim()}
            >
              {t("app.tutor.availability.actions.addCalendar") || "Add Calendar"}
            </Button>
          </div>
          <div className="hint">
            {t("app.tutor.availability.hints.calendarUrl")}
          </div>
        </div>

        {/* List of calendars */}
        {calendars.length > 0 ? (
          <div className="calendars-list">
            <h4>{t("app.tutor.availability.calendarsList") || "Your Calendars"}</h4>
            {calendars.map((cal) => (
              <div key={cal.id} className="calendar-item">
                <div className="calendar-info">
                  <div className="calendar-name">{cal.name || "Unnamed Calendar"}</div>
                  <div className="calendar-url">{cal.calendarUrl}</div>
                </div>
                <Button
                  variant="danger"
                  size="small"
                  onClick={() => deleteCalendar(cal.id)}
                >
                  {t("app.tutor.availability.actions.remove") || "Remove"}
                </Button>
              </div>
            ))}
          </div>
        ) : (
          <div style={{ marginTop: "16px", padding: "12px", background: "rgba(255,255,255,0.05)", borderRadius: "8px" }}>
            <p style={{ color: "rgba(255,255,255,0.7)", fontSize: "14px", margin: 0 }}>
              {t("app.tutor.availability.noCalendars")}
            </p>
          </div>
        )}

        {/* Sync button */}
        {calendars.length > 0 && (
          <div style={{ marginTop: "16px" }}>
            <Button
              variant="secondary"
              size="small"
              onClick={syncAllCalendars}
              disabled={loadingCalendar}
            >
              {loadingCalendar ? "Synchronizing..." : t("app.tutor.availability.actions.syncAll") || "Sync All Calendars"}
            </Button>
          </div>
        )}

        {loadingCalendar && <div className="loading">Synchronizing calendars...</div>}
        {calendarSynced && busyTimes.length > 0 && (
          <div className="success-message" style={{ marginTop: "8px" }}>
            {t("app.tutor.availability.calendarSynced", { count: busyTimes.length })}
          </div>
        )}
        {errors.calendar && <div className="error-message">{errors.calendar}</div>}
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
