import { useState } from "react";
import CalendarPro from "../../../components/ui/CalendarPro";

export default function TutorCalendar() {
  const [items, setItems] = useState([
    // przykładowe lekcje (blokowane do drag/res)
    { id:"l1", type:"lesson", title:"Matematyka – Jan", start:new Date().toISOString(), end:new Date(Date.now()+60*60*1000).toISOString() },
    // dostępność (edytowalna)
    { id:"a1", type:"availability", title:"Okienko", start:new Date(new Date().setHours(16,0,0,0)).toISOString(), end:new Date(new Date().setHours(18,0,0,0)).toISOString() },
  ]);

  const save = () => {
    // TODO: API
    console.log("tutor.calendar.save", items);
    alert("Zapisano (demo).");
  };

  return (
    <div className="card">
      <h3>Kalendarz</h3>
      <p style={{margin:"0 0 8px", opacity:.9}}>
        Zaznacz obszar, aby dodać <b>dostępność</b>. Lekcje są widoczne jako fioletowe bloki.
      </p>
      <CalendarPro role="tutor" locale="pl" events={items} onChange={setItems} />
      <div style={{display:"flex", justifyContent:"flex-end", marginTop:10}}>
        <button className="btn primary" onClick={save}>Zapisz</button>
      </div>
    </div>
  );
}
