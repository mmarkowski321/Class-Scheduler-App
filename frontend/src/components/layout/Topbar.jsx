import { useNavigate } from "react-router-dom";
import "./Topbar.css";
import { clearAuthSession } from "../../utils/auth";

export default function Topbar({ user = { name: "Użytkownik", role: "student", email: "" } }) {
  const navigate = useNavigate();

  const logout = () => {
    clearAuthSession();
    navigate("/", { replace: true });
  };

  return (
    <header className="topbar">
      <div className="topbar-left">
        <div className="brand-dot" />
        <span className="brand">EduScheduler</span>
      </div>

      <div className="topbar-right">
        <span className="role-badge">{user.role === "tutor" ? "Korepetytor" : "Uczeń"}</span>

        <div className="user-chip">
          <div className="avatar">{(user.name || "U")[0].toUpperCase()}</div>
          <div className="user-meta">
            <strong>{user.name}</strong>
            <small>{user.email || "online"}</small>
          </div>

          <div className="user-menu">
            <button onClick={() => navigate("settings")}>Ustawienia / Zmień hasło</button>
            <button onClick={() => navigate("profile")}>Mój profil</button>
            <hr />
            <button className="danger" onClick={logout}>Wyloguj</button>
          </div>
        </div>
      </div>
    </header>
  );
}
