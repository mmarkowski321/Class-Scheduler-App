import Navbar from "../../components/layout/Navbar";
import Footer from "../../components/layout/Footer";
import Container from "../../components/ui/Container";
import Button from "../../components/ui/Button";
import "./Auth.css";
import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Link, useNavigate, useSearchParams } from "react-router-dom";

function ResetPasswordPage() {
  const { t } = useTranslation("common");
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get("token");

  const [formData, setFormData] = useState({ newPassword: "", confirmPassword: "" });
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [tokenValid, setTokenValid] = useState(null);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    // Validate token on mount
    if (!token) {
      setTokenValid(false);
      return;
    }

    const validateToken = async () => {
      try {
        const response = await fetch(`/api/auth/validate-reset-token?token=${token}`);
        if (response.ok) {
          setTokenValid(true);
        } else {
          setTokenValid(false);
        }
      } catch (error) {
        setTokenValid(false);
      }
    };

    validateToken();
  }, [token]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData({ ...formData, [name]: value });
    setErrors((prev) => ({ ...prev, [name]: undefined, form: undefined }));
  };

  const validate = () => {
    const newErrors = {};
    if (!formData.newPassword.trim()) {
      newErrors.newPassword = t("resetPassword.errors.password");
    } else if (formData.newPassword.length < 8) {
      newErrors.newPassword = t("resetPassword.errors.passwordShort");
    }

    if (!formData.confirmPassword.trim()) {
      newErrors.confirmPassword = t("resetPassword.errors.confirmPassword");
    } else if (formData.newPassword !== formData.confirmPassword) {
      newErrors.confirmPassword = t("resetPassword.errors.mismatch");
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
      const response = await fetch("/api/auth/reset-password", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          token,
          newPassword: formData.newPassword,
          confirmPassword: formData.confirmPassword,
        }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        setErrors({ form: errorData.message || t("errors.server", { defaultValue: "An error occurred" }) });
        setSubmitting(false);
        return;
      }

      setSuccess(true);
      // Redirect to login after 3 seconds
      setTimeout(() => {
        navigate("/login");
      }, 3000);
    } catch (error) {
      console.error("Reset password error:", error);
      setErrors({ form: t("errors.server") });
      setSubmitting(false);
    }
  };

  if (tokenValid === null) {
    return (
      <div className="page-layout">
        <Navbar />
        <main className="auth-main">
          <Container>
            <div className="auth-box login-box">
              <h2>{t("resetPassword.title")}</h2>
              <p>{t("resetPassword.loading")}</p>
            </div>
          </Container>
        </main>
        <Footer />
      </div>
    );
  }

  if (tokenValid === false) {
    return (
      <div className="page-layout">
        <Navbar />
        <main className="auth-main">
          <Container>
            <div className="auth-box login-box">
              <h2>{t("resetPassword.title")}</h2>
              <div className="form-error">
                {t("resetPassword.invalidToken")}
              </div>
              <Link to="/forgot-password" className="link">
                <Button variant="primary">{t("resetPassword.requestNew")}</Button>
              </Link>
            </div>
          </Container>
        </main>
        <Footer />
      </div>
    );
  }

  return (
    <div className="page-layout">
      <Navbar />
      <main className="auth-main">
        <Container>
          <div className="auth-box login-box">
            <h2>{t("resetPassword.title")}</h2>
            {success ? (
              <div className="auth-success">
                <p>{t("resetPassword.success")}</p>
                <Link to="/login" className="link">
                  <Button variant="primary">{t("resetPassword.goToLogin")}</Button>
                </Link>
              </div>
            ) : (
              <>
                <p className="auth-subtitle">{t("resetPassword.subtitle")}</p>
                <form onSubmit={handleSubmit} className="auth-form">
                  <div className="field password-field">
                    <label htmlFor="newPassword">{t("resetPassword.newPassword")}</label>
                    <div className="password-wrap">
                      <input
                        id="newPassword"
                        type={showPassword ? "text" : "password"}
                        name="newPassword"
                        placeholder="••••••••"
                        value={formData.newPassword}
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
                    {errors.newPassword && <div className="field-error">{errors.newPassword}</div>}
                  </div>

                  <div className="field password-field">
                    <label htmlFor="confirmPassword">{t("resetPassword.confirmPassword")}</label>
                    <div className="password-wrap">
                      <input
                        id="confirmPassword"
                        type={showConfirm ? "text" : "password"}
                        name="confirmPassword"
                        placeholder="••••••••"
                        value={formData.confirmPassword}
                        onChange={handleChange}
                        required
                      />
                      <button
                        type="button"
                        className="eye-btn"
                        onClick={() => setShowConfirm((prev) => !prev)}
                      >
                        {showConfirm ? "🙈" : "👁️"}
                      </button>
                    </div>
                    {errors.confirmPassword && <div className="field-error">{errors.confirmPassword}</div>}
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
                      {submitting ? t("resetPassword.submitting") : t("resetPassword.reset")}
                    </Button>
                  </div>
                </form>

                <p className="auth-meta">
                  {t("resetPassword.remember")}{" "}
                  <Link to="/login" className="link">
                    {t("resetPassword.loginLink")}
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

export default ResetPasswordPage;

