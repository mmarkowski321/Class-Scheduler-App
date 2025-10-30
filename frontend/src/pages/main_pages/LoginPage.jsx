import Navbar from "../../components/layout/Navbar";
import Footer from "../../components/layout/Footer";
import Container from "../../components/ui/Container";
import Button from "../../components/ui/Button";
import "./Auth.css";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";

function LoginPage() {
  const { t } = useTranslation("common");
  const navigate = useNavigate();
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

  const handleSubmit = async (e) => {
    e.preventDefault();
    const validation = validate();
    if (Object.keys(validation).length > 0) {
      setErrors(validation);
      return;
    }

    setSubmitting(true);
    try {
      // Send login data to backend
      const response = await fetch("/api/auth/login", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email: formData.email,
          password: formData.password,
        }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        setErrors({ form: errorData.message || t("login.errors.invalid") });
        setSubmitting(false);
        return;
      }

      const data = await response.json();

      // Require token to proceed
      if (!data || !data.token) {
        localStorage.removeItem("token");
        localStorage.removeItem("userId");
        localStorage.removeItem("role");
        setErrors({ form: data?.message || t("login.errors.invalid") });
        setSubmitting(false);
        return;
      }

      // Store token and user info in localStorage
      localStorage.setItem("token", data.token);
      localStorage.setItem("userId", data.userId);
      localStorage.setItem("role", data.role);

      // Fetch profile to get first/last name (for sidebar display)
      try {
        const profRes = await fetch(`/api/profile/${data.userId}`, {
          headers: { Authorization: `Bearer ${data.token}` }
        });
        if (profRes.ok) {
          const prof = await profRes.json();
          if (prof?.firstName) localStorage.setItem("firstName", prof.firstName);
          if (prof?.lastName) localStorage.setItem("lastName", prof.lastName);
        }
      } catch {}

      // Redirect to appropriate dashboard
      if (data.role === "STUDENT") {
        navigate("/app/student");
      } else if (data.role === "TUTOR") {
        navigate("/app/tutor");
      } else {
        navigate("/");
      }
    } catch (error) {
      console.error("Login error:", error);
      setErrors({ form: t("login.errors.server") });
      setSubmitting(false);
    }
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

              {errors.form && <div className="form-error">{errors.form}</div>}

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
