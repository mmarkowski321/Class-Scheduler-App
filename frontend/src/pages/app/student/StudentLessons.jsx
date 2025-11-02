import { useState } from "react";
import { useTranslation } from "react-i18next";
import Button from "../../../components/ui/Button";
import ReviewForm from "../../../components/ui/ReviewForm";

const canReschedule = (startISO) => {
  const start = new Date(startISO).getTime();
  const now = Date.now();
  const hours = (start - now) / 36e5;
  return hours >= 24;
};

const isLessonCompleted = (startISO) => {
  const start = new Date(startISO).getTime();
  const now = Date.now();
  return now > start;
};

export default function StudentLessons() {
  const { t } = useTranslation("common");
  const [showReviewForm, setShowReviewForm] = useState(null);
  const [lessons, setLessons] = useState([
    { id:1, title:"Matematyka", start:"2025-10-25T17:00:00Z", tutor:"Jan Kowalski", joinUrl:"#", completed: true}
  ]); // TODO: fetch

  const handleReviewSubmit = async (reviewData) => {
    try {
      const token = localStorage.getItem("token");
      const userId = localStorage.getItem("userId");
      
      const response = await fetch("/api/reviews", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          lessonId: showReviewForm.lessonId,
          studentId: userId,
          tutorRating: reviewData.rating,
          platformRating: reviewData.platformRating,
          comment: reviewData.comment,
        }),
      });

      if (!response.ok) {
        const errorData = await response.json();
        console.error("Failed to submit review:", errorData);
        alert("Failed to submit review. Please try again.");
        return;
      }

      const savedReview = await response.json();
      console.log("Review submitted:", savedReview);
      setShowReviewForm(null);
      
      // Update lessons state to mark as reviewed
      setLessons((prevLessons) =>
        prevLessons.map((lesson) =>
          lesson.id === showReviewForm.lessonId
            ? { ...lesson, reviewed: true }
            : lesson
        )
      );
    } catch (error) {
      console.error("Error submitting review:", error);
      alert("An error occurred. Please try again.");
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('pl-PL', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <>
      <div className="card">
        <h3>Moje zajęcia</h3>
        {lessons.map(l => (
          <div key={l.id} className="item" style={{padding:"10px 0", borderBottom:"1px solid rgba(255,255,255,.1)"}}>
            <div><strong>{l.title}</strong> — z {l.tutor}</div>
            <div>Start: {formatDate(l.start)}</div>
            <div className="row" style={{marginTop:8}}>
              {isLessonCompleted(l.start) ? (
                <>
                  <a href={l.joinUrl} target="_blank" rel="noopener noreferrer" style={{padding:"8px 16px", background:"#10b981", borderRadius:"8px", textDecoration:"none", color:"white", fontSize:"14px", border:"none", fontWeight:"600", transition:"all 0.2s", boxShadow:"0 2px 8px rgba(16, 185, 129, 0.3)", display:"inline-block", textAlign:"center"}}>
                    🔗 {t("app.student.lessons.joinMeeting")}
                  </a>
                  <Button 
                    size="small" 
                    variant="primary" 
                    onClick={() => setShowReviewForm({ 
                      lessonId: l.id, 
                      tutorName: l.tutor, 
                      lessonDate: formatDate(l.start)
                    })}
                  >
                    ⭐ {t("app.student.lessons.leaveReview")}
                  </Button>
                </>
              ) : (
                <>
                  <a href={l.joinUrl}>Dołącz do spotkania</a>
                  <Button size="small" disabled={!canReschedule(l.start)} onClick={() => {/* TODO */}}>
                    Przełóż
                  </Button>
                </>
              )}
            </div>
          </div>
        ))}
        {!lessons.length && <div className="empty">Brak rezerwacji.</div>}
      </div>

      {showReviewForm && (
        <ReviewForm
          tutorName={showReviewForm.tutorName}
          lessonDate={showReviewForm.lessonDate}
          onClose={() => setShowReviewForm(null)}
          onSubmit={handleReviewSubmit}
        />
      )}
    </>
  );
}
