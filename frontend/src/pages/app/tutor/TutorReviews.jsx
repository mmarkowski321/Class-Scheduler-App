import { useEffect, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import "./tutor-reviews.css";

// Simple Star component
function Star({ className, size = 20 }) {
  return (
    <svg 
      className={className} 
      width={size} 
      height={size} 
      viewBox="0 0 24 24" 
      fill="none" 
      stroke="currentColor" 
      strokeWidth="2" 
      strokeLinecap="round" 
      strokeLinejoin="round"
    >
      <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
    </svg>
  );
}

export default function TutorReviews() {
  const { t, i18n } = useTranslation("common");
  const [sortBy, setSortBy] = useState("newest");
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const tutorId = useMemo(() => localStorage.getItem("userId"), []);
  const token = useMemo(() => localStorage.getItem("token") || localStorage.getItem("access_token"), []);

  useEffect(() => {
    const loadReviews = async () => {
      if (!tutorId) {
        setReviews([]);
        setLoading(false);
        return;
      }
      setLoading(true);
      try {
        const response = await fetch(`/api/reviews/tutor/${tutorId}`, {
          headers: {
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
            "Accept-Language": i18n.language || "pl",
          },
        });
        if (!response.ok) {
          throw new Error("Failed to load reviews");
        }
        const data = await response.json();
        setReviews(Array.isArray(data) ? data : []);
      } catch (error) {
        console.error("Failed to fetch tutor reviews:", error);
        setReviews([]);
      } finally {
        setLoading(false);
      }
    };

    loadReviews();
  }, [tutorId, token, i18n.language]);

  const averageRating = useMemo(() => {
    if (!reviews.length) return "0.0";
    const sum = reviews.reduce((acc, review) => acc + (review.tutorRating || 0), 0);
    return (sum / reviews.length).toFixed(1);
  }, [reviews]);

  const formatDate = (dateString) => {
    if (!dateString) return "";
    const date = new Date(dateString);
    if (Number.isNaN(date.getTime())) {
      return dateString;
    }
    return date.toLocaleDateString(i18n.language === "pl" ? "pl-PL" : "en-US", {
      year: "numeric",
      month: "long",
      day: "numeric",
    });
  };

  const sortedReviews = useMemo(() => {
    const withDates = reviews.map((review) => ({
      ...review,
      _date: review.studentReviewAt || review.createdAt || review.updatedAt,
    }));
    return withDates.sort((a, b) => {
      const aDate = a._date ? new Date(a._date).getTime() : 0;
      const bDate = b._date ? new Date(b._date).getTime() : 0;
      return sortBy === "newest" ? bDate - aDate : aDate - bDate;
    });
  }, [reviews, sortBy]);

  const renderStars = (rating) => {
    return Array.from({ length: 5 }, (_, i) => (
      <Star
        key={i}
        className={`star ${i < rating ? 'filled' : 'empty'}`}
        size={20}
      />
    ));
  };

  return (
    <div className="tutor-reviews">
      <div className="reviews-header">
        <h2>{t("app.tutor.reviews.title")}</h2>
      </div>

      <div className="average-rating-section">
        <div className="rating-display">
          <div className="rating-number">{averageRating}</div>
          <div className="rating-stars">{renderStars(Math.round(Number(averageRating)))}</div>
          <div className="rating-count">
            {reviews.length} {reviews.length === 1 ? t("app.tutor.reviews.review") : t("app.tutor.reviews.reviews")}
          </div>
        </div>
      </div>

      <div className="sort-section">
        <label>{t("app.tutor.reviews.sortBy")}:</label>
        <div className="sort-toggle">
          <button
            className={`sort-btn ${sortBy === "newest" ? "active" : ""}`}
            onClick={() => setSortBy("newest")}
          >
            {t("app.tutor.reviews.newest")}
          </button>
          <button
            className={`sort-btn ${sortBy === "oldest" ? "active" : ""}`}
            onClick={() => setSortBy("oldest")}
          >
            {t("app.tutor.reviews.oldest")}
          </button>
        </div>
      </div>

      <div className="reviews-list">
        {loading ? (
          <div className="no-reviews">{t("loading", { defaultValue: "Loading..." })}</div>
        ) : sortedReviews.length === 0 ? (
          <div className="no-reviews">{t("app.tutor.reviews.noReviews")}</div>
        ) : (
          sortedReviews.map((review) => (
            <div key={review.id} className="review-card">
              <div className="review-header">
                <div className="review-student">
                  {review.student?.firstName} {review.student?.lastName}
                </div>
                <div className="review-date">{formatDate(review._date || review.createdAt)}</div>
              </div>
              <div className="review-rating">{renderStars(review.tutorRating || 0)}</div>
              {review.comment && <div className="review-comment">{review.comment}</div>}
              {review.tutorComment && (
                <div className="review-tutor-note">
                  <strong>{t("app.tutor.reviews.tutorNote")}:</strong> {review.tutorComment}
                </div>
              )}
            </div>
          ))
        )}
      </div>
    </div>
  );
}
