import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import Navbar from "../../components/layout/Navbar";
import Footer from "../../components/layout/Footer";
import Container from "../../components/ui/Container";
import Button from "../../components/ui/Button";
import { useTranslation } from "react-i18next";

function VerifyEmail() {
  const { t } = useTranslation("common");
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState("verifying"); // verifying, success, error
  const [message, setMessage] = useState("");
  const [hasVerified, setHasVerified] = useState(false);

  useEffect(() => {
    // Prevent duplicate verification calls
    if (hasVerified) {
      return;
    }
    
    const token = searchParams.get("token");
    
    if (!token) {
      setStatus("error");
      setMessage(t("verify.error.noToken"));
      return;
    }

    // Send verification request to backend
    console.log("Verifying token:", token);
    fetch(`/api/auth/verify?token=${token}`)
      .then((response) => {
        console.log("Response status:", response.status);
        if (!response.ok) {
          // Handle HTTP error
          return response.json().then(err => {
            console.log("Error from server:", err);
            throw new Error(err.error || 'Verification failed');
          });
        }
        return response.json();
      })
      .then((data) => {
        console.log("Response data:", data);
        setHasVerified(true); // Mark as verified to prevent duplicate calls
        
        if (data.message) {
          setStatus("success");
          setMessage(data.message);
          
          // Redirect to login after 5 seconds
          setTimeout(() => {
            navigate("/login");
          }, 5000);
        } else if (data.error) {
          setStatus("error");
          setMessage(data.error);
        }
      })
      .catch((error) => {
        console.error("Verification error:", error);
        setHasVerified(true); // Mark as attempted to prevent retries
        setStatus("error");
        setMessage(error.message || t("verify.error.server"));
      });
  }, [searchParams, navigate, t, hasVerified]);

  return (
    <div className="page-layout">
      <Navbar />
      <main className="auth-main">
        <Container>
          <div className="auth-box" style={{ textAlign: "center" }}>
            {status === "verifying" && (
              <>
                <h2>⏳ {t("verify.verifying")}</h2>
                <p>{t("verify.verifyingText")}</p>
              </>
            )}
            
            {status === "success" && (
              <>
                <h2 style={{ color: "#10b981", fontSize: "2rem" }}>✅ {t("verify.success")}</h2>
                <p style={{ fontSize: "1.1rem", margin: "20px 0" }}>{message || t("verify.successText")}</p>
                <div style={{ marginTop: "30px", padding: "20px", background: "rgba(16, 185, 129, 0.1)", borderRadius: "8px", border: "1px solid rgba(16, 185, 129, 0.3)" }}>
                  <p style={{ fontSize: "14px", color: "rgba(255,255,255,0.9)" }}>
                    {t("verify.redirecting")}... (5 {t("verify.seconds")})
                  </p>
                </div>
                <Button 
                  variant="primary" 
                  size="large" 
                  onClick={() => navigate("/login")}
                  style={{ marginTop: "20px" }}
                >
                  {t("verify.goToLogin")}
                </Button>
              </>
            )}
            
            {status === "error" && (
              <>
                <h2 style={{ color: "#ef4444" }}>❌ {t("verify.errorTitle")}</h2>
                <p>{message || t("verify.errorText")}</p>
                <Button 
                  variant="primary" 
                  size="large" 
                  onClick={() => navigate("/login")}
                  style={{ marginTop: "20px" }}
                >
                  {t("verify.goToLogin")}
                </Button>
              </>
            )}
          </div>
        </Container>
      </main>
      <Footer />
    </div>
  );
}

export default VerifyEmail;

