import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import Button from "../../../components/ui/Button";

export default function AdminMessages() {
  const { t } = useTranslation("common");
  const [messages, setMessages] = useState([]);
  const [selectedMessage, setSelectedMessage] = useState(null);
  const [replyText, setReplyText] = useState("");
  const [loading, setLoading] = useState(true);
  const token = localStorage.getItem("token");

  useEffect(() => {
    fetchMessages();
  }, [token]);

  const fetchMessages = async () => {
    try {
      const response = await fetch("/api/contact", {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      if (response.ok) {
        const data = await response.json();
        setMessages(data);
      }
    } catch (error) {
      console.error("Failed to fetch messages:", error);
    } finally {
      setLoading(false);
    }
  };

  const handleReply = async (messageId) => {
    if (!replyText.trim()) {
      alert(t("app.admin.messages.replyPlaceholder"));
      return;
    }

    try {
      const response = await fetch(`/api/contact/${messageId}/reply`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ reply: replyText }),
      });

      if (response.ok) {
        setReplyText("");
        setSelectedMessage(null);
        fetchMessages();
        alert(t("app.admin.messages.replySent"));
      } else {
        alert(t("app.admin.messages.replyFailed"));
      }
    } catch (error) {
      console.error("Failed to send reply:", error);
      alert("Failed to send reply");
    }
  };

  if (loading) return <div className="card">{t("app.admin.overview.loading")}</div>;

  return (
    <div>
      <div className="card">
        <h2>{t("app.admin.messages.title")} ({messages.length})</h2>
        {messages.length > 0 ? (
          <div style={{ marginTop: "16px" }}>
            {messages.map((msg) => (
              <div
                key={msg.id}
                style={{
                  padding: "16px",
                  borderBottom: "1px solid rgba(255,255,255,0.1)",
                  background: msg.replied ? "rgba(0,255,0,0.1)" : "rgba(255,255,0,0.1)",
                  marginBottom: "12px",
                  borderRadius: "8px",
                }}
              >
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start" }}>
                  <div style={{ flex: 1 }}>
                    <div><strong>{msg.name}</strong> - {msg.email}</div>
                    {msg.subject && <div style={{ marginTop: "4px", color: "#aaa" }}>{t("app.admin.messages.subject")}: {msg.subject}</div>}
                    <div style={{ marginTop: "8px" }}>{msg.message}</div>
                    <div style={{ marginTop: "8px", fontSize: "12px", color: "#aaa" }}>
                      {new Date(msg.createdAt).toLocaleString()}
                    </div>
                    {msg.replied && (
                      <div style={{ marginTop: "12px", padding: "12px", background: "rgba(255,255,255,0.1)", borderRadius: "4px" }}>
                        <strong>{t("app.admin.messages.replied")}:</strong> {msg.adminReply}
                        <div style={{ fontSize: "12px", color: "#aaa", marginTop: "4px" }}>
                          {new Date(msg.repliedAt).toLocaleString()}
                        </div>
                      </div>
                    )}
                  </div>
                  {!msg.replied && (
                    <Button
                      size="small"
                      variant="primary"
                      onClick={() => setSelectedMessage(msg)}
                    >
                      {t("app.admin.messages.reply")}
                    </Button>
                  )}
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p>{t("app.admin.messages.noMessages")}</p>
        )}
      </div>

      {selectedMessage && (
        <div className="card" style={{ marginTop: "20px" }}>
          <h3>{t("app.admin.messages.replyTo")} {selectedMessage.name}</h3>
          <textarea
            value={replyText}
            onChange={(e) => setReplyText(e.target.value)}
            placeholder={t("app.admin.messages.replyPlaceholder")}
            rows={6}
            style={{
              width: "100%",
              padding: "12px",
              borderRadius: "8px",
              marginTop: "12px",
              background: "rgba(255,255,255,0.1)",
              border: "1px solid rgba(255,255,255,0.2)",
              color: "white",
            }}
          />
          <div style={{ marginTop: "12px", display: "flex", gap: "8px" }}>
            <Button
              variant="primary"
              onClick={() => handleReply(selectedMessage.id)}
            >
              {t("app.admin.messages.sendReply")}
            </Button>
            <Button onClick={() => {
              setSelectedMessage(null);
              setReplyText("");
            }}>
              {t("app.admin.messages.cancel")}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

