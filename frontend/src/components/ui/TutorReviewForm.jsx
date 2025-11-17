import { useState } from "react";
import { useTranslation } from "react-i18next";
import "./review-form.css";

function Star({ className, size = 24, onClick, onMouseEnter, onMouseLeave }) {
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
      onClick={onClick}
      onMouseEnter={onMouseEnter}
      onMouseLeave={onMouseLeave}
      style={{ cursor: onClick ? "pointer" : "default" }}
    >
      <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
    </svg>
  );
}

export default function TutorReviewForm({ studentName, lessonDate, onClose, onSubmit }) {
  const { t } = useTranslation("common");
  const [studentRating, setStudentRating] = useState(0);
  const [hoveredStudentRating, setHoveredStudentRating] = useState(0);
  const [platformRating, setPlatformRating] = useState(0);
  const [hoveredPlatformRating, setHoveredPlatformRating] = useState(0);
  const [comment, setComment] = useState("");
  const [errors, setErrors] = useState({});

  const renderStars = (currentRating, hovered, onRatingChange, onHoverEnter, onHoverLeave) => {
    return Array.from({ length: 5 }, (_, i) => (
      <Star
        key={i}
        className={`star ${i < (hovered || currentRating) ? "filled" : ""}`}
        size={32}
        onClick={() => onRatingChange(i + 1)}
        onMouseEnter={() => onHoverEnter(i + 1)}
        onMouseLeave={() => onHoverLeave(0)}
      />
    ));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    const nextErrors = {};
    if (studentRating === 0) {
      nextErrors.studentRating = t("components.tutorReviewForm.errors.studentRating");
    }
    if (platformRating === 0) {
      nextErrors.platformRating = t("components.tutorReviewForm.errors.platformRating");
    }
    if (comment.trim().length < 10) {
      nextErrors.comment = t("components.tutorReviewForm.errors.commentLength");
    }

    if (Object.keys(nextErrors).length) {
      setErrors(nextErrors);
      return;
    }

    onSubmit({ studentRating, platformRating, comment: comment.trim() });
  };

  return (
    <div className="review-form-overlay">
      <div className="review-form">
        <div className="review-form-header">
          <h2>{t("components.tutorReviewForm.title")}</h2>
          <button className="close-btn" onClick={onClose}>
            ×
          </button>
        </div>

        <div className="review-form-tutor">
          <h3>{t("components.tutorReviewForm.question").replace("{{studentName}}", studentName)}</h3>
          <p className="lesson-date">
            {t("components.tutorReviewForm.date")}: {lessonDate}
          </p>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>{t("components.tutorReviewForm.studentRatingLabel")} *</label>
            <div className="stars-container">
              {renderStars(
                studentRating,
                hoveredStudentRating,
                setStudentRating,
                setHoveredStudentRating,
                () => setHoveredStudentRating(0)
              )}
              {studentRating > 0 && (
                <span className="rating-text">
                  {t("components.tutorReviewForm.studentRatingText").replace("{{rating}}", studentRating)}
                </span>
              )}
            </div>
            {errors.studentRating && <div className="error-message">{errors.studentRating}</div>}
          </div>

          <div className="form-group">
            <label>{t("components.tutorReviewForm.platformRatingLabel")} *</label>
            <div className="stars-container">
              {renderStars(
                platformRating,
                hoveredPlatformRating,
                setPlatformRating,
                setHoveredPlatformRating,
                () => setHoveredPlatformRating(0)
              )}
              {platformRating > 0 && (
                <span className="rating-text">
                  {t("components.tutorReviewForm.platformRatingText").replace("{{rating}}", platformRating)}
                </span>
              )}
            </div>
            {errors.platformRating && <div className="error-message">{errors.platformRating}</div>}
          </div>

          <div className="form-group">
            <label htmlFor="comment">{t("components.tutorReviewForm.commentLabel")} *</label>
            <textarea
              id="comment"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder={t("components.tutorReviewForm.commentPlaceholder")}
              rows={6}
              maxLength={400}
            />
            {errors.comment && <div className="error-message">{errors.comment}</div>}
            <div className="hint">
              {comment.length} / 400 {t("components.reviewForm.characters")}
            </div>
          </div>

          <div className="review-form-actions">
            <button type="button" className="btn-cancel" onClick={onClose}>
              {t("components.reviewForm.cancel")}
            </button>
            <button type="submit" className="btn-submit">
              {t("components.reviewForm.submit")}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}




