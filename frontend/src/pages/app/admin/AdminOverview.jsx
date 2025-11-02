import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";

export default function AdminOverview() {
  const { t } = useTranslation("common");
  const [stats, setStats] = useState(null);
  const token = localStorage.getItem("token");

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const response = await fetch("/api/admin/stats", {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        });
        if (response.ok) {
          const data = await response.json();
          setStats(data);
        }
      } catch (error) {
        console.error("Failed to fetch stats:", error);
      }
    };
    fetchStats();
  }, [token]);

  return (
    <div className="card">
      <h2>{t("app.admin.overview.title")}</h2>
      {stats ? (
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(200px, 1fr))", gap: "16px", marginTop: "20px" }}>
          <div style={{ padding: "20px", background: "rgba(255,255,255,0.1)", borderRadius: "8px" }}>
            <h3>{t("app.admin.overview.totalUsers")}</h3>
            <p style={{ fontSize: "32px", margin: "8px 0" }}>{stats.totalUsers}</p>
          </div>
          <div style={{ padding: "20px", background: "rgba(255,255,255,0.1)", borderRadius: "8px" }}>
            <h3>{t("app.admin.overview.tutors")}</h3>
            <p style={{ fontSize: "32px", margin: "8px 0" }}>{stats.tutorsCount}</p>
          </div>
          <div style={{ padding: "20px", background: "rgba(255,255,255,0.1)", borderRadius: "8px" }}>
            <h3>{t("app.admin.overview.students")}</h3>
            <p style={{ fontSize: "32px", margin: "8px 0" }}>{stats.studentsCount}</p>
          </div>
          <div style={{ padding: "20px", background: "rgba(255,255,255,0.1)", borderRadius: "8px" }}>
            <h3>{t("app.admin.overview.totalLessons")}</h3>
            <p style={{ fontSize: "32px", margin: "8px 0" }}>{stats.totalLessons}</p>
          </div>
          <div style={{ padding: "20px", background: "rgba(255,255,255,0.1)", borderRadius: "8px" }}>
            <h3>{t("app.admin.overview.totalReviews")}</h3>
            <p style={{ fontSize: "32px", margin: "8px 0" }}>{stats.totalReviews}</p>
          </div>
        </div>
      ) : (
        <p>{t("app.admin.overview.loading")}</p>
      )}
    </div>
  );
}

