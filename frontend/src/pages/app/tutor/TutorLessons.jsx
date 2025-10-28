import Button from "../../../components/ui/Button";
import { useTranslation } from "react-i18next";

const canModify = (startISO) => {
  const start = new Date(startISO).getTime();
  return (start - Date.now()) / 36e5 >= 24;
};

export default function TutorLessons() {
  const { t, i18n } = useTranslation("common");

  const requests = [
    { id: 11, student: "Ala", title: "Matematyka", start: "2025-10-24T17:00:00Z" },
  ]; // TODO: fetch
  const confirmed = []; // TODO

  const fmt = (iso) =>
    new Date(iso).toLocaleString(i18n.language === "pl" ? "pl-PL" : "en-US");

  return (
    <>
      <div className="card">
        <h3>{t("app.tutor.lessons.requestsTitle")}</h3>

        {requests.map((r) => (
          <div
            key={r.id}
            className="item"
            style={{ padding: "10px 0", borderBottom: "1px solid rgba(255,255,255,.1)" }}
          >
            <div>
              <strong>{r.title}</strong> — {fmt(r.start)} • {r.student}
            </div>
            <div className="row" style={{ marginTop: 8 }}>
              <Button size="small" variant="primary" onClick={() => {/* TODO: accept */}}>
                {t("app.tutor.lessons.actions.confirm")}
              </Button>
              <Button size="small" onClick={() => {/* TODO: decline */}}>
                {t("app.tutor.lessons.actions.decline")}
              </Button>
            </div>
          </div>
        ))}

        {!requests.length && <div className="empty">{t("app.tutor.lessons.noRequests")}</div>}
      </div>

      <div className="card">
        <h3>{t("app.tutor.lessons.confirmedTitle")}</h3>

        {confirmed.length ? (
          confirmed.map((c) => (
            <div
              key={c.id}
              className="item"
              style={{ padding: "10px 0", borderBottom: "1px solid rgba(255,255,255,.1)" }}
            >
              <div>
                <strong>{c.title}</strong> — {fmt(c.start)} • {c.student}
              </div>
              <div className="row" style={{ marginTop: 8, gap: 8 }}>
                <a href={c.joinUrl}>{t("app.tutor.lessons.joinLink")}</a>
                <Button
                  size="small"
                  disabled={!canModify(c.start)}
                  onClick={() => {/* TODO: reschedule */}}
                >
                  {t("app.tutor.lessons.actions.reschedule")}
                </Button>
                <Button size="small" onClick={() => {/* TODO: cancel */}}>
                  {t("app.tutor.lessons.actions.cancel")}
                </Button>
              </div>
            </div>
          ))
        ) : (
          <div className="empty">{t("app.tutor.lessons.empty")}</div>
        )}
      </div>
    </>
  );
}
