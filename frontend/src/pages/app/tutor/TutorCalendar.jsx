import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import CalendarPro from "../../../components/ui/CalendarPro";

export default function TutorCalendar() {
  const { t, i18n } = useTranslation("common");
  const token = localStorage.getItem("token");
  const userId = localStorage.getItem("userId");
  
  const [items, setItems] = useState([]); // Availability slots (editable)
  
  const [busyTimes, setBusyTimes] = useState([]);
  const [lessons, setLessons] = useState([]);
  const [loading, setLoading] = useState(false);

  // Load user's own lessons and Google Calendar busy times
  useEffect(() => {
    const loadCalendarData = async () => {
      if (!token || !userId) return;
      
      setLoading(true);
      try {
        // Load user's own lessons with details
        const lessonsResponse = await fetch(`/api/calendar/lessons/${userId}`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        
        if (lessonsResponse.ok) {
          const lessonsData = await lessonsResponse.json();
          if (lessonsData.lessons && lessonsData.lessons.length > 0) {
            // Convert lessons to calendar events
            const lessonEvents = lessonsData.lessons.map(lesson => ({
              id: `lesson-${lesson.id}`,
              type: "lesson",
              title: lesson.title,
              start: lesson.start,
              end: lesson.end,
              status: lesson.status,
              meetingLink: lesson.meetingLink,
              notes: lesson.notes,
            }));
            setLessons(lessonEvents);
          }
        }
        
        // Load Google Calendar busy times
        const busyResponse = await fetch(`/api/calendar/sync/${userId}`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        
        if (busyResponse.ok) {
          const busyData = await busyResponse.json();
          console.log("Loaded busy times from backend:", busyData);
          if (busyData.busyTimes && busyData.busyTimes.length > 0) {
            // Convert busy times to calendar events with actual titles from Google Calendar
            const busyEvents = busyData.busyTimes.map((bt, idx) => ({
              id: `busy-${idx}`,
              type: "busy",
              title: bt.title || (i18n.language === "pl" ? "Zajęte (Google Calendar)" : "Busy (Google Calendar)"),
              start: bt.start,
              end: bt.end,
              description: bt.description,
            }));
            console.log("Converted busy events:", busyEvents);
            setBusyTimes(busyEvents);
          } else {
            console.warn("No busy times found. Response:", busyData);
            if (busyData.warning) {
              console.warn("Warning from backend:", busyData.warning);
            }
          }
        } else {
          const errorData = await busyResponse.json();
          console.error("Failed to load busy times:", errorData);
        }
      } catch (error) {
        console.error("Failed to load calendar data:", error);
      } finally {
        setLoading(false);
      }
    };
    
    loadCalendarData();
  }, [token, userId]);

  // Combine items with lessons and busy times
  const allEvents = [...items, ...lessons, ...busyTimes];

  const save = () => {
    // TODO: API - save only non-busy items
    const itemsToSave = items.filter(item => item.type !== "busy");
    console.log("tutor.calendar.save", itemsToSave);
    alert(t("app.tutor.calendar.saved"));
  };

  return (
    <div className="card">
      <h3>{t("app.tutor.calendar.title")}</h3>
      <p style={{margin:"0 0 8px", opacity:.9}} dangerouslySetInnerHTML={{__html: t("app.tutor.calendar.descriptionTutor")}}>
      </p>
      {busyTimes.length > 0 && (
        <p style={{display:"block", margin:"4px 0 8px", color:"#ef4444", opacity:.9}}>
          {t("app.tutor.calendar.busyTimesNote")}
        </p>
      )}
      {loading && <p style={{margin:"0 0 8px", opacity:.7}}>{t("app.tutor.calendar.loading")}</p>}
      <CalendarPro role="tutor" locale={i18n.language} events={allEvents} onChange={setItems} />
      <div style={{display:"flex", justifyContent:"flex-end", marginTop:10}}>
        <button className="btn primary" onClick={save}>{t("app.tutor.calendar.save")}</button>
      </div>
    </div>
  );
}
