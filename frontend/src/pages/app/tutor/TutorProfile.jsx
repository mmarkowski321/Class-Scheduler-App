import { useEffect, useMemo, useRef, useState } from "react";
import { useTranslation } from "react-i18next";
import Button from "../../../components/ui/Button";
import "./tutor-profile.css";
import { SUBJECTS, normalizeSubject } from "../../../data/subjects";

const splitSubjects = (raw = "") =>
  raw
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);

const normalizeSubjectsString = (raw = "") => {
  const subjectTokens = splitSubjects(raw);
  const normalized = [];
  const extras = [];
  subjectTokens.forEach((token) => {
    const canonical = normalizeSubject(token);
    if (canonical) {
      if (!normalized.includes(canonical)) {
        normalized.push(canonical);
      }
    } else if (!extras.includes(token)) {
      extras.push(token);
    }
  });
  return [...normalized, ...extras].join(", ");
};

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
  const [isEditing, setIsEditing] = useState(false);
  const fileInputRef = useRef(null);

  const onChange = (e) =>
    setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const onToggle = (group, key) =>
    setForm((f) => ({ ...f, [group]: { ...f[group], [key]: !f[group][key] } }));

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
    subjectOptions.forEach((entry) => map.set(entry.value, entry.label));
    return map;
  }, [subjectOptions]);

  const recognizedSubjects = useMemo(() => {
    const acc = [];
    splitSubjects(form.subjects).forEach((token) => {
      const canonical = normalizeSubject(token);
      if (canonical && !acc.includes(canonical)) {
        acc.push(canonical);
      }
    });
    return acc;
  }, [form.subjects]);

  const customSubjects = useMemo(() => {
    const acc = [];
    splitSubjects(form.subjects).forEach((token) => {
      const canonical = normalizeSubject(token);
      if (!canonical) {
        const trimmed = token.trim();
        if (trimmed && !acc.includes(trimmed)) {
          acc.push(trimmed);
        }
      }
    });
    return acc;
  }, [form.subjects]);

  const subjectChips = useMemo(() => {
    const chips = [];
    recognizedSubjects.forEach((value) => {
      chips.push(subjectLabelMap.get(value) || value);
    });
    customSubjects.forEach((value) => chips.push(value));
    return chips;
  }, [recognizedSubjects, customSubjects, subjectLabelMap]);

  const langsList = useMemo(
    () => form.languages.split(",").map((s) => s.trim()).filter(Boolean),
    [form.languages]
  );

  const selectedModes = useMemo(
    () =>
      Object.entries(form.modes)
        .filter(([, value]) => value)
        .map(([key]) => key),
    [form.modes]
  );

  const isOnsiteMode = selectedModes.includes("onsite") || selectedModes.includes("hybrid");
  const [customSubjectInput, setCustomSubjectInput] = useState("");

  const handleSubjectsSelect = (event) => {
    const selected = Array.from(event.target.selectedOptions).map((opt) => opt.value);
    const combined = Array.from(new Set([...selected, ...customSubjects]));
    setForm((f) => ({ ...f, subjects: combined.join(", ") }));
  };

  const handleCustomSubjectAdd = () => {
    const trimmed = customSubjectInput.trim();
    if (!trimmed) return;
    const canonical = normalizeSubject(trimmed);
    const current = new Set([...recognizedSubjects, ...customSubjects]);
    if (canonical) {
      current.add(canonical);
    } else {
      current.add(trimmed);
    }
    setForm((f) => ({ ...f, subjects: Array.from(current).join(", ") }));
    setCustomSubjectInput("");
  };

  const handleCustomSubjectKeyDown = (event) => {
    if (event.key === "Enter") {
      event.preventDefault();
      handleCustomSubjectAdd();
    }
  };

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
  useEffect(() => {
    const token = localStorage.getItem("token");
    const userId = localStorage.getItem("userId");
    if (!token || !userId) return;
    (async () => {
      try {
        const res = await fetch(`/api/profile/${userId}`, { headers: { Authorization: `Bearer ${token}` } });
        if (!res.ok) return;
        const p = await res.json();
        setForm((f) => {
          const mergedSubjects = normalizeSubjectsString(p.subjects || "");

          return {
          ...f,
          education: p.education || "",
          experienceYears: p.experienceYears ?? "",
          subjects: mergedSubjects,
          exams: p.examResults || "",
          hourlyRate: p.hourlyRate ?? "",
          lessonDuration: p.lessonDuration ?? "60",
          modes: p.lessonModes ? JSON.parse(p.lessonModes) : f.modes,
          city: p.city || "",
          travelRadiusKm: p.travelRadius ?? "",
          languages: p.teachingLanguages || f.languages,
          methods: p.teachingMethods || "",
          bio: p.bio || "",
          certificates: p.certificates || "",
          website: p.website || "",
          linkedin: p.linkedIn || "",
          // preferredDays etc. są w Tutor modelu, ale nie ma UI tutaj – pomijamy
        };
        });
      } catch {}
    })();
  }, []);

  const onSave = async () => {
    const eMap = {};
    const trimmedEducation = form.education.trim();
    if (!trimmedEducation) {
      eMap.education = t("app.tutor.profile.validation.education");
    }

    const experienceValue = form.experienceYears ? parseFloat(form.experienceYears) : NaN;
    if (!Number.isFinite(experienceValue) || experienceValue < 0) {
      eMap.experienceYears = t("app.tutor.profile.validation.experienceYears");
    }

    const subjectsCount = form.subjects
      .split(",")
      .map((s) => s.trim())
      .filter(Boolean).length;
    if (!subjectsCount) {
      eMap.subjects = t("app.tutor.profile.validation.subjects");
    }

    const hourlyValue = form.hourlyRate ? parseFloat(form.hourlyRate) : NaN;
    if (!Number.isFinite(hourlyValue) || hourlyValue <= 0) {
      eMap.hourlyRate = t("app.tutor.profile.validation.hourlyRate");
    }

    const durationValue = form.lessonDuration ? parseInt(form.lessonDuration, 10) : NaN;
    if (!Number.isFinite(durationValue) || durationValue <= 0) {
      eMap.lessonDuration = t("app.tutor.profile.validation.lessonDuration");
    }

    if (!form.languages.trim()) {
      eMap.languages = t("app.tutor.profile.validation.languages");
    }

    if (!selectedModes.length) {
      eMap.lessonModes = t("app.tutor.profile.validation.lessonModes");
    }

    if (isOnsiteMode && !form.city.trim()) {
      eMap.city = t("app.tutor.profile.validation.city");
    }

    const travelValue = form.travelRadiusKm ? parseInt(form.travelRadiusKm, 10) : NaN;
    if (isOnsiteMode && (!Number.isFinite(travelValue) || travelValue <= 0)) {
      eMap.travelRadiusKm = t("app.tutor.profile.validation.travelRadius");
    }

    if (!form.methods.trim()) {
      eMap.methods = t("app.tutor.profile.validation.methods");
    }

    if (!form.bio.trim()) {
      eMap.bio = t("app.tutor.profile.validation.bio");
    }

    if (Object.keys(eMap).length) {
      setErrors(eMap);
      return;
    }
    setErrors({});

    try {
      const token = localStorage.getItem("token");
      const userId = localStorage.getItem("userId");
      if (!token || !userId) return;
      const experienceYears = Number.isFinite(experienceValue) ? Math.max(0, Math.round(experienceValue)) : null;
      const hourlyRate = Number.isFinite(hourlyValue) ? parseFloat(hourlyValue.toFixed(2)) : null;
      const lessonDuration = Number.isFinite(durationValue) ? Math.max(1, durationValue) : null;
      const travelRadius =
        Number.isFinite(travelValue) && travelValue > 0 ? Math.round(travelValue) : null;
      const payload = {
        education: trimmedEducation,
        experienceYears,
        subjects: form.subjects
          .split(",")
          .map((s) => s.trim())
          .filter(Boolean)
          .join(", "),
        examResults: form.exams,
        hourlyRate,
        lessonDuration,
        teachingLanguages: form.languages.trim(),
        lessonModes: JSON.stringify(form.modes),
        city: form.city.trim(),
        travelRadius,
        teachingMethods: form.methods.trim(),
        bio: form.bio.trim(),
        certificates: form.certificates,
        website: form.website,
        linkedIn: form.linkedin,
      };
      const res = await fetch(`/api/profile/tutor/${userId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
        body: JSON.stringify(payload),
      });
      if (!res.ok) throw new Error("save failed");
      setIsEditing(false);
      // reload
      const r2 = await fetch(`/api/profile/${userId}`, { headers: { Authorization: `Bearer ${token}` } });
      if (r2.ok) {
        const p = await r2.json();
        setForm((f) => ({
          ...f,
          education: p.education || "",
          experienceYears: p.experienceYears ?? "",
        subjects: normalizeSubjectsString(p.subjects || ""),
          exams: p.examResults || "",
          hourlyRate: p.hourlyRate ?? "",
          lessonDuration: p.lessonDuration ?? "60",
          modes: p.lessonModes ? JSON.parse(p.lessonModes) : f.modes,
          city: p.city || "",
          travelRadiusKm: p.travelRadius ?? "",
          languages: p.teachingLanguages || f.languages,
          methods: p.teachingMethods || "",
          bio: p.bio || "",
          certificates: p.certificates || "",
          website: p.website || "",
          linkedin: p.linkedIn || "",
        }));
      }
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
    <div className="tutor-profile-wrap">
    <div className="tutor-card">
      {/* Header */}
      <div className="tutor-header">
        <img className="avatar" src={avatarSrc} alt="" />
        <div>
          <h3>{t("app.tutor.profile.title")}</h3>
          <p>{t("app.tutor.profile.basic")}</p>
        </div>
      </div>

      <div className="required-note">
        <span
          className="required-badge"
          aria-hidden="true"
        >
          *
        </span>
        <span>{t("app.tutor.profile.requiredNotice")}</span>
      </div>

      {/* PODSTAWOWE */}
      <fieldset disabled={!isEditing} className="tutor-fieldset">
      <div className="tutor-section">
        <h4 className="title">{t("app.tutor.profile.basic")}</h4>
        <div className="form-single-column">
          <div className="field">
            <label>
              {t("app.tutor.profile.education")}
              <span
                className="required-badge"
                title={t("app.tutor.profile.requiredLabel")}
                aria-hidden="true"
              >
                *
              </span>
            </label>
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
            <label>
              {t("app.tutor.profile.experience")}
              <span
                className="required-badge"
                title={t("app.tutor.profile.requiredLabel")}
                aria-hidden="true"
              >
                *
              </span>
            </label>
            <input
              name="experienceYears"
              inputMode="numeric"
              value={form.experienceYears}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.experience")}
            />
            {errors.experienceYears && <div className="field-error">{errors.experienceYears}</div>}
          </div>

          {/* Uploader zdjęcia */}
          <div className="field photo-field">
            <label>
              {t("app.tutor.profile.photoUpload")}
              <span className="optional-badge">{t("app.tutor.profile.optionalTag")}</span>
            </label>
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
            <label>
              {t("app.tutor.profile.subjects")}
              <span
                className="required-badge"
                title={t("app.tutor.profile.requiredLabel")}
                aria-hidden="true"
              >
                *
              </span>
            </label>
            <select
              multiple
              value={recognizedSubjects}
              onChange={handleSubjectsSelect}
              className="select"
            >
              {subjectOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <small className="hint">{t("app.tutor.profile.subjectsHintSelect")}</small>
            {!!subjectChips.length && (
              <div className="chips">
                {subjectChips.map((label, index) => (
                  <span key={index} className="chip">
                    {label}
                  </span>
                ))}
              </div>
            )}
            {errors.subjects && <div className="field-error">{errors.subjects}</div>}
          </div>
          <div className="field">
            <label>
              {t("app.tutor.profile.subjectsCustom")}
              <span className="optional-badge">{t("app.tutor.profile.optionalTag")}</span>
            </label>
            <div style={{ display: "flex", gap: "12px" }}>
              <input
                value={customSubjectInput}
                onChange={(e) => setCustomSubjectInput(e.target.value)}
                onKeyDown={handleCustomSubjectKeyDown}
                placeholder={t("app.tutor.profile.subjectsCustomPlaceholder")}
              />
              <button type="button" className="btn-secondary" onClick={handleCustomSubjectAdd}>
                {t("app.tutor.profile.subjectsCustomAdd")}
              </button>
            </div>
            <small className="hint">{t("app.tutor.profile.subjectsCustomHint")}</small>
          </div>

          <div className="field">
            <label>
              {t("app.tutor.profile.exams")}
              <span className="optional-badge">{t("app.tutor.profile.optionalTag")}</span>
            </label>
            <input
              name="exams"
              value={form.exams}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.exams")}
            />
            <small className="hint">{t("app.tutor.profile.examsHint")}</small>
          </div>

          <div className="field">
            <label>
              {t("app.tutor.profile.hourly")}
              <span
                className="required-badge"
                title={t("app.tutor.profile.requiredLabel")}
                aria-hidden="true"
              >
                *
              </span>
            </label>
            <input
              name="hourlyRate"
              inputMode="decimal"
              value={form.hourlyRate}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.hourly")}
            />
            {errors.hourlyRate && <div className="field-error">{errors.hourlyRate}</div>}
          </div>

          <div className="field">
            <label>
              {t("app.tutor.profile.lessonDuration")}
              <span
                className="required-badge"
                title={t("app.tutor.profile.requiredLabel")}
                aria-hidden="true"
              >
                *
              </span>
            </label>
            <input
              name="lessonDuration"
              inputMode="numeric"
              value={form.lessonDuration}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.duration")}
            />
            {errors.lessonDuration && <div className="field-error">{errors.lessonDuration}</div>}
          </div>

          <div className="field">
            <label>
              {t("app.tutor.profile.languages")}
              <span
                className="required-badge"
                title={t("app.tutor.profile.requiredLabel")}
                aria-hidden="true"
              >
                *
              </span>
            </label>
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
            {errors.languages && <div className="field-error">{errors.languages}</div>}
          </div>

          <div className="field">
            <label>
              {t("app.tutor.profile.modes")}
              <span
                className="required-badge"
                title={t("app.tutor.profile.requiredLabel")}
                aria-hidden="true"
              >
                *
              </span>
            </label>
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
            <small className="hint">{t("app.tutor.profile.modesHint")}</small>
            {errors.lessonModes && <div className="field-error">{errors.lessonModes}</div>}
          </div>

          <div className="field">
            <label>
              {t("app.tutor.profile.city")}
              {isOnsiteMode ? (
                <span
                  className="required-badge"
                  title={t("app.tutor.profile.requiredLabel")}
                  aria-hidden="true"
                >
                  *
                </span>
              ) : (
                <span className="optional-badge">{t("app.tutor.profile.optionalTag")}</span>
              )}
            </label>
            <input
              name="city"
              value={form.city}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.city")}
            />
            <small className="hint">
              {isOnsiteMode
                ? t("app.tutor.profile.cityHintRequired")
                : t("app.tutor.profile.cityHintOptional")}
            </small>
            {errors.city && <div className="field-error">{errors.city}</div>}
          </div>

          <div className="field">
            <label>
              {t("app.tutor.profile.radius")}
              {isOnsiteMode ? (
                <span
                  className="required-badge"
                  title={t("app.tutor.profile.requiredLabel")}
                  aria-hidden="true"
                >
                  *
                </span>
              ) : (
                <span className="optional-badge">{t("app.tutor.profile.optionalTag")}</span>
              )}
            </label>
            <input
              name="travelRadiusKm"
              inputMode="numeric"
              value={form.travelRadiusKm}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.radius")}
            />
            <small className="hint">
              {isOnsiteMode
                ? t("app.tutor.profile.travelHintRequired")
                : t("app.tutor.profile.travelHintOptional")}
            </small>
            {errors.travelRadiusKm && <div className="field-error">{errors.travelRadiusKm}</div>}
          </div>
        </div>
      </div>

      {/* METODY & BIO */}
      <div className="tutor-section">
        <h4 className="title">{t("app.tutor.profile.methods")}</h4>
        <div className="form-single-column">
          <div className="field">
            <label>
              {t("app.tutor.profile.teachingMethods")}
              <span
                className="required-badge"
                title={t("app.tutor.profile.requiredLabel")}
                aria-hidden="true"
              >
                *
              </span>
            </label>
            <textarea
              name="methods"
              rows={4}
              value={form.methods}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.methods")}
            />
            {errors.methods && <div className="field-error">{errors.methods}</div>}
          </div>

          <div className="field">
            <label>
              {t("app.tutor.profile.bio")}
              <span
                className="required-badge"
                title={t("app.tutor.profile.requiredLabel")}
                aria-hidden="true"
              >
                *
              </span>
            </label>
            <textarea
              name="bio"
              rows={3}
              value={form.bio}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.bio")}
            />
            {errors.bio && <div className="field-error">{errors.bio}</div>}
          </div>

          <div className="field">
            <label>
              {t("app.tutor.profile.certs")}
              <span className="optional-badge">{t("app.tutor.profile.optionalTag")}</span>
            </label>
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
            <label>
              {t("app.tutor.profile.website")}
              <span className="optional-badge">{t("app.tutor.profile.optionalTag")}</span>
            </label>
            <input
              name="website"
              value={form.website}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.website")}
            />
          </div>

          <div className="field">
            <label>
              {t("app.tutor.profile.linkedin")}
              <span className="optional-badge">{t("app.tutor.profile.optionalTag")}</span>
            </label>
            <input
              name="linkedin"
              value={form.linkedin}
              onChange={onChange}
              placeholder={t("app.tutor.profile.placeholders.linkedin")}
            />
          </div>
        </div>
      </div>
      </fieldset>

      {/* Floating action buttons bottom-right */}
      <div className="tutor-fab">
        {!isEditing ? (
          <Button variant="primary" onClick={() => setIsEditing(true)}>
            {t("actions.edit")}
          </Button>
        ) : (
          <>
            <Button variant="ghost" onClick={() => setIsEditing(false)}>
              {t("actions.cancel")}
            </Button>
            <Button variant="primary" onClick={onSave}>
              {t("app.tutor.profile.save")}
            </Button>
          </>
        )}
      </div>
    </div>
    </div>
  );
}
