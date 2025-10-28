import { useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import Button from "../../../components/ui/Button";
import "./tutor-profile.css";

function Check({ label, name, checked, onChange }) {
  return (
    <label className="check">
      <input type="checkbox" name={name} checked={checked} onChange={onChange} />
      <span>{label}</span>
    </label>
  );
}

export default function TutorProfile() {
  const { t, i18n } = useTranslation("common");

  const [form, setForm] = useState({
    education: "",
    experienceYears: "",
    subjects: "",
    exams: "",
    hourlyRate: "",
    lessonDuration: "60",
    modes: { online: true, onsite: false, hybrid: false },
    city: "",
    travelRadiusKm: "",
    languages: "polski",
    methods: "",
    bio: "",
    certificates: "",
    website: "",
    linkedin: "",
    // zdjęcie:
    photoFile: null,
    photoPreview: "", // blob: URL do podglądu
  });

  const [errors, setErrors] = useState({});
  const fileInputRef = useRef(null);

  const onChange = (e) =>
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const onToggle = (group, key) =>
    setForm((f) => ({ ...f, [group]: { ...f[group], [key]: !f[group][key] } }));

  const subjectsList = useMemo(
    () => form.subjects.split(",").map((s) => s.trim()).filter(Boolean),
    [form.subjects]
  );
  const langsList = useMemo(
    () => form.languages.split(",").map((s) => s.trim()).filter(Boolean),
    [form.languages]
  );

  // Avatar w nagłówku (pokazuje podgląd jeśli jest wgrane zdjęcie)
  const avatarSrc = form.photoPreview
    ? form.photoPreview
    : `https://ui-avatars.com/api/?background=6a70e8&color=fff&name=Tutor`;

  // ---- Upload zdjęcia ----
  const pickFile = (file) => {
    if (!file) return;
    if (!file.type.startsWith("image/")) {
      setErrors((e) => ({ ...e, photo: t("app.tutor.profile.photoErrors.invalidType") }));
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setErrors((e) => ({ ...e, photo: t("app.tutor.profile.photoErrors.tooLarge") }));
      return;
    }
    const url = URL.createObjectURL(file);
    setForm((f) => ({ ...f, photoFile: file, photoPreview: url }));
    setErrors((e) => ({ ...e, photo: undefined }));
  };

  const handleFileChange = (e) => pickFile(e.target.files?.[0]);
  const prevent = (e) => { e.preventDefault(); e.stopPropagation(); };
  const handleDrop = (e) => { prevent(e); pickFile(e.dataTransfer.files?.[0]); };
  const clearPhoto = () =>
    setForm((f) => ({ ...f, photoFile: null, photoPreview: "" }));

  // ---- Zapis (multipart/form-data) ----
  const onSave = async () => {
    const eMap = {};
    if (!form.education.trim()) eMap.education = t("app.tutor.profile.validation.education");
    if (Object.keys(eMap).length) {
      setErrors(eMap);
      return;
    }

    try {
      const fd = new FormData();
      // tekstowe
      fd.append("education", form.education);
      fd.append("experienceYears", form.experienceYears);
      fd.append("subjects", form.subjects);
      fd.append("exams", form.exams);
      fd.append("hourlyRate", form.hourlyRate);
      fd.append("lessonDuration", form.lessonDuration);
      fd.append("city", form.city);
      fd.append("travelRadiusKm", form.travelRadiusKm);
      fd.append("languages", form.languages);
      fd.append("methods", form.methods);
      fd.append("bio", form.bio);
      fd.append("certificates", form.certificates);
      fd.append("website", form.website);
      fd.append("linkedin", form.linkedin);
      fd.append("maxLessonsPerDay", form.maxLessonsPerDay);
      fd.append("bufferMin", form.bufferMin);

      // checkboxy
      fd.append("modes", JSON.stringify(form.modes));
      fd.append("preferredDays", JSON.stringify(form.preferredDays));

      // plik
      if (form.photoFile) fd.append("photo", form.photoFile);

      // TODO: podłącz backend
      // await fetch('/api/tutor/profile', { method: 'POST', body: fd });

      console.log("FORM-DATA WYSŁANE:", {
        ...form,
        photoFile: form.photoFile ? form.photoFile.name : null,
      });
      alert(t("app.tutor.profile.savedDemo")); // "Zapisano (demo). Zobacz konsolę."
    } catch (err) {
      console.error(err);
      alert(t("app.tutor.profile.saveError"));
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

  const modeLabels = {
    online: t("app.tutor.profile.modesLabels.online"),
    onsite: t("app.tutor.profile.modesLabels.onsite"),
    hybrid: t("app.tutor.profile.modesLabels.hybrid"),
  };

  return (
    <div className="tutor-card">
      {/* Header */}
      <div className="tutor-header">
        <img className="avatar" src={avatarSrc} alt="" />
        <div>
          <h3>{t("app.tutor.profile.title")}</h3>
          <p>{t("app.tutor.profile.basic")}</p>
        </div>
      </div>

      {/* PODSTAWOWE */}
      <div className="tutor-section">
        <h4 className="title">{t("app.tutor.profile.basic")}</h4>
        <div className="form-single-column">
          <div className="field">
            <label>{t("app.tutor.profile.education")}</label>
            <input
              name="education"
              value={form.education}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.education")}
            />
            <small className="hint">{t("app.tutor.profile.educationHint")}</small>
            {errors.education && <div className="field-error">{errors.education}</div>}
          </div>

          <div className="field">
            <label>{t("app.tutor.profile.experience")}</label>
            <input
              name="experienceYears"
              inputMode="numeric"
              value={form.experienceYears}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.experience")}
            />
          </div>

          {/* Uploader zdjęcia */}
          <div className="field photo-field">
            <label>{t("app.tutor.profile.photoUpload")}</label>
            <div
              className={`photo-uploader ${form.photoPreview ? "has-image" : ""}`}
              onDragEnter={prevent}
              onDragOver={prevent}
              onDragLeave={prevent}
              onDrop={handleDrop}
            >
              {form.photoPreview ? (
                <img className="photo-preview" src={form.photoPreview} alt={t("app.tutor.profile.photoAlt")} />
              ) : (
                <div className="photo-placeholder">
                  <div className="photo-icon">📷</div>
                  <div className="photo-text">{t("app.tutor.profile.photoDrag")}</div>
                  <div className="photo-or">{t("app.tutor.profile.or")}</div>
                  <button
                    type="button"
                    className="btn-ghost"
                    onClick={() => fileInputRef.current?.click()}
                  >
                    {t("app.tutor.profile.photoPick")}
                  </button>
                </div>
              )}
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                onChange={handleFileChange}
                hidden
              />
            </div>

            <div className="uploader-actions">
              <button
                type="button"
                className="btn-secondary"
                onClick={() => fileInputRef.current?.click()}
              >
                {t("app.tutor.profile.photoChange")}
              </button>
              {form.photoPreview && (
                <button type="button" className="btn-danger" onClick={clearPhoto}>
                  {t("app.tutor.profile.photoRemove")}
                </button>
              )}
            </div>
            <small className="hint">{t("app.tutor.profile.photoHint")}</small>
            {errors.photo && <div className="field-error">{errors.photo}</div>}
          </div>
        </div>
      </div>

      {/* OFERTA */}
      <div className="tutor-section">
        <h4 className="title">{t("app.tutor.profile.offer")}</h4>
        <div className="form-single-column">
          <div className="field">
            <label>{t("app.tutor.profile.subjects")}</label>
            <input
              name="subjects"
              value={form.subjects}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.subjects")}
            />
            <small className="hint">{t("app.tutor.profile.subjectsHint")}</small>
            {!!subjectsList.length && (
              <div className="chips">
                {subjectsList.map((s, i) => (
                  <span key={i} className="chip">{s}</span>
                ))}
              </div>
            )}
          </div>

          <div className="field">
            <label>{t("app.tutor.profile.exams")}</label>
            <input
              name="exams"
              value={form.exams}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.exams")}
            />
            <small className="hint">{t("app.tutor.profile.examsHint")}</small>
          </div>

          <div className="field">
            <label>{t("app.tutor.profile.hourly")}</label>
            <input
              name="hourlyRate"
              inputMode="decimal"
              value={form.hourlyRate}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.hourly")}
            />
          </div>

          <div className="field">
            <label>{t("app.tutor.profile.lessonDuration")}</label>
            <input
              name="lessonDuration"
              inputMode="numeric"
              value={form.lessonDuration}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.duration")}
            />
          </div>

          <div className="field">
            <label>{t("app.tutor.profile.languages")}</label>
            <input
              name="languages"
              value={form.languages}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.languages")}
            />
            {!!langsList.length && (
              <div className="chips">
                {langsList.map((s, i) => (
                  <span key={i} className="chip">{s}</span>
                ))}
              </div>
            )}
          </div>

          <div className="field">
            <label>{t("app.tutor.profile.modes")}</label>
            <div className="checks">
              <Check
                label={modeLabels.online}
                name="online"
                checked={form.modes.online}
                onChange={() => onToggle("modes", "online")}
              />
              <Check
                label={modeLabels.onsite}
                name="onsite"
                checked={form.modes.onsite}
                onChange={() => onToggle("modes", "onsite")}
              />
              <Check
                label={modeLabels.hybrid}
                name="hybrid"
                checked={form.modes.hybrid}
                onChange={() => onToggle("modes", "hybrid")}
              />
            </div>
          </div>

          <div className="field">
            <label>{t("app.tutor.profile.city")}</label>
            <input
              name="city"
              value={form.city}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.city")}
            />
          </div>

          <div className="field">
            <label>{t("app.tutor.profile.radius")}</label>
            <input
              name="travelRadiusKm"
              inputMode="numeric"
              value={form.travelRadiusKm}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.radius")}
            />
          </div>
        </div>
      </div>

      {/* METODY & BIO */}
      <div className="tutor-section">
        <h4 className="title">{t("app.tutor.profile.methods")}</h4>
        <div className="form-single-column">
          <div className="field">
            <label>{t("app.tutor.profile.teachingMethods")}</label>
            <textarea
              name="methods"
              rows={4}
              value={form.methods}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.methods")}
            />
          </div>

          <div className="field">
            <label>{t("app.tutor.profile.bio")}</label>
            <textarea
              name="bio"
              rows={3}
              value={form.bio}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.bio")}
            />
          </div>

          <div className="field">
            <label>{t("app.tutor.profile.certs")}</label>
            <textarea
              name="certificates"
              rows={3}
              value={form.certificates}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.certs")}
            />
          </div>
        </div>
      </div>

      {/* LINKI & DOSTĘPNOŚĆ */}
      <div className="tutor-section">
        <h4 className="title">{t("app.tutor.profile.links")}</h4>
        <div className="form-single-column">
          <div className="field">
            <label>{t("app.tutor.profile.website")}</label>
            <input
              name="website"
              value={form.website}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.website")}
            />
          </div>

          <div className="field">
            <label>{t("app.tutor.profile.linkedin")}</label>
            <input
              name="linkedin"
              value={form.linkedin}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.linkedin")}
            />
          </div>
        </div>
      </div>

      {/* Akcja */}
      <div className="tutor-actions">
        <Button variant="primary" onClick={onSave}>
          {t("app.tutor.profile.save")}
        </Button>
      </div>
    </div>
  );
}
