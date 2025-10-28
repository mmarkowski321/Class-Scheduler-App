import { useState } from "react";
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
  const { t } = useTranslation("common");
  
  const [sortBy, setSortBy] = useState("newest"); // "newest" or "oldest"

  // Demo data - później będzie z API
  const reviews = [
    {
      id: 1,
      studentName: "Anna Kowalska",
      rating: 5,
      comment: "Świetny korepetytor! Bardzo mi pomogła w przygotowaniu do matury z matematyki.",
      date: "2024-03-15"
    },
    {
      id: 2,
      studentName: "Jan Nowak",
      rating: 4,
      comment: "Profesjonalnie prowadzone lekcje. Trochę brakowało mi więcej praktycznych przykładów.",
      date: "2024-03-10"
    },
    {
      id: 3,
      studentName: "Maria Wiśniewska",
      rating: 5,
      comment: "Najlepszy korepetytor! Cierpliwy, kompetentny i zawsze dobrze przygotowany.",
      date: "2024-03-05"
    },
    {
      id: 4,
      studentName: "Piotr Zieliński",
      rating: 5,
      comment: "Polecam! Bardzo profesjonalne podejście i świetne wyniki.",
      date: "2024-02-28"
    },
    {
      id: 5,
      studentName: "Katarzyna Szymańska",
      rating: 3,
      comment: "Dobra, ale mogło być lepiej. Trochę zbyt teoretyczne podejście dla mnie.",
      date: "2024-02-20"
    }
  ];

  // Obliczanie średniej oceny
  const averageRating = reviews.length > 0 
    ? (reviews.reduce((sum, review) => sum + review.rating, 0) / reviews.length).toFixed(1)
    : 0;

  // Sortowanie opinii
  const sortedReviews = [...reviews].sort((a, b) => {
    if (sortBy === "newest") {
      return new Date(b.date) - new Date(a.date);
    } else {
      return new Date(a.date) - new Date(b.date);
    }
  });

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    return date.toLocaleDateString('pl-PL', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  };

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

      {/* Średnia ocena */}
      <div className="average-rating-section">
        <div className="rating-display">
          <div className="rating-number">{averageRating}</div>
          <div className="rating-stars">
            {renderStars(Math.round(averageRating))}
          </div>
          <div className="rating-count">
            {reviews.length} {reviews.length === 1 ? t("app.tutor.reviews.review") : t("app.tutor.reviews.reviews")}
          </div>
        </div>
      </div>

      {/* Sortowanie */}
      <div className="sort-section">
        <label>{t("app.tutor.reviews.sortBy")}:</label>
        <div className="sort-toggle">
          <button
            className={`sort-btn ${sortBy === 'newest' ? 'active' : ''}`}
            onClick={() => setSortBy('newest')}
          >
            {t("app.tutor.reviews.newest")}
          </button>
          <button
            className={`sort-btn ${sortBy === 'oldest' ? 'active' : ''}`}
            onClick={() => setSortBy('oldest')}
          >
            {t("app.tutor.reviews.oldest")}
          </button>
        </div>
      </div>

      {/* Lista opinii */}
      <div className="reviews-list">
        {sortedReviews.length === 0 ? (
          <div className="no-reviews">
            {t("app.tutor.reviews.noReviews")}
          </div>
        ) : (
          sortedReviews.map((review) => (
            <div key={review.id} className="review-card">
              <div className="review-header">
                <div className="review-student">{review.studentName}</div>
                <div className="review-date">{formatDate(review.date)}</div>
              </div>
              <div className="review-rating">
                {renderStars(review.rating)}
              </div>
              <div className="review-comment">
                {review.comment}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
