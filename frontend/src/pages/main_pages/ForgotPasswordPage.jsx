import Navbar from "../../components/layout/Navbar";
import Footer from "../../components/layout/Footer";
import Container from "../../components/ui/Container";
import Button from "../../components/ui/Button";
import "./Auth.css";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate } from "react-router-dom";

function ForgotPasswordPage() {
  const { t } = useTranslation("common");
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(false);

  const handleChange = (e) => {
    setEmail(e.target.value);
    setErrors({});
  };

  const validate = () => {
    const newErrors = {};
    if (!email.trim()) {
      newErrors.email = t("login.errors.email", { defaultValue: "Email is required" });
    } else {
      const re = /\S+@\S+\.\S+/;
      if (!re.test(email)) {
        newErrors.email = t("login.errors.emailInvalid", { defaultValue: "Invalid email format" });
      }
    }
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
      const response = await fetch("/api/auth/forgot-password", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ email }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        setErrors({ form: errorData.message || t("errors.server") });
        setSubmitting(false);
        return;
      }

      setSuccess(true);
    } catch (error) {
      console.error("Forgot password error:", error);
      setErrors({ form: t("errors.server") });
      setSubmitting(false);
    }
  };

  return (
    <div className="page-layout">
      <Navbar />
      <main className="auth-main">
        <Container>
          <div className="auth-box login-box">
            <h2>{t("forgotPassword.title")}</h2>
            {success ? (
              <div className="auth-success">
                <p>{t("forgotPassword.success")}</p>
                <Link to="/login" className="link">
                  <Button variant="primary">{t("forgotPassword.backToLogin")}</Button>
                </Link>
              </div>
            ) : (
              <>
                <p className="auth-subtitle">{t("forgotPassword.subtitle")}</p>
                <form onSubmit={handleSubmit} className="auth-form">
                  <div className="field">
                    <label htmlFor="email">{t("login.email")}</label>
                    <input
                      id="email"
                      type="email"
                      name="email"
                      placeholder="you@example.com"
                      value={email}
                      onChange={handleChange}
                      required
                    />
                    {errors.email && <div className="field-error">{errors.email}</div>}
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
                      {submitting ? t("forgotPassword.submitting") : t("forgotPassword.sendLink")}
                    </Button>
                  </div>
                </form>

                <p className="auth-meta">
                  {t("forgotPassword.remember")}{" "}
                  <Link to="/login" className="link">
                    {t("forgotPassword.loginLink")}
                  </Link>
                </p>
              </>
            )}
          </div>
        </Container>
      </main>
      <Footer />
    </div>
  );
}

export default ForgotPasswordPage;

