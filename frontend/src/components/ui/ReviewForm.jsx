import { useState } from "react";
import { useTranslation } from "react-i18next";
import "./review-form.css";

// Simple Star component for ratings
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
      style={{ cursor: onClick ? 'pointer' : 'default' }}
    >
      <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
    </svg>
  );
}

export default function ReviewForm({ tutorName, lessonDate, onClose, onSubmit }) {
  const { t } = useTranslation("common");
  const [rating, setRating] = useState(0);
  const [hoveredRating, setHoveredRating] = useState(0);
  const [platformRating, setPlatformRating] = useState(0);
  const [hoveredPlatformRating, setHoveredPlatformRating] = useState(0);
  const [comment, setComment] = useState("");
  const [errors, setErrors] = useState({});

  const handleSubmit = (e) => {
    e.preventDefault();
    
    const newErrors = {};
    if (rating === 0) {
      newErrors.rating = t("components.reviewForm.errors.rating");
    }
    if (platformRating === 0) {
      newErrors.platformRating = t("components.reviewForm.errors.platformRating");
    }
    if (comment.trim().length < 10) {
      newErrors.comment = t("components.reviewForm.errors.commentLength");
    }

    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }

    onSubmit({ rating, platformRating, comment });
  };

  const renderStars = (currentRating, hovered, onRatingChange, onHoverEnter, onHoverLeave) => {
    return Array.from({ length: 5 }, (_, i) => (
      <Star
        key={i}
        className={`star ${i < (hovered || currentRating) ? 'filled' : ''}`}
        size={32}
        onClick={() => onRatingChange(i + 1)}
        onMouseEnter={() => onHoverEnter(i + 1)}
        onMouseLeave={() => onHoverLeave(0)}
      />
    ));
  };

  return (
    <div className="review-form-overlay">
      <div className="review-form">
        <div className="review-form-header">
          <h2>{t("components.reviewForm.title")}</h2>
          <button className="close-btn" onClick={onClose}>×</button>
        </div>

        <div className="review-form-tutor">
          <h3>{t("components.reviewForm.question").replace("{{tutorName}}", tutorName)}</h3>
          <p className="lesson-date">{t("components.reviewForm.date")}: {lessonDate}</p>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>{t("components.reviewForm.ratingLabel")} *</label>
            <div className="stars-container">
              {renderStars(rating, hoveredRating, setRating, setHoveredRating, () => setHoveredRating(0))}
              {rating > 0 && (
                <span className="rating-text">{t("components.reviewForm.ratingText").replace("{{rating}}", rating)}</span>
              )}
            </div>
            {errors.rating && <div className="error-message">{errors.rating}</div>}
          </div>

          <div className="form-group">
            <label>{t("components.reviewForm.platformRatingLabel")} *</label>
            <div className="stars-container">
              {renderStars(platformRating, hoveredPlatformRating, setPlatformRating, setHoveredPlatformRating, () => setHoveredPlatformRating(0))}
              {platformRating > 0 && (
                <span className="rating-text">{t("components.reviewForm.platformRatingText").replace("{{rating}}", platformRating)}</span>
              )}
            </div>
            {errors.platformRating && <div className="error-message">{errors.platformRating}</div>}
          </div>

          <div className="form-group">
            <label htmlFor="comment">{t("components.reviewForm.commentLabel")} *</label>
            <textarea
              id="comment"
              value={comment}
              onChange={(e) => setComment(e.target.value)}
              placeholder={t("components.reviewForm.commentPlaceholder")}
              rows={6}
              maxLength={200}
            />
            {errors.comment && <div className="error-message">{errors.comment}</div>}
            <div className="hint">{comment.length} / 200 {t("components.reviewForm.characters")}</div>
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

