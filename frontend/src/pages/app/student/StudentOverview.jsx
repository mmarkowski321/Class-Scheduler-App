import Button from "../../../components/ui/Button";

export default function StudentOverview({ onQuickBook }) {
  const lessons = []; // TODO: fetch

  return (
    <>
      <div className="card">
        <h3>Najbliższe lekcje</h3>
        {lessons.length ? lessons.map(l => (
          <div key={l.id} className="item">
            <strong>{l.title}</strong> — {new Date(l.start).toLocaleString()} • z {l.tutor}
          </div>
        )) : <div className="empty">Nic w kalendarzu. Zacznij od rezerwacji.</div>}
      </div>

      <div className="card">
        <h3>Szybkie akcje</h3>
        <div className="row">
          <Button className="btn" onClick={onQuickBook}>Zarezerwuj zajęcia</Button>
          <Button className="btn secondary" onClick={() => location.assign('/student')}>Przeglądaj ofertę</Button>
        </div>
      </div>
    </>
  );
}
