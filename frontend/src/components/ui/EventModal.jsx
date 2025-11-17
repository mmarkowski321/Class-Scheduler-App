import { useTranslation } from "react-i18next";
import "./event-modal.css";

export default function EventModal({ event, onClose }) {
  const { t, i18n } = useTranslation("common");
  const locale = i18n.language === "pl" ? "pl" : "en";

  if (!event) return null;

  const deliveryMode = event.deliveryMode;
  const isOnline = deliveryMode === "ONLINE";
  const locationLine = [event.onsiteStreet, event.onsiteBuilding, event.onsiteApartment]
    .filter(Boolean)
    .join(" ");
  const cityLine = [event.onsitePostalCode, event.onsiteCity].filter(Boolean).join(" ");

  const formatDate = (dateString) => {
    if (!dateString) return "";
    const date = new Date(dateString);
    const dateStr = date.toLocaleDateString(locale === "pl" ? "pl-PL" : "en-US", {
      weekday: "short",
      day: "numeric",
      month: "short"
    });
    const timeStr = date.toLocaleTimeString(locale === "pl" ? "pl-PL" : "en-US", {
      hour: "2-digit",
      minute: "2-digit"
    });
    return `${dateStr}, ${timeStr}`;
  };

  const formatTime = (dateString) => {
    if (!dateString) return "";
    const date = new Date(dateString);
    return date.toLocaleTimeString(locale === "pl" ? "pl-PL" : "en-US", {
      hour: "2-digit",
      minute: "2-digit"
    });
  };

  const startTime = formatDate(event.start);
  const endTime = formatTime(event.end);
  
  const calculateDuration = () => {
    if (!event.start || !event.end) return "";
    const start = new Date(event.start);
    const end = new Date(event.end);
    const diffMs = end - start;
    const diffMins = Math.floor(diffMs / 60000);
    const hours = Math.floor(diffMins / 60);
    const mins = diffMins % 60;
    if (hours > 0) {
      return `${hours} ${locale === "pl" ? "godz." : "hr"} ${mins > 0 ? `${mins} ${locale === "pl" ? "min" : "min"}` : ""}`.trim();
    }
    return `${mins} ${locale === "pl" ? "min" : "min"}`;
  };

  const getStatusText = (status) => {
    if (!status) return "";
    const statusMap = {
      "SCHEDULED": locale === "pl" ? "Zaplanowana" : "Scheduled",
      "RESCHEDULED": locale === "pl" ? "Przełożona" : "Rescheduled",
      "IN_PROGRESS": locale === "pl" ? "W trakcie" : "In Progress",
      "COMPLETED": locale === "pl" ? "Zakończona" : "Completed",
      "CANCELLED": locale === "pl" ? "Anulowana" : "Cancelled"
    };
    return statusMap[status] || status;
  };

  const isLesson = event.type === "lesson";
  const isBusy = event.type === "busy";

  return (
    <div className="event-modal-overlay" onClick={onClose}>
      <div className="event-modal" onClick={(e) => e.stopPropagation()}>
        <button className="event-modal-close" onClick={onClose} aria-label={locale === "pl" ? "Zamknij" : "Close"}>
          ×
        </button>
        
        <div className="event-modal-header">
          <div className={`event-modal-icon ${isLesson ? "lesson" : "busy"}`}>
            {isLesson ? "📚" : "📅"}
          </div>
          <h2 className="event-modal-title">{event.title || (locale === "pl" ? "Wydarzenie" : "Event")}</h2>
        </div>

        <div className="event-modal-content">
          <div className="event-modal-info">
            <div className="event-modal-info-row">
              <div className="event-modal-info-item">
                <span className="event-modal-label">📅 {locale === "pl" ? "Data rozpoczęcia" : "Start"}</span>
                <span className="event-modal-value">{startTime}</span>
              </div>
              
              <div className="event-modal-info-item">
                <span className="event-modal-label">🕐 {locale === "pl" ? "Czas trwania" : "Duration"}</span>
                <span className="event-modal-value">{calculateDuration()}</span>
              </div>
            </div>
            
            <div className="event-modal-info-item">
              <span className="event-modal-label">⏰ {locale === "pl" ? "Zakończenie" : "End"}</span>
              <span className="event-modal-value">{endTime}</span>
            </div>

            {isLesson && event.status && (
              <div className="event-modal-info-item">
                <span className="event-modal-label">📊 {locale === "pl" ? "Status" : "Status"}</span>
                <span className={`event-modal-status event-modal-status-${event.status.toLowerCase()}`}>
                  {getStatusText(event.status)}
                </span>
              </div>
            )}

            {isLesson && event.meetingLink && (
              <div className="event-modal-info-item">
                <span className="event-modal-label">🔗 {locale === "pl" ? "Link do spotkania" : "Meeting Link"}</span>
                <a
                  href={event.meetingLink}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="event-modal-link"
                >
                  {locale === "pl" ? "Dołącz do spotkania" : "Join meeting"} →
                </a>
                <code className="event-modal-link-raw">{event.meetingLink}</code>
              </div>
            )}

            {isLesson && deliveryMode && (
              <div className="event-modal-info-item">
                <span className="event-modal-label">
                  🧭 {locale === "pl" ? "Tryb zajęć" : "Delivery mode"}
                </span>
                <span className="event-modal-value">
                  {isOnline ? (locale === "pl" ? "Online" : "Online") : (locale === "pl" ? "Stacjonarnie" : "Onsite")}
                </span>
              </div>
            )}

            {!isOnline && (locationLine || cityLine) && (
              <div className="event-modal-info-item">
                <span className="event-modal-label">
                  📍 {locale === "pl" ? "Adres" : "Address"}
                </span>
                <div className="event-modal-location">
                  {[locationLine, cityLine]
                    .filter(Boolean)
                    .map((line, idx) => (
                      <span key={`${line}-${idx}`}>{line}</span>
                    ))}
                </div>
              </div>
            )}

            {(event.description || event.notes) && (
              <div className="event-modal-info-item event-modal-description">
                <span className="event-modal-label">
                  📝 {isLesson ? (locale === "pl" ? "Notatki" : "Notes") : (locale === "pl" ? "Opis" : "Description")}
                </span>
                <div className="event-modal-description-text">
                  {(event.description || event.notes).split('\n').map((line, idx) => (
                    <p key={idx} style={{ margin: idx > 0 ? '12px 0 0 0' : '0' }}>
                      {line}
                    </p>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>

        <div className="event-modal-footer">
          <button className="event-modal-button" onClick={onClose}>
            {locale === "pl" ? "Zamknij" : "Close"}
          </button>
        </div>
      </div>
    </div>
  );
}
