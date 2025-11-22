import React, { useMemo, useRef, useState } from "react";
import FullCalendar from "@fullcalendar/react";
import timeGridPlugin from "@fullcalendar/timegrid";
import interactionPlugin from "@fullcalendar/interaction";
import dayGridPlugin from "@fullcalendar/daygrid";
import plLocale from "@fullcalendar/core/locales/pl";
import enLocale from "@fullcalendar/core/locales/en-gb";
import EventModal from "./EventModal";
import "./calendar-pro.css";

function CalendarPro({
  role = "tutor",
  events = [],
  onChange,
  locale = "pl",
  weekStart = 1,
  businessHours = { start: "00:00", end: "24:00" },
  readOnly = false,
}) {
  const calRef = useRef(null);
  const [selectedEvent, setSelectedEvent] = useState(null);

  const fcEvents = useMemo(() => {
    const now = Date.now();
    return events.map((ev) => {
      const statusLower = (ev.status || "").toLowerCase();
      if (ev.type === "availability" || ev.type === "free") {
        return {
          ...ev,
          display: "background",
          overlap: true,
          color: role === "tutor" ? "rgba(34,197,94,.35)" : "rgba(59,130,246,.30)",
        };
      }
      if (ev.type === "busy") {
        return {
          id: ev.id,
          title: ev.title || (locale === "pl" ? "Zajęte (Google Calendar)" : "Busy (Google Calendar)"),
          start: ev.start,
          end: ev.end,
          display: "auto", // Changed from "background" to show title
          overlap: false,
          color: "#ef4444", // Red color
          classNames: ["fc-busy"],
          extendedProps: {
            type: ev.type,
            description: ev.description,
          },
        };
      }
      const isCurrent =
        ev.type === "lesson" &&
        statusLower === "scheduled" &&
        ev.start &&
        ev.end &&
        new Date(ev.start).getTime() <= now &&
        now < new Date(ev.end).getTime();

      return {
        id: ev.id,
        title:
          ev.title ||
          (locale === "pl"
            ? (isCurrent ? "Lekcja (w trakcie)" : "Lekcja")
            : (isCurrent ? "Lesson (in progress)" : "Lesson")),
        start: ev.start,
        end: ev.end,
        display: "auto",
        editable: false,
        color: isCurrent ? "#22c55e" : "#9333ea", // highlight running lesson
        classNames: ["fc-lesson"],
        extendedProps: {
          type: ev.type,
          status: isCurrent ? "in_progress" : statusLower || ev.status,
          meetingLink: ev.meetingLink,
          notes: ev.notes,
          studentId: ev.studentId,
            tutorId: ev.tutorId,
            deliveryMode: ev.deliveryMode,
            onsiteCity: ev.onsiteCity,
            onsitePostalCode: ev.onsitePostalCode,
            onsiteStreet: ev.onsiteStreet,
            onsiteBuilding: ev.onsiteBuilding,
            onsiteApartment: ev.onsiteApartment,
        },
      };
    });
  }, [events, role]);

  const eventAllow = (_dropInfo, draggedEvent) => {
    const ev = draggedEvent.extendedProps;
    return ev && (ev.type === "availability" || ev.type === "free");
  };

  const handleEventDrop = (arg) => {
    const id = arg.event.id;
    onChange?.(events.map(e => e.id === id ? ({
      ...e,
      start: arg.event.start.toISOString(),
      end: (arg.event.end || arg.event.start).toISOString()
    }) : e));
  };
  const handleEventResize = handleEventDrop;

  const handleSelect = (sel) => {
    if (readOnly) {
      return;
    }
    // Check if selected time overlaps with any busy times
    const selectedStart = new Date(sel.startStr).getTime();
    const selectedEnd = new Date(sel.endStr).getTime();
    
    const overlapsBusy = events.some(ev => {
      if (ev.type === "busy") {
        const busyStart = new Date(ev.start).getTime();
        const busyEnd = new Date(ev.end).getTime();
        return selectedStart < busyEnd && selectedEnd > busyStart;
      }
      return false;
    });
    
    if (overlapsBusy) {
      // Note: This alert is hardcoded since CalendarPro doesn't have access to i18n
      // The translation key is handled in the parent component
      alert(locale === "pl" 
        ? "Ten termin jest zajęty w Twoim kalendarzu Google. Wybierz inny termin." 
        : "This time slot is busy in your Google Calendar. Please select another time.");
      return;
    }
    
    onChange?.([...events, {
      id: `tmp-${Date.now()}`,
      type: role === "tutor" ? "availability" : "free",
      title: role === "tutor" 
        ? (locale === "pl" ? "Dostępność" : "Availability")
        : (locale === "pl" ? "Wolny czas" : "Free time"),
      start: sel.startStr,
      end: sel.endStr,
    }]);
  };

  const handleEventClick = (info) => {
    if (readOnly) {
      return;
    }
    const ev = info.event.extendedProps;
    if (ev?.type === "availability" || ev?.type === "free") {
      // Note: This confirm is hardcoded since CalendarPro doesn't have access to i18n
      // The translation key is handled in the parent component
      if (confirm(locale === "pl" ? "Usunąć ten blok?" : "Remove this block?")) {
        onChange?.(events.filter(e => e.id !== info.event.id));
      }
    } else if (ev?.type === "lesson" || ev?.type === "busy") {
      // Show modal with event details
      const eventData = {
        type: ev.type,
        title: info.event.title || "",
        start: info.event.start ? info.event.start.toISOString() : "",
        end: info.event.end ? info.event.end.toISOString() : "",
        status: ev.status,
        meetingLink: ev.meetingLink,
        notes: ev.notes,
        description: ev.description,
        deliveryMode: ev.deliveryMode,
        onsiteCity: ev.onsiteCity,
        onsitePostalCode: ev.onsitePostalCode,
        onsiteStreet: ev.onsiteStreet,
        onsiteBuilding: ev.onsiteBuilding,
        onsiteApartment: ev.onsiteApartment,
      };
      setSelectedEvent(eventData);
    }
  };

  const calendarLocale = useMemo(() => {
    return locale === "pl" ? plLocale : enLocale;
  }, [locale]);

  return (
    <div className={`calpro calpro--${role}`}>
      <FullCalendar
        ref={calRef}
        plugins={[timeGridPlugin, interactionPlugin, dayGridPlugin]}
        locales={[plLocale, enLocale]}
        locale={locale}
        initialView="timeGridWeek"
        firstDay={weekStart}
        slotMinTime={businessHours.start}
        slotMaxTime={businessHours.end}
        allDaySlot={false}
        height="auto"
        nowIndicator
        selectable={!readOnly}
        selectMirror={!readOnly}
        selectOverlap={!readOnly}
        select={readOnly ? undefined : handleSelect}
        editable={!readOnly}
        eventAllow={readOnly ? undefined : eventAllow}
        eventDrop={readOnly ? undefined : handleEventDrop}
        eventResize={readOnly ? undefined : handleEventResize}
        eventClick={readOnly ? undefined : handleEventClick}
        events={fcEvents}
        headerToolbar={{
          left: "prev,next today",
          center: "title",
          right: "timeGridWeek,timeGridDay,dayGridMonth",
        }}
        slotDuration="00:30:00"
        expandRows
        scrollTime="00:00:00"
        dayMaxEvents
      />
      <div className="calpro__legend">
        <span className="lg lg--lesson">{locale === "pl" ? "Lekcja" : "Lesson"}</span>
        <span className="lg lg--avail">
          {role === "tutor" ? (locale === "pl" ? "Dostępność" : "Availability")
                            : (locale === "pl" ? "Wolny czas" : "Free time")}
        </span>
        {events.some(ev => ev.type === "busy") && (
          <span className="lg" style={{color: "#ef4444"}}>
            {locale === "pl" ? "Zajęte (Google Calendar)" : "Busy (Google Calendar)"}
          </span>
        )}
      </div>
      
      {selectedEvent && (
        <EventModal 
          event={selectedEvent} 
          onClose={() => setSelectedEvent(null)} 
        />
      )}
    </div>
  );
}

export default CalendarPro;
