import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import Button from "../../../components/ui/Button";

export default function AdminReviews() {
  const { t } = useTranslation("common");
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const token = localStorage.getItem("token");

  useEffect(() => {
    fetchReviews();
  }, [token]);

  const fetchReviews = async () => {
    try {
      const response = await fetch("/api/admin/reviews", {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      if (response.ok) {
        const data = await response.json();
        setReviews(data);
      }
    } catch (error) {
      console.error("Failed to fetch reviews:", error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) return <div className="card">{t("app.admin.overview.loading")}</div>;

  return (
    <div className="card">
      <h2>{t("app.admin.reviews.title")} ({reviews.length})</h2>
      {reviews.length > 0 ? (
        <div style={{ marginTop: "16px" }}>
          {reviews.map((review) => (
            <div
              key={review.id}
              style={{
                padding: "16px",
                borderBottom: "1px solid rgba(255,255,255,0.1)",
                marginBottom: "12px",
                background: "rgba(255,255,255,0.05)",
                borderRadius: "8px",
              }}
            >
              <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px" }}>
                <div>
                  <strong>{t("app.admin.reviews.student")}:</strong> {review.student?.firstName} {review.student?.lastName} ({review.student?.email})
                </div>
                <div>
                  <strong>{t("app.admin.reviews.tutor")}:</strong> {review.tutor?.firstName} {review.tutor?.lastName} ({review.tutor?.email})
                </div>
              </div>
              <div style={{ marginTop: "12px" }}>
                <div><strong>{t("app.admin.reviews.tutorRating")}:</strong> {"⭐".repeat(review.tutorRating)} ({review.tutorRating}/5)</div>
                <div><strong>{t("app.admin.reviews.platformRating")}:</strong> {"⭐".repeat(review.platformRating)} ({review.platformRating}/5)</div>
                {review.studentBehaviorRating && (
                  <div><strong>{t("app.admin.reviews.studentBehaviorRating")}:</strong> {"⭐".repeat(review.studentBehaviorRating)} ({review.studentBehaviorRating}/5)</div>
                )}
              </div>
              {review.comment && (
                <div style={{ marginTop: "12px", padding: "12px", background: "rgba(255,255,255,0.1)", borderRadius: "4px" }}>
                  <strong>{t("app.admin.reviews.comment")}:</strong> {review.comment}
                </div>
              )}
              <div style={{ marginTop: "8px", fontSize: "12px", color: "#aaa" }}>
                {t("app.admin.reviews.created")}: {new Date(review.createdAt).toLocaleString()}
              </div>
            </div>
          ))}
        </div>
      ) : (
        <p>{t("app.admin.reviews.noReviews")}</p>
      )}
    </div>
  );
}

