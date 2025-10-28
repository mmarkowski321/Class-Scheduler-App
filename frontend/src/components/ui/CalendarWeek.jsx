import { useEffect, useMemo, useRef, useState } from "react";
import "./calendar-week.css";

/**
 * Reusable weekly calendar with selection + simple editor.
 *
 * Props:
 *  - mode: "tutor" | "student" (only affects colors/labels)
 *  - value: array of blocks [{id, type:"availability"|"lesson", day:0..6, start:"HH:MM", end:"HH:MM", title?:string}]
 *  - onChange(next) => void
 *  - startHour?: number (default 7)
 *  - endHour?: number (default 22)
 */
export default function CalendarWeek({
  mode = "tutor",
  value = [],
  onChange,
  startHour = 7,
  endHour = 22,
}) {
  const hours = useMemo(
    () => Array.from({ length: (endHour - startHour) + 1 }, (_, i) => startHour + i),
    [startHour, endHour]
  );
  const days = useMemo(() => (["Mon","Tue","Wed","Thu","Fri","Sat","Sun"]), []);

  // selection state (drag add)
  const wrapRef = useRef(null);
  const [sel, setSel] = useState(null); // {day, startMin, endMin}
  const [editing, setEditing] = useState(null); // {block?, draftBlock}
  const [blocks, setBlocks] = useState(value);

  useEffect(() => setBlocks(value), [value]);

  const emit = (next) => {
    setBlocks(next);
    onChange?.(next);
  };

  // helpers
  const minPerCell = 30;
  const toMin = (h, m = 0) => h * 60 + m;
  const fromMin = (min) => {
    const h = Math.floor(min / 60);
    const m = min % 60;
    return `${String(h).padStart(2,"0")}:${String(m).padStart(2,"0")}`;
  };
  const clamp = (v, a, b) => Math.max(a, Math.min(b, v));
  const dayColCount = 7;
  const hoursHeight = (endHour - startHour) * (60 / minPerCell) * 28; // CSS unit base

  // grid math
  const pickCell = (clientX, clientY) => {
    const el = wrapRef.current;
    if (!el) return null;
    const rect = el.getBoundingClientRect();
    const x = clamp(clientX - rect.left, 0, rect.width - 1);
    const y = clamp(clientY - rect.top, 0, rect.height - 1);

    const colW = rect.width / dayColCount;
    const rowH = 28; // must match CSS --rowH
    const day = Math.floor(x / colW);
    const row = Math.floor(y / rowH);
    const minutesFromStart = row * minPerCell + startHour * 60;
    return { day, minutes: clamp(minutesFromStart, startHour*60, endHour*60) };
  };

  const onMouseDown = (e) => {
    if (e.button !== 0) return;
    const pick = pickCell(e.clientX, e.clientY);
    if (!pick) return;
    setSel({ day: pick.day, startMin: pick.minutes, endMin: pick.minutes + minPerCell });
  };
  const onMouseMove = (e) => {
    if (!sel) return;
    const pick = pickCell(e.clientX, e.clientY);
    if (!pick || pick.day !== sel.day) return;
    const end = Math.max(pick.minutes + minPerCell, sel.startMin + minPerCell);
    setSel({ ...sel, endMin: clamp(end, startHour*60+minPerCell, endHour*60) });
  };
  const finishSelection = () => {
    if (!sel) return;
    const draft = {
      id: `tmp-${Date.now()}`,
      type: "availability",
      day: sel.day,
      start: fromMin(sel.startMin),
      end: fromMin(sel.endMin),
      title: mode === "tutor" ? "Okienko" : "Wolny czas",
    };
    setEditing({ block: null, draftBlock: draft });
    setSel(null);
  };

  // click on block -> edit
  const onBlockClick = (b) => {
    setEditing({ block: b, draftBlock: { ...b } });
  };

  // drag handlers for simple resize (top/bottom handles)
  const dragInfo = useRef(null);
  const onHandleDown = (b, pos, e) => {
    e.stopPropagation();
    dragInfo.current = { id: b.id, pos }; // "start" | "end"
    document.body.style.userSelect = "none";
  };
  const onGlobalMove = (e) => {
    if (!dragInfo.current) return;
    const pick = pickCell(e.clientX, e.clientY);
    if (!pick) return;
    setBlocks(prev => prev.map(b => {
      if (b.id !== dragInfo.current.id) return b;
      if (pick.day !== b.day) return b;
      const curStart = toMin(...b.start.split(":").map(Number));
      const curEnd = toMin(...b.end.split(":").map(Number));
      if (dragInfo.current.pos === "start") {
        const nextStart = clamp(pick.minutes, startHour*60, curEnd - minPerCell);
        return { ...b, start: fromMin(nextStart) };
      } else {
        const nextEnd = clamp(pick.minutes + minPerCell, curStart + minPerCell, endHour*60);
        return { ...b, end: fromMin(nextEnd) };
      }
    }));
  };
  const onGlobalUp = () => {
    if (dragInfo.current) {
      onChange?.(blocks);
      dragInfo.current = null;
      document.body.style.userSelect = "";
    }
  };
  useEffect(() => {
    window.addEventListener("mousemove", onGlobalMove);
    window.addEventListener("mouseup", onGlobalUp);
    return () => {
      window.removeEventListener("mousemove", onGlobalMove);
      window.removeEventListener("mouseup", onGlobalUp);
    };
  });

  // save / remove in editor
  const saveDraft = () => {
    if (!editing) return;
    const d = editing.draftBlock;
    // normalize
    const [sh, sm] = d.start.split(":").map(Number);
    const [eh, em] = d.end.split(":").map(Number);
    const s = toMin(sh, sm), e = toMin(eh, em);
    if (e <= s) return; // simple guard

    if (editing.block) {
      emit(blocks.map(b => b.id === editing.block.id ? { ...d, id: editing.block.id } : b));
    } else {
      emit([...blocks, { ...d, id: `${Date.now()}` }]);
    }
    setEditing(null);
  };
  const removeDraft = () => {
    if (!editing) return setEditing(null);
    if (!editing.block) return setEditing(null);
    emit(blocks.filter(b => b.id !== editing.block.id));
    setEditing(null);
  };

  // render helpers
  const colorFor = (b) => {
    if (b.type === "lesson") return "var(--cal-lesson)";
    // availability:
    return mode === "tutor" ? "var(--cal-available)" : "var(--cal-student)";
  };
  const topFor = (time) => {
    const [h, m] = time.split(":").map(Number);
    const total = toMin(h, m) - startHour * 60;
    return (total / minPerCell) * 28; // px
  };
  const heightFor = (b) => {
    const start = toMin(...b.start.split(":").map(Number));
    const end = toMin(...b.end.split(":").map(Number));
    return ((end - start) / minPerCell) * 28;
  };

  return (
    <div className="cw">
      <div className="cw__head">
        <div className="cw__corner" />
        {days.map((d,i)=><div key={i} className="cw__day">{d}</div>)}
      </div>

      <div
        className="cw__grid"
        ref={wrapRef}
        onMouseDown={onMouseDown}
        onMouseMove={onMouseMove}
        onMouseLeave={finishSelection}
        onMouseUp={finishSelection}
        role="grid"
        aria-label="week-calendar"
      >
        {/* hour labels */}
        <div className="cw__hours" style={{ height: hoursHeight }}>
          {hours.map((h,i)=>(
            <div key={i} className="cw__hour">
              <span>{String(h).padStart(2,"0")}:00</span>
            </div>
          ))}
        </div>

        {/* day columns */}
        <div className="cw__cols" style={{ height: hoursHeight }}>
          {Array.from({length: dayColCount}).map((_,day)=>(
            <div key={day} className="cw__col">
              {/* blocks for this day */}
              {blocks.filter(b=>b.day===day).map(b=>(
                <div
                  key={b.id}
                  className="cw__block"
                  style={{
                    top: topFor(b.start),
                    height: heightFor(b),
                    background: colorFor(b),
                  }}
                  onClick={(e)=>{ e.stopPropagation(); onBlockClick(b); }}
                >
                  <div className="cw__block-title">{b.title || (b.type==="lesson"?"Lesson":"Availability")}</div>
                  <div className="cw__block-time">{b.start}–{b.end}</div>
                  <div className="cw__handle cw__handle--top" onMouseDown={(e)=>onHandleDown(b,"start",e)}/>
                  <div className="cw__handle cw__handle--bot" onMouseDown={(e)=>onHandleDown(b,"end",e)}/>
                </div>
              ))}

              {/* selection preview */}
              {sel && sel.day===day && (
                <div
                  className="cw__selection"
                  style={{ top: topFor(fromMin(sel.startMin)), height: ((sel.endMin - sel.startMin)/minPerCell)*28 }}
                />
              )}
            </div>
          ))}
        </div>
      </div>

      {/* inline editor */}
      {editing && (
        <div className="cw__editor">
          <div className="cw__editor-card">
            <div className="cw__editor-title">
              {editing.block ? "Edytuj blok" : "Dodaj blok"}
            </div>
            <div className="cw__editor-row">
              <label>
                Rodzaj
                <select
                  value={editing.draftBlock.type}
                  onChange={(e)=>setEditing(s=>({ ...s, draftBlock:{ ...s.draftBlock, type:e.target.value }}))}
                >
                  <option value="availability">{mode==="tutor" ? "Dostępność" : "Wolny czas"}</option>
                  <option value="lesson">{mode==="tutor" ? "Lekcja" : "Zajęcia"}</option>
                </select>
              </label>
              <label>
                Dzień
                <select
                  value={editing.draftBlock.day}
                  onChange={(e)=>setEditing(s=>({ ...s, draftBlock:{ ...s.draftBlock, day:+e.target.value }}))}
                >
                  {days.map((d,i)=><option key={i} value={i}>{d}</option>)}
                </select>
              </label>
              <label>
                Od
                <input
                  type="time"
                  value={editing.draftBlock.start}
                  onChange={(e)=>setEditing(s=>({ ...s, draftBlock:{ ...s.draftBlock, start:e.target.value }}))}
                  step={60*minPerCell}
                />
              </label>
              <label>
                Do
                <input
                  type="time"
                  value={editing.draftBlock.end}
                  onChange={(e)=>setEditing(s=>({ ...s, draftBlock:{ ...s.draftBlock, end:e.target.value }}))}
                  step={60*minPerCell}
                />
              </label>
            </div>
            <label className="cw__editor-full">
              Tytuł/opis
              <input
                value={editing.draftBlock.title ?? ""}
                onChange={(e)=>setEditing(s=>({ ...s, draftBlock:{ ...s.draftBlock, title:e.target.value }}))}
                placeholder={mode==="tutor" ? "np. Matematyka – okienko / lekcja" : "np. wolne popołudnie"}
              />
            </label>

            <div className="cw__editor-actions">
              <button className="btn ghost" onClick={()=>setEditing(null)}>Anuluj</button>
              {editing.block && <button className="btn danger" onClick={removeDraft}>Usuń</button>}
              <button className="btn primary" onClick={saveDraft}>Zapisz</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
