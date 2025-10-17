import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import Navbar from "../../components/layout/Navbar";
import Footer from "../../components/layout/Footer";
import Container from "../../components/ui/Container";
import Button from "../../components/ui/Button";
import { useTranslation } from "react-i18next";
import "./Auth.css";
import DateInput from "../../components/ui/DateInput";

function RegisterPage() {
  const { t, i18n } = useTranslation("common");
  const navigate = useNavigate();

  const [form, setForm] = useState({
    firstName: "",
    lastName: "",
    birthDate: "",
    email: "",
    password: "",
    confirmPassword: "",
    role: "", // nowość
  });

  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
    setErrors((err) => ({ ...err, [name]: undefined, form: undefined }));
  };

  const calculateAge = (dateString) => {
    const today = new Date();
    const birth = new Date(dateString);
    let age = today.getFullYear() - birth.getFullYear();
    const m = today.getMonth() - birth.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birth.getDate())) age--;
    return age;
  };

  const validate = () => {
    const err = {};
    if (!form.firstName.trim()) err.firstName = t("register.errors.firstName");
    if (!form.lastName.trim()) err.lastName = t("register.errors.lastName");
    if (!form.role) err.role = t("register.errors.role");
  
    if (!form.email) err.email = t("register.errors.email");
    else {
      const re = /\S+@\S+\.\S+/;
      if (!re.test(form.email)) err.email = t("register.errors.emailInvalid");
    }
  
    if (!form.password) err.password = t("register.errors.password");
    else if (form.password.length < 8)
      err.password = t("register.errors.passwordShort");
  
    if (!form.confirmPassword)
      err.confirmPassword = t("register.errors.confirmPassword");
    else if (form.password !== form.confirmPassword)
      err.form = t("register.errors.passwordsMismatch");
  
    // role-based age check
    if (!form.birthDate) {
      err.birthDate = t("register.errors.birthDate");
    } else {
      const age = calculateAge(form.birthDate);
      if (form.role === "tutor" && age < 18) {
        err.birthDate = t("register.errors.ageTutor");    // e.g. "You must be at least 18 to register as a tutor."
      } else if (form.role === "student" && age < 13) {
        err.birthDate = t("register.errors.ageStudent");  // e.g. "You must be at least 13 to register as a student."
      }
    }
  
    return err;
  };
  const today = new Date();
  const yearsAgo = (n) => new Date(today.getFullYear() - n, today.getMonth(), today.getDate());
  const roleMinAge = form.role === "tutor" ? 18 : 13;

  const roleHintKey =
    form.role === "tutor"
      ? "register.hints.role.tutor"
      : form.role === "student"
      ? "register.hints.role.student"
      : "register.hints.role.general";

  const birthDateHintKey =
    form.role === "tutor"
      ? "register.hints.birthDate.tutor"
      : form.role === "student"
      ? "register.hints.birthDate.student"
      : "register.hints.birthDate.general";

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validation = validate();
    if (Object.keys(validation).length > 0) {
      setErrors(validation);
      return;
    }

    setSubmitting(true);
    try {
      console.log("Form data:", form);
      setTimeout(() => {
        setSubmitting(false);
        navigate("/login");
      }, 700);
    } catch {
      setSubmitting(false);
      setErrors({ form: t("register.errors.server") });
    }
  };

  return (
    <div className="page-layout">
      <Navbar />
      <main className="auth-main">
        <Container>
          <div className="auth-box auth-register">
            <h2>{t("register.title")}</h2>
            <form onSubmit={!submitting ? handleSubmit : undefined} className="auth-form" noValidate>
              <div className="row ">
                <div className="field">
                  <label htmlFor="firstName">{t("register.firstName")}</label>
                  <input
                    id="firstName"
                    name="firstName"
                    value={form.firstName}
                    onChange={handleChange}
                    placeholder={t("register.firstNamePlaceholder")}
                    autoFocus
                    required
                  />
                  {errors.firstName && (
                    <div className="field-error">{errors.firstName}</div>
                  )}
                </div>

                <div className="field">
                  <label htmlFor="lastName">{t("register.lastName")}</label>
                  <input
                    id="lastName"
                    name="lastName"
                    value={form.lastName}
                    onChange={handleChange}
                    placeholder={t("register.lastNamePlaceholder")}
                    required
                  />
                  {errors.lastName && (
                    <div className="field-error">{errors.lastName}</div>
                  )}
                </div>
              </div>
              
              <DateInput
                label={t("register.birthDate")}
                value={form.birthDate}
                onChange={handleChange}
                error={errors.birthDate}
                lang={i18n.language}
                maxDate={yearsAgo(roleMinAge)}   
                hint={t(birthDateHintKey)}
              />

              <div className="field">
                <label htmlFor="role">{t("register.role")}</label>
                <select
                  id="role"
                  name="role"
                  value={form.role}
                  onChange={handleChange}
                  className="select-role"
                  required
                >
                  <option value="">{t("register.rolePlaceholder")}</option>
                  <option value="student">{t("register.roleStudent")}</option>
                  <option value="tutor">{t("register.roleTutor")}</option>
                </select>
                {errors.role && <div className="field-error">{errors.role}</div>}
              </div>

              <div className="field">
                <label htmlFor="email">{t("register.email")}</label>
                <input
                  id="email"
                  name="email"
                  type="email"
                  value={form.email}
                  onChange={handleChange}
                  placeholder={t("register.emailPlaceholder")}
                  required
                />
                {errors.email && (
                  <div className="field-error">{errors.email}</div>
                )}
              </div>

              <div className="field password-field">
                <label htmlFor="password">{t("register.password")}</label>
                <div className="password-wrap">
                  <input
                    id="password"
                    name="password"
                    type={showPassword ? "text" : "password"}
                    value={form.password}
                    onChange={handleChange}
                    placeholder={t("register.passwordPlaceholder")}
                    required
                  />
                  <button
                    type="button"
                    className="eye-btn"
                    onClick={() => setShowPassword((s) => !s)}
                  >
                    {showPassword ? "🙈" : "👁️"}
                  </button>
                </div>
                {errors.password && (
                  <div className="field-error">{errors.password}</div>
                )}
              </div>

              <div className="field password-field">
                <label htmlFor="confirmPassword">
                  {t("register.confirmPassword")}
                </label>
                <div className="password-wrap">
                  <input
                    id="confirmPassword"
                    name="confirmPassword"
                    type={showConfirm ? "text" : "password"}
                    value={form.confirmPassword}
                    onChange={handleChange}
                    placeholder={t("register.confirmPasswordPlaceholder")}
                    required
                  />
                  <button
                    type="button"
                    className="eye-btn"
                    onClick={() => setShowConfirm((s) => !s)}
                  >
                    {showConfirm ? "🙈" : "👁️"}
                  </button>
                </div>
                {errors.confirmPassword && (
                  <div className="field-error">{errors.confirmPassword}</div>
                )}
              </div>

              {errors.form && <div className="form-error">{errors.form}</div>}

              <div className="auth-actions">
                <Button
                  type="submit"
                  variant="primary"
                  size="large"
                  className="auth-submit"
                  disabled={submitting}
                >
                  <span className="btn-text">
                    {submitting ? t("register.submitting") : t("register.button")}
                  </span>
                </Button>
              </div>
            </form>

            <p className="auth-meta">
              {t("register.haveAccount")}{" "}
              <Link to="/login" className="link">
                {t("register.loginLink")}
              </Link>
            </p>
          </div>
        </Container>
      </main>
    </div>
  );
}

export default RegisterPage;
