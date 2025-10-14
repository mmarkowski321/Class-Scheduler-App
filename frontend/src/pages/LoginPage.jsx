import Navbar from "../components/layout/Navbar";
import Footer from "../components/layout/Footer";
import Container from "../components/ui/Container";
import Button from "../components/ui/Button";
import "./Auth.css";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";

function LoginPage() {
  const { t } = useTranslation("common");
  const [formData, setFormData] = useState({ email: "", password: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
    setErrors((prev) => ({ ...prev, [name]: undefined }));
  };

  const validate = () => {
    const newErrors = {};
    if (!formData.email.trim()) newErrors.email = t("login.errors.email");
    else {
      const re = /\S+@\S+\.\S+/;
      if (!re.test(formData.email)) newErrors.email = t("login.errors.emailInvalid");
    }

    if (!formData.password.trim()) newErrors.password = t("login.errors.password");
    return newErrors;
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const validation = validate();
    if (Object.keys(validation).length > 0) {
      setErrors(validation);
      return;
    }

    setSubmitting(true);
    console.log("Login:", formData);

    setTimeout(() => {
      setSubmitting(false);
      alert(t("login.success") || "Login successful!");
    }, 700);
  };

  return (
    <div className="page-layout">
      <Navbar />
      <main className="auth-main">
        <Container>
        <div className="auth-box login-box">
            <h2>{t("login.title")}</h2>

            <form onSubmit={handleSubmit} className="auth-form">
              <div className="field">
                <label htmlFor="email">{t("login.email")}</label>
                <input
                  id="email"
                  type="email"
                  name="email"
                  placeholder="you@example.com"
                  value={formData.email}
                  onChange={handleChange}
                  required
                />
                {errors.email && <div className="field-error">{errors.email}</div>}
              </div>

              <div className="field password-field">
                <label htmlFor="password">{t("login.password")}</label>
                <div className="password-wrap">
                  <input
                    id="password"
                    type={showPassword ? "text" : "password"}
                    name="password"
                    placeholder="••••••••"
                    value={formData.password}
                    onChange={handleChange}
                    required
                  />
                  <button
                    type="button"
                    className="eye-btn"
                    onClick={() => setShowPassword((prev) => !prev)}
                  >
                    {showPassword ? "🙈" : "👁️"}
                  </button>
                </div>
                {errors.password && (
                  <div className="field-error">{errors.password}</div>
                )}
              </div>

              <div className="auth-actions">
                <Button
                  type="submit"
                  variant="primary"
                  size="large"
                  className="auth-submit"
                  disabled={submitting}
                >
                  {submitting ? t("login.submitting") : t("login.button")}
                </Button>
              </div>
            </form>

            <p className="auth-meta">
              {t("login.noAccount")}{" "}
              <Link to="/register" className="link">
                {t("login.registerLink")}
              </Link>
            </p>

            <p className="auth-forgot">
              <a href="#" className="link">
                {t("login.forgotPassword")}
              </a>
            </p>
          </div>
        </Container>
      </main>

    </div>
  );
}

export default LoginPage;
