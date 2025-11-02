import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import Button from "../../../components/ui/Button";

export default function AdminUsers() {
  const { t } = useTranslation("common");
  const [users, setUsers] = useState({ tutors: [], students: [] });
  const [loading, setLoading] = useState(true);
  const [selectedUser, setSelectedUser] = useState(null);
  const [userStats, setUserStats] = useState(null);
  const [userLessons, setUserLessons] = useState(null);
  const [userReviews, setUserReviews] = useState(null);
  const token = localStorage.getItem("token");

  useEffect(() => {
    fetchUsers();
  }, [token]);

  const fetchUsers = async () => {
    try {
      const response = await fetch("/api/admin/users", {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      if (response.ok) {
        const data = await response.json();
        setUsers(data);
      }
    } catch (error) {
      console.error("Failed to fetch users:", error);
    } finally {
      setLoading(false);
    }
  };

  const fetchUserDetails = async (userId) => {
    try {
      // Fetch user details, stats, lessons, and reviews
      const [userRes, statsRes, lessonsRes, reviewsRes] = await Promise.all([
        fetch(`/api/admin/users/${userId}`, {
          headers: { Authorization: `Bearer ${token}` },
        }),
        fetch(`/api/admin/users/${userId}/stats`, {
          headers: { Authorization: `Bearer ${token}` },
        }),
        fetch(`/api/admin/users/${userId}/lessons`, {
          headers: { Authorization: `Bearer ${token}` },
        }),
        fetch(`/api/admin/reviews/user/${userId}`, {
          headers: { Authorization: `Bearer ${token}` },
        }),
      ]);

      if (userRes.ok && statsRes.ok && lessonsRes.ok && reviewsRes.ok) {
        const user = await userRes.json();
        const stats = await statsRes.json();
        const lessons = await lessonsRes.json();
        const reviews = await reviewsRes.json();

        setSelectedUser(user);
        setUserStats(stats);
        setUserLessons(lessons);
        setUserReviews(reviews);
      }
    } catch (error) {
      console.error("Failed to fetch user details:", error);
    }
  };

  const handleBan = async (userId) => {
    if (!confirm(t("app.admin.users.banConfirm"))) return;

    try {
      const response = await fetch(`/api/admin/users/${userId}`, {
        method: "DELETE",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      if (response.ok) {
        fetchUsers();
        setSelectedUser(null);
        alert("User banned successfully");
      } else {
        const error = await response.json();
        alert(error.error || "Failed to ban user");
      }
    } catch (error) {
      console.error("Failed to ban user:", error);
      alert("Failed to ban user");
    }
  };

  if (loading) return <div className="card">{t("app.admin.overview.loading")}</div>;

  return (
    <div>
      <div className="card">
        <h2>{t("app.admin.users.tutorsTitle")} ({users.tutors?.length || 0})</h2>
        {users.tutors && users.tutors.length > 0 ? (
          <div style={{ marginTop: "16px" }}>
            {users.tutors.map((user) => (
              <div
                key={user.id}
                style={{
                  padding: "12px",
                  borderBottom: "1px solid rgba(255,255,255,0.1)",
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                }}
              >
                <div style={{ flex: 1 }}>
                  <strong>{user.firstName} {user.lastName}</strong> - {user.email}
                </div>
                <div style={{ display: "flex", gap: "8px" }}>
                  <Button
                    size="small"
                    variant="primary"
                    onClick={() => fetchUserDetails(user.id)}
                  >
                    {t("app.admin.users.userDetails")}
                  </Button>
                  <Button
                    size="small"
                    variant="primary"
                    onClick={() => handleBan(user.id)}
                    style={{ background: "#ef4444" }}
                  >
                    {t("app.admin.users.banUser")}
                  </Button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p>{t("app.admin.users.noTutors")}</p>
        )}
      </div>

      <div className="card" style={{ marginTop: "20px" }}>
        <h2>{t("app.admin.users.studentsTitle")} ({users.students?.length || 0})</h2>
        {users.students && users.students.length > 0 ? (
          <div style={{ marginTop: "16px" }}>
            {users.students.map((user) => (
              <div
                key={user.id}
                style={{
                  padding: "12px",
                  borderBottom: "1px solid rgba(255,255,255,0.1)",
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                }}
              >
                <div style={{ flex: 1 }}>
                  <strong>{user.firstName} {user.lastName}</strong> - {user.email}
                </div>
                <div style={{ display: "flex", gap: "8px" }}>
                  <Button
                    size="small"
                    variant="primary"
                    onClick={() => fetchUserDetails(user.id)}
                  >
                    {t("app.admin.users.userDetails")}
                  </Button>
                  <Button
                    size="small"
                    variant="primary"
                    onClick={() => handleBan(user.id)}
                    style={{ background: "#ef4444" }}
                  >
                    {t("app.admin.users.banUser")}
                  </Button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p>{t("app.admin.users.noStudents")}</p>
        )}
      </div>

      {selectedUser && (
        <div className="card" style={{ marginTop: "20px", padding: "24px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "start", marginBottom: "20px" }}>
            <h2>{t("app.admin.users.userDetails")}</h2>
            <Button onClick={() => {
              setSelectedUser(null);
              setUserStats(null);
              setUserLessons(null);
              setUserReviews(null);
            }}>
              {t("app.admin.users.close")}
            </Button>
          </div>

          <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "16px", marginBottom: "20px" }}>
            <div>
              <strong>{t("app.admin.users.firstName")}:</strong> {selectedUser.firstName}
            </div>
            <div>
              <strong>{t("app.admin.users.lastName")}:</strong> {selectedUser.lastName}
            </div>
            <div>
              <strong>{t("app.admin.users.email")}:</strong> {selectedUser.email}
            </div>
            <div>
              <strong>{t("app.admin.users.emailVerified")}:</strong> {selectedUser.emailVerified ? "✅" : "❌"}
            </div>
          </div>

          {userStats && (
            <div style={{ marginBottom: "20px", padding: "16px", background: "rgba(255,255,255,0.1)", borderRadius: "8px" }}>
              <h3>{t("app.admin.users.lessonsCount")}: {userStats.lessonsCount}</h3>
              <h3>{t("app.admin.users.reviewsCount")}: {userStats.reviewsCount}</h3>
            </div>
          )}

          {userLessons && userLessons.length > 0 && (
            <div style={{ marginBottom: "20px" }}>
              <h3>{t("app.admin.users.viewLessons")} ({userLessons.length})</h3>
              <div style={{ maxHeight: "300px", overflowY: "auto", marginTop: "12px" }}>
                {userLessons.map((lesson) => (
                  <div
                    key={lesson.id}
                    style={{
                      padding: "12px",
                      marginBottom: "8px",
                      background: "rgba(255,255,255,0.05)",
                      borderRadius: "4px",
                    }}
                  >
                    <div><strong>Status:</strong> {lesson.status}</div>
                    <div><strong>Start:</strong> {new Date(lesson.startTime).toLocaleString()}</div>
                    <div><strong>End:</strong> {new Date(lesson.endTime).toLocaleString()}</div>
                    {lesson.meetingLink && (
                      <div><strong>Meeting Link:</strong> <a href={lesson.meetingLink} target="_blank" rel="noopener noreferrer">{lesson.meetingLink}</a></div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {userReviews && userReviews.length > 0 && (
            <div>
              <h3>{t("app.admin.users.viewReviews")} ({userReviews.length})</h3>
              <div style={{ maxHeight: "300px", overflowY: "auto", marginTop: "12px" }}>
                {userReviews.map((review) => (
                  <div
                    key={review.id}
                    style={{
                      padding: "12px",
                      marginBottom: "8px",
                      background: "rgba(255,255,255,0.05)",
                      borderRadius: "4px",
                    }}
                  >
                    <div><strong>{t("app.admin.reviews.tutorRating")}:</strong> {"⭐".repeat(review.tutorRating)} ({review.tutorRating}/5)</div>
                    <div><strong>{t("app.admin.reviews.platformRating")}:</strong> {"⭐".repeat(review.platformRating)} ({review.platformRating}/5)</div>
                    {review.studentBehaviorRating && (
                      <div><strong>{t("app.admin.reviews.studentBehaviorRating")}:</strong> {"⭐".repeat(review.studentBehaviorRating)} ({review.studentBehaviorRating}/5)</div>
                    )}
                    {review.comment && (
                      <div style={{ marginTop: "8px" }}><strong>{t("app.admin.reviews.comment")}:</strong> {review.comment}</div>
                    )}
                    <div style={{ fontSize: "12px", color: "#aaa", marginTop: "4px" }}>
                      {new Date(review.createdAt).toLocaleString()}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          <div style={{ marginTop: "20px" }}>
            <Button
              variant="primary"
              onClick={() => handleBan(selectedUser.id)}
              style={{ background: "#ef4444" }}
            >
              {t("app.admin.users.banUser")} {selectedUser.firstName} {selectedUser.lastName}
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}
