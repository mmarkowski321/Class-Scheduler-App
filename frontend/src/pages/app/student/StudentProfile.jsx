// pages/app/student/StudentProfile.jsx
import { useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import Button from "../../../components/ui/Button";
import "./student-profile.css";

function Check({ label, name, checked, onChange }) {
  return (
    <label className="sp-check">
      <input type="checkbox" name={name} checked={checked} onChange={onChange} />
      <span>{label}</span>
    </label>
  );
}

export default function StudentProfile() {
  const { t } = useTranslation("common");

  const [form, setForm] = useState({
    school: "",
    grade: "",
    track: "",
    languages: "polski",
    phone: "",
    timezone: Intl.DateTimeFormat().resolvedOptions().timeZone || "Europe/Warsaw",
    aboutMe: "",
    goals: "",
    strengths: "",
    difficulties: "",
    preferredSubjects: "",
    avoidSubjects: "",
    learningStyle: "mixed",
    meetingMode: { online: true, onsite: false, hybrid: false },
    city: "",
    preferredTools: { meet: true, zoom: false, teams: false, other: false },
    otherTool: "",
    preferredDays: { mon: true, tue: true, wed: true, thu: true, fri: true, sat: false, sun: false },
    guardianName: "",
    guardianEmail: "",
    shareProfile: true,
  });

  const [errors, setErrors] = useState({});
  const onChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));
  const onToggle = (group, key) =>
    setForm((f) => ({ ...f, [group]: { ...f[group], [key]: !f[group][key] } }));

  const subjectsChips = useMemo(
    () => form.preferredSubjects.split(",").map((s) => s.trim()).filter(Boolean),
    [form.preferredSubjects]
  );
  const langsChips = useMemo(
    () => form.languages.split(",").map((s) => s.trim()).filter(Boolean),
    [form.languages]
  );

  const validate = () => {
    const e = {};
    if (!form.school.trim()) e.school = t("app.student.profile.errors.school");
    if (!form.grade.trim()) e.grade = t("app.student.profile.errors.grade");
    if (!form.timezone.trim()) e.timezone = t("app.student.profile.errors.timezone");
    if (form.guardianEmail && !/\S+@\S+\.\S+/.test(form.guardianEmail))
      e.guardianEmail = t("app.student.profile.errors.guardianEmail");
    return e;
  };

  const onSave = () => {
    const e = validate();
    if (Object.keys(e).length) return setErrors(e);
    console.log("student.profile.save", form);
    alert(t("app.student.profile.savedDemo"));
  };

  const L = t("app.student.profile.learningStyleOptions", { returnObjects: true });
  const M = t("app.student.profile.meetingModeLabels", { returnObjects: true });
  const TL = t("app.student.profile.toolsLabels", { returnObjects: true });
  const D = t("app.student.profile.dayLabels", { returnObjects: true });
  const P = t("app.student.profile.placeholders", { returnObjects: true });

  return (
    <div className="sp-card card">
      <div className="sp-header">
        <div className="sp-title">{t("app.student.profile.title")}</div>
        <div className="sp-subtitle">{t("app.student.profile.subtitle")}</div>
      </div>

      {/* BASIC */}
      <section className="sp-section">
        <h4 className="sp-section-title">{t("app.student.profile.sections.basic")}</h4>
        <div className="sp-grid two">
          <label className="sp-field">
            <span>{t("app.student.profile.fields.school")}</span>
            <input name="school" value={form.school} onChange={onChange} placeholder={P.school} />
            {errors.school && <div className="sp-error">{errors.school}</div>}
          </label>
          <label className="sp-field">
            <span>{t("app.student.profile.fields.grade")}</span>
            <input name="grade" value={form.grade} onChange={onChange} placeholder={P.grade} />
            {errors.grade && <div className="sp-error">{errors.grade}</div>}
          </label>
          <label className="sp-field">
            <span>{t("app.student.profile.fields.track")}</span>
            <input name="track" value={form.track} onChange={onChange} placeholder={P.track} />
          </label>
          <label className="sp-field">
            <span>{t("app.student.profile.fields.phone")}</span>
            <input name="phone" value={form.phone} onChange={onChange} placeholder={P.phone} />
          </label>
          <label className="sp-field">
            <span>{t("app.student.profile.fields.languages")}</span>
            <input
              name="languages"
              value={form.languages}
              onChange={onChange}
              placeholder={P.languages}
            />
            {!!langsChips.length && (
              <div className="sp-chips">
                {langsChips.map((s, i) => (
                  <span key={i} className="sp-chip">
                    {s}
                  </span>
                ))}
              </div>
            )}
          </label>
          <label className="sp-field">
            <span>{t("app.student.profile.fields.timezone")}</span>
            <input name="timezone" value={form.timezone} onChange={onChange} placeholder="Europe/Warsaw" />
            {errors.timezone && <div className="sp-error">{errors.timezone}</div>}
          </label>
        </div>
      </section>

      {/* ABOUT */}
      <section className="sp-section">
        <h4 className="sp-section-title">{t("app.student.profile.sections.about")}</h4>
        <div className="sp-grid two">
          <label className="sp-field">
            <span>{t("app.student.profile.fields.aboutMe")}</span>
            <textarea
              name="aboutMe"
              rows={3}
              value={form.aboutMe}
              onChange={onChange}
              placeholder={P.aboutMe}
            />
          </label>
          <label className="sp-field">
            <span>{t("app.student.profile.fields.goals")}</span>
            <textarea name="goals" rows={3} value={form.goals} onChange={onChange} placeholder={P.goals} />
          </label>
          <label className="sp-field">
            <span>{t("app.student.profile.fields.strengths")}</span>
            <input
              name="strengths"
              value={form.strengths}
              onChange={onChange}
              placeholder={P.strengths}
            />
          </label>
          <label className="sp-field">
            <span>{t("app.student.profile.fields.difficulties")}</span>
            <input
              name="difficulties"
              value={form.difficulties}
              onChange={onChange}
              placeholder={P.difficulties}
            />
          </label>
        </div>
      </section>

      {/* SUBJECTS */}
      <section className="sp-section">
        <h4 className="sp-section-title">{t("app.student.profile.sections.subjects")}</h4>
        <div className="sp-grid two">
          <label className="sp-field">
            <span>{t("app.student.profile.fields.preferredSubjects")}</span>
            <input
              name="preferredSubjects"
              value={form.preferredSubjects}
              onChange={onChange}
              placeholder={P.preferredSubjects}
            />
            {!!subjectsChips.length && (
              <div className="sp-chips">
                {subjectsChips.map((s, i) => (
                  <span key={i} className="sp-chip">
                    {s}
                  </span>
                ))}
              </div>
            )}
          </label>
          <label className="sp-field">
            <span>{t("app.student.profile.fields.avoidSubjects")}</span>
            <input
              name="avoidSubjects"
              value={form.avoidSubjects}
              onChange={onChange}
              placeholder={P.avoidSubjects}
            />
          </label>
        </div>
      </section>

      {/* PREFERENCES */}
      <section className="sp-section">
        <h4 className="sp-section-title">{t("app.student.profile.sections.preferences")}</h4>
        <div className="sp-grid two">
          <label className="sp-field">
            <span>{t("app.student.profile.fields.learningStyle")}</span>
            <select name="learningStyle" value={form.learningStyle} onChange={onChange}>
              <option value="visual">{L.visual}</option>
              <option value="auditory">{L.auditory}</option>
              <option value="kinesthetic">{L.kinesthetic}</option>
              <option value="mixed">{L.mixed}</option>
            </select>
          </label>
          <label className="sp-field">
            <span>{t("app.student.profile.fields.city")}</span>
            <input name="city" value={form.city} onChange={onChange} placeholder={P.city} />
          </label>
        </div>

        <div className="sp-field">
          <span>{t("app.student.profile.fields.meetingMode")}</span>
          <div className="sp-checks">
            <Check
              label={M.online}
              name="online"
              checked={form.meetingMode.online}
              onChange={() => onToggle("meetingMode", "online")}
            />
            <Check
              label={M.onsite}
              name="onsite"
              checked={form.meetingMode.onsite}
              onChange={() => onToggle("meetingMode", "onsite")}
            />
            <Check
              label={M.hybrid}
              name="hybrid"
              checked={form.meetingMode.hybrid}
              onChange={() => onToggle("meetingMode", "hybrid")}
            />
          </div>
        </div>

        <div className="sp-field">
          <span>{t("app.student.profile.fields.preferredTools")}</span>
          <div className="sp-checks">
            <Check
              label={TL.meet}
              name="meet"
              checked={form.preferredTools.meet}
              onChange={() => onToggle("preferredTools", "meet")}
            />
            <Check
              label={TL.zoom}
              name="zoom"
              checked={form.preferredTools.zoom}
              onChange={() => onToggle("preferredTools", "zoom")}
            />
            <Check
              label={TL.teams}
              name="teams"
              checked={form.preferredTools.teams}
              onChange={() => onToggle("preferredTools", "teams")}
            />
            <Check
              label={TL.other}
              name="other"
              checked={form.preferredTools.other}
              onChange={() => onToggle("preferredTools", "other")}
            />
          </div>
          {form.preferredTools.other && (
            <input
              className="sp-input-inline"
              name="otherTool"
              value={form.otherTool}
              onChange={onChange}
              placeholder={P.otherTool}
            />
          )}
        </div>
      </section>

      {/* GUARDIAN */}
      <section className="sp-section">
        <h4 className="sp-section-title">{t("app.student.profile.sections.guardian")}</h4>
        <div className="sp-grid two">
          <label className="sp-field">
            <span>{t("app.student.profile.fields.guardianName")}</span>
            <input
              name="guardianName"
              value={form.guardianName}
              onChange={onChange}
              placeholder={P.guardianName}
            />
          </label>
          <label className="sp-field">
            <span>{t("app.student.profile.fields.guardianEmail")}</span>
            <input
              name="guardianEmail"
              value={form.guardianEmail}
              onChange={onChange}
              placeholder={P.guardianEmail}
            />
            {errors.guardianEmail && <div className="sp-error">{errors.guardianEmail}</div>}
          </label>
        </div>
      </section>

      {/* CONSENT */}
      <section className="sp-section">
        <h4 className="sp-section-title">{t("app.student.profile.sections.consent")}</h4>
        <div className="sp-checks">
          <Check
            label={t("app.student.profile.fields.shareProfile")}
            name="shareProfile"
            checked={form.shareProfile}
            onChange={() => setForm((f) => ({ ...f, shareProfile: !f.shareProfile }))}
          />
        </div>
      </section>

      <div className="sp-actions">
        <Button variant="primary" onClick={onSave}>
          {t("app.student.profile.save")}
        </Button>
      </div>
    </div>
  );
}
