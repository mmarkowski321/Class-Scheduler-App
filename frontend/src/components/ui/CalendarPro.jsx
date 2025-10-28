import React, { useMemo, useRef } from "react";
import FullCalendar from "@fullcalendar/react";
import timeGridPlugin from "@fullcalendar/timegrid";
import interactionPlugin from "@fullcalendar/interaction";
import dayGridPlugin from "@fullcalendar/daygrid";
import plLocale from "@fullcalendar/core/locales/pl";
import "./calendar-pro.css";

function CalendarPro({
  role = "tutor",
  events = [],
  onChange,
  locale = "pl",
  weekStart = 1,
  businessHours = { start: "07:00", end: "22:00" },
}) {
  const calRef = useRef(null);

  const fcEvents = useMemo(() => {
    return events.map(ev => {
      if (ev.type === "availability" || ev.type === "free") {
        return {
          ...ev,
          display: "background",
          overlap: true,
          color: role === "tutor" ? "rgba(34,197,94,.35)" : "rgba(59,130,246,.30)",
        };
      }
      return {
        ...ev,
        display: "auto",
        editable: false,
        classNames: ["fc-lesson"],
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
    onChange?.([...events, {
      id: `tmp-${Date.now()}`,
      type: role === "tutor" ? "availability" : "free",
      title: role === "tutor" ? "Dostępność" : "Wolny czas",
      start: sel.startStr,
      end: sel.endStr,
    }]);
  };

  const handleEventClick = (info) => {
    const ev = info.event.extendedProps;
    if (ev?.type === "availability" || ev?.type === "free") {
      if (confirm(locale === "pl" ? "Usunąć ten blok?" : "Remove this block?")) {
        onChange?.(events.filter(e => e.id !== info.event.id));
      }
    }
  };

  return (
    <div className={`calpro calpro--${role}`}>
      <FullCalendar
        ref={calRef}
        plugins={[timeGridPlugin, interactionPlugin, dayGridPlugin]}
        locales={[plLocale]}
        locale={locale}
        initialView="timeGridWeek"
        firstDay={weekStart}
        slotMinTime={businessHours.start}
        slotMaxTime={businessHours.end}
        allDaySlot={false}
        height="auto"
        nowIndicator
        selectable
        selectMirror
        selectOverlap
        select={handleSelect}
        editable
        eventAllow={eventAllow}
        eventDrop={handleEventDrop}
        eventResize={handleEventResize}
        eventClick={handleEventClick}
        events={fcEvents}
        headerToolbar={{
          left: "prev,next today",
          center: "title",
          right: "timeGridWeek,timeGridDay,dayGridMonth",
        }}
        slotDuration="00:30:00"
        expandRows
        scrollTime="09:00:00"
        dayMaxEvents
      />
      <div className="calpro__legend">
        <span className="lg lg--lesson">{locale === "pl" ? "Lekcja" : "Lesson"}</span>
        <span className="lg lg--avail">
          {role === "tutor" ? (locale === "pl" ? "Dostępność" : "Availability")
                            : (locale === "pl" ? "Wolny czas" : "Free time")}
        </span>
      </div>
    </div>
  );
}

export default CalendarPro;
