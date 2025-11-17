export default function Upcoming({ title, items=[] }) {
    return (
      <div className="card">
        <h3>{title}</h3>
        {items.length ? items.map(x => (
          <div key={x.id} className="item" style={{padding:"10px 0", borderBottom:"1px solid rgba(255,255,255,.1)"}}>
            <div><strong>{x.title}</strong> — {new Date(x.date || x.start).toLocaleString()}</div>
            <div style={{fontSize:12, opacity:.85}}>{x.with || x.student || x.tutor}</div>
          </div>
        )) : <div className="empty">Nic w najbliższym czasie.</div>}
      </div>
    );
  }
  