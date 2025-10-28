import { useState } from "react";
import CalendarPro from "../../../components/ui/CalendarPro";

export default function StudentCalendar() {
  const [items, setItems] = useState([
    // przykładowa lekcja
    { id:"l10", type:"lesson", title:"Angielski – Ania", start:new Date(new Date().setHours(17,0,0,0)).toISOString(), end:new Date(new Date().setHours(18,0,0,0)).toISOString() },
    // wolny czas (edytowalny, background)
    { id:"f1", type:"free", title:"Wolne", start:new Date(new Date().setHours(19,0,0,0)).toISOString(), end:new Date(new Date().setHours(21,0,0,0)).toISOString() },
  ]);

  const save = () => {
    // TODO: API
    console.log("student.calendar.save", items);
    alert("Zapisano (demo).");
  };

  return (
    <div className="card">
      <h3>Kalendarz</h3>
      <p style={{margin:"0 0 8px", opacity:.9}}>
        Dodawaj <b>wolny czas</b> zaznaczeniem myszką. Potwierdzone lekcje są fioletowe.
      </p>
      <CalendarPro role="student" locale="pl" events={items} onChange={setItems} />
      <div style={{display:"flex", justifyContent:"flex-end", marginTop:10}}>
        <button className="btn primary" onClick={save}>Zapisz</button>
      </div>
    </div>
  );
}
