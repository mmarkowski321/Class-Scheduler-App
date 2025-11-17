import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import Button from "../../../components/ui/Button";
import Alert from "../../../components/ui/Alert";
import "./student-settings.css";

export default function StudentSettings() {
  const { t } = useTranslation("common");
  const token = localStorage.getItem("token");
  const userId = localStorage.getItem("userId");
  
  // Account Information
  const [currentEmail, setCurrentEmail] = useState("");
  const [newEmail, setNewEmail] = useState("");
  const [confirmEmail, setConfirmEmail] = useState("");
  
  // Security
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  
  // Phone Numbers
  const [primaryPhone, setPrimaryPhone] = useState("");
  const [additionalPhones, setAdditionalPhones] = useState([]);
  const [newPhoneLabel, setNewPhoneLabel] = useState("");
  const [newPhoneNumber, setNewPhoneNumber] = useState("");
  
  // Load user data on mount
  useEffect(() => {
    const loadUserData = async () => {
      if (!token || !userId) return;
      try {
        const res = await fetch(`/api/profile/${userId}`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        if (res.ok) {
          const user = await res.json();
          setCurrentEmail(user.email || "");
          setPrimaryPhone(user.phone || "");
          setEmailLanguage(user.emailLanguage || "pl");
        }
      } catch (error) {
        console.error("Failed to load user data:", error);
      }
      try {
        const res2 = await fetch(`/api/settings/notifications/${userId}`, {
          headers: { Authorization: `Bearer ${token}` }
        });
        if (res2.ok) {
          const p = await res2.json();
          setNotifications({
            email: !!p.emailNotifications,
            lessonReminders: !!p.lessonReminders,
            changeNotifications: !!p.changeNotifications
          });
        }
      } catch {}
    };
    loadUserData();
  }, [token, userId]);
  
  // Notifications
  const [notifications, setNotifications] = useState({
    email: true,
    lessonReminders: true,
    changeNotifications: true
  });

  // Email language preference
  const [emailLanguage, setEmailLanguage] = useState("pl");
  
  const [errors, setErrors] = useState({});
  const [successMessage, setSuccessMessage] = useState("");

  const validateEmail = (email) => {
    return /\S+@\S+\.\S+/.test(email);
  };

  const validatePhone = (phone) => {
    return /^\+?[\d\s\-\(\)]+$/.test(phone) && phone.length >= 9;
  };

  const handleChangeEmail = async () => {
    const newErrors = {};
    
    if (!newEmail.trim()) {
      newErrors.newEmail = t("app.student.settings.errors.emailRequired");
    } else if (!validateEmail(newEmail)) {
      newErrors.newEmail = t("app.student.settings.errors.emailInvalid");
    }
    
    if (!confirmEmail.trim()) {
      newErrors.confirmEmail = t("app.student.settings.errors.emailRequired");
    } else if (newEmail !== confirmEmail) {
      newErrors.confirmEmail = t("app.student.settings.errors.emailsMismatch");
    }
    
    if (!currentPassword.trim()) {
      newErrors.currentPassword = t("app.student.settings.errors.currentPasswordRequired");
    }
    
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }
    
    try {
      const response = await fetch(`/api/settings/email/${userId}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          newEmail,
          currentPassword,
        }),
      });
      
      if (response.ok) {
        const data = await response.json();
        setSuccessMessage(data.message || t("app.student.settings.success.emailChanged"));
        setCurrentEmail(newEmail);
        setNewEmail("");
        setConfirmEmail("");
        setCurrentPassword("");
        setErrors({});
      } else {
        const errorData = await response.json();
        setErrors({ form: errorData.error || "Failed to change email" });
      }
    } catch (error) {
      console.error("Failed to change email:", error);
      setErrors({ form: "Failed to change email" });
    }
  };

  const handleChangePassword = async () => {
    const newErrors = {};
    
    if (!currentPassword.trim()) {
      newErrors.currentPassword = t("app.student.settings.errors.currentPasswordRequired");
    }
    
    if (!newPassword.trim()) {
      newErrors.newPassword = t("app.student.settings.errors.passwordRequired");
    } else if (newPassword.length < 8) {
      newErrors.newPassword = t("app.student.settings.errors.passwordShort");
    }
    
    if (!confirmPassword.trim()) {
      newErrors.confirmPassword = t("app.student.settings.errors.passwordRequired");
    } else if (newPassword !== confirmPassword) {
      newErrors.confirmPassword = t("app.student.settings.errors.passwordsMismatch");
    }
    
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }
    
    try {
      const response = await fetch(`/api/settings/password/${userId}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          currentPassword,
          newPassword,
        }),
      });
      
      if (response.ok) {
        const data = await response.json();
        setSuccessMessage(data.message || t("app.student.settings.success.passwordChanged"));
        setCurrentPassword("");
        setNewPassword("");
        setConfirmPassword("");
        setErrors({});
      } else {
        const errorData = await response.json();
        setErrors({ form: errorData.error || "Failed to change password" });
      }
    } catch (error) {
      console.error("Failed to change password:", error);
      setErrors({ form: "Failed to change password" });
    }
  };

  const handleAddPhone = () => {
    const newErrors = {};
    
    if (!newPhoneNumber.trim()) {
      newErrors.newPhoneNumber = t("app.student.settings.errors.phoneRequired");
    } else if (!validatePhone(newPhoneNumber)) {
      newErrors.newPhoneNumber = t("app.student.settings.errors.phoneInvalid");
    }
    
    if (!newPhoneLabel.trim()) {
      newErrors.newPhoneLabel = "Label is required";
    }
    
    if (Object.keys(newErrors).length > 0) {
      setErrors(newErrors);
      return;
    }
    
    const newPhone = {
      id: Date.now(),
      label: newPhoneLabel,
      number: newPhoneNumber
    };
    
    setAdditionalPhones([...additionalPhones, newPhone]);
    setNewPhoneLabel("");
    setNewPhoneNumber("");
    setErrors({});
    setSuccessMessage(t("app.student.settings.success.phoneAdded"));
  };

  const handleRemovePhone = (id) => {
    setAdditionalPhones(additionalPhones.filter(phone => phone.id !== id));
    setSuccessMessage(t("app.student.settings.success.phoneRemoved"));
  };

  const handleSavePhone = async () => {
    try {
      const response = await fetch(`/api/settings/phone/${userId}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          phone: primaryPhone,
        }),
      });
      
      if (response.ok) {
        const data = await response.json();
        setSuccessMessage(data.message || t("app.student.settings.success.settingsSaved"));
        setErrors({});
      } else {
        const errorData = await response.json();
        setErrors({ form: errorData.error || "Failed to save phone number" });
      }
    } catch (error) {
      console.error("Failed to save phone:", error);
      setErrors({ form: "Failed to save phone number" });
    }
  };

  const handleSaveEmailLanguage = async () => {
    try {
      const response = await fetch(`/api/settings/email-language/${userId}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          emailLanguage,
        }),
      });
      
      if (response.ok) {
        const data = await response.json();
        setSuccessMessage(data.message || t("app.student.settings.success.settingsSaved"));
        setErrors({});
      } else {
        const errorData = await response.json();
        setErrors({ form: errorData.error || "Failed to save email language" });
      }
    } catch (error) {
      console.error("Failed to save email language:", error);
      setErrors({ form: "Failed to save email language" });
    }
  };

  const handleSaveSettings = async () => {
    // Save phone number
    await handleSavePhone();
    
    // Save email language preference
    await handleSaveEmailLanguage();
    
    // Save notifications preferences
    try {
      const res = await fetch(`/api/settings/notifications/${userId}`, {
        method: "PUT",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          emailNotifications: !!notifications.email,
          lessonReminders: !!notifications.lessonReminders,
          changeNotifications: !!notifications.changeNotifications
        })
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.error || "Failed to save notifications");
      }
      setSuccessMessage(t("app.student.settings.success.settingsSaved"));
    } catch (e) {
      setErrors({ form: e.message || "Failed to save notifications" });
    }
    
    // TODO: Save notifications preferences when backend supports it
  };

  const toggleNotification = (key) => {
    setNotifications(prev => ({ ...prev, [key]: !prev[key] }));
  };

  const [working, setWorking] = useState(false);
  const [confirmAction, setConfirmAction] = useState(null); // 'deactivate' | 'activate' | 'delete'

  const performDeactivate = async () => {
    if (!token || !userId) return;
    setWorking(true);
    try {
      const res = await fetch(`/api/settings/account/${userId}/deactivate`, {
        method: "PUT",
        headers: { Authorization: `Bearer ${token}` }
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.error || "Failed to deactivate account");
      }
      setSuccessMessage(t("app.settings.success.deactivated"));
    } catch (e) {
      setErrors({ form: e.message || "Failed" });
    } finally {
      setWorking(false);
      setConfirmAction(null);
    }
  };

  const performActivate = async () => {
    if (!token || !userId) return;
    setWorking(true);
    try {
      const res = await fetch(`/api/settings/account/${userId}/activate`, {
        method: "PUT",
        headers: { Authorization: `Bearer ${token}` }
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.error || "Failed to activate account");
      }
      setSuccessMessage(t("app.settings.success.activated"));
    } catch (e) {
      setErrors({ form: e.message || "Failed" });
    } finally {
      setWorking(false);
      setConfirmAction(null);
    }
  };

  const performDelete = async () => {
    if (!token || !userId) return;
    setWorking(true);
    try {
      const res = await fetch(`/api/settings/account/${userId}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` }
      });
      if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.error || "Failed to delete account");
      }
      setSuccessMessage(t("app.settings.success.deleted"));
      localStorage.clear();
      setTimeout(() => (window.location.href = "/"), 1200);
    } catch (e) {
      setErrors({ form: e.message || "Failed" });
    } finally {
      setWorking(false);
      setConfirmAction(null);
    }
  };

  return (
    <div className="student-settings">
      <div className="settings-header">
        <h2>{t("app.student.settings.title")}</h2>
        {successMessage && (
          <div className="success-message">{successMessage}</div>
        )}
      </div>

      {/* Account Information */}
      <div className="settings-section">
        <h3>{t("app.student.settings.sections.account")}</h3>
        
        <div className="form-group">
          <label>{t("app.student.settings.fields.currentEmail")}</label>
          <input 
            type="email" 
            value={currentEmail} 
            disabled 
            className="disabled-input"
          />
        </div>

        <div className="form-group">
          <label>{t("app.student.settings.fields.newEmail")}</label>
          <input
            type="email"
            value={newEmail}
            onChange={(e) => setNewEmail(e.target.value)}
            placeholder={t("app.student.settings.placeholders.newEmail")}
          />
          {errors.newEmail && <div className="error-message">{errors.newEmail}</div>}
        </div>

        <div className="form-group">
          <label>{t("app.student.settings.fields.confirmEmail")}</label>
          <input
            type="email"
            value={confirmEmail}
            onChange={(e) => setConfirmEmail(e.target.value)}
            placeholder={t("app.student.settings.placeholders.confirmEmail")}
          />
          {errors.confirmEmail && <div className="error-message">{errors.confirmEmail}</div>}
        </div>

        <div className="form-group">
          <label>{t("app.student.settings.fields.currentPassword")}</label>
          <input
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            placeholder={t("app.student.settings.placeholders.currentPassword")}
          />
          {errors.currentPassword && <div className="error-message">{errors.currentPassword}</div>}
        </div>

        {errors.form && <div className="error-message">{errors.form}</div>}

        <div className="settings-actions">
          <Button variant="primary" onClick={handleChangeEmail}>
            {t("app.student.settings.actions.changeEmail")}
          </Button>
        </div>
      </div>

      {/* Security */}
      <div className="settings-section">
        <h3>{t("app.student.settings.sections.security")}</h3>
        
        <div className="form-group">
          <label>{t("app.student.settings.fields.currentPassword")}</label>
          <input
            type="password"
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            placeholder={t("app.student.settings.placeholders.currentPassword")}
          />
          {errors.currentPassword && <div className="error-message">{errors.currentPassword}</div>}
        </div>

        <div className="form-group">
          <label>{t("app.student.settings.fields.newPassword")}</label>
          <input
            type="password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder={t("app.student.settings.placeholders.newPassword")}
          />
          {errors.newPassword && <div className="error-message">{errors.newPassword}</div>}
        </div>

        <div className="form-group">
          <label>{t("app.student.settings.fields.confirmPassword")}</label>
          <input
            type="password"
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder={t("app.student.settings.placeholders.confirmPassword")}
          />
          {errors.confirmPassword && <div className="error-message">{errors.confirmPassword}</div>}
        </div>

        {errors.form && <div className="error-message">{errors.form}</div>}

        <div className="settings-actions">
          <Button variant="primary" onClick={handleChangePassword}>
            {t("app.student.settings.actions.changePassword")}
          </Button>
        </div>
      </div>

      {/* Phone Numbers */}
      <div className="settings-section">
        <h3>{t("app.student.settings.fields.phoneNumbers")}</h3>
        
        <div className="form-group">
          <label>{t("app.student.settings.fields.primaryPhone")}</label>
          <input
            type="tel"
            value={primaryPhone}
            onChange={(e) => setPrimaryPhone(e.target.value)}
            placeholder={t("app.student.settings.placeholders.phoneNumber")}
          />
        </div>

        <div className="additional-phones">
          <h4>{t("app.student.settings.fields.additionalPhones")}</h4>
          
          {additionalPhones.map(phone => (
            <div key={phone.id} className="phone-item">
              <span className="phone-label">{phone.label}:</span>
              <span className="phone-number">{phone.number}</span>
              <Button 
                variant="danger" 
                size="small"
                onClick={() => handleRemovePhone(phone.id)}
              >
                {t("app.student.settings.actions.removePhone")}
              </Button>
            </div>
          ))}

          <div className="add-phone-form">
            <input
              type="text"
              value={newPhoneLabel}
              onChange={(e) => setNewPhoneLabel(e.target.value)}
              placeholder={t("app.student.settings.placeholders.phoneLabel")}
              className="phone-label-input"
            />
            <input
              type="tel"
              value={newPhoneNumber}
              onChange={(e) => setNewPhoneNumber(e.target.value)}
              placeholder={t("app.student.settings.placeholders.phoneNumber")}
              className="phone-number-input"
            />
            <Button variant="secondary" onClick={handleAddPhone}>
              {t("app.student.settings.actions.addPhone")}
            </Button>
          </div>
          {errors.newPhoneNumber && <div className="error-message">{errors.newPhoneNumber}</div>}
          {errors.newPhoneLabel && <div className="error-message">{errors.newPhoneLabel}</div>}
        </div>

        {errors.form && <div className="error-message">{errors.form}</div>}

        <div className="settings-actions">
          <Button variant="primary" onClick={handleSavePhone}>
            {t("app.student.settings.actions.saveChanges")}
          </Button>
        </div>
      </div>


      {/* Notifications */}
      <div className="settings-section">
        <h3>{t("app.student.settings.sections.notifications")}</h3>
        
        <div className="notification-settings">
          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={notifications.email}
              onChange={() => toggleNotification('email')}
            />
            {t("app.student.settings.fields.emailNotifications")}
          </label>

          <div className="form-group">
            <label>{t("app.student.settings.fields.emailLanguage")}</label>
            <div className="language-toggle">
              <button
                type="button"
                className={`language-btn ${emailLanguage === 'pl' ? 'active' : ''}`}
                onClick={() => setEmailLanguage('pl')}
              >
                {t("app.student.settings.languages.polish")}
              </button>
              <button
                type="button"
                className={`language-btn ${emailLanguage === 'en' ? 'active' : ''}`}
                onClick={() => setEmailLanguage('en')}
              >
                {t("app.student.settings.languages.english")}
              </button>
            </div>
          </div>

          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={notifications.lessonReminders}
              onChange={() => toggleNotification('lessonReminders')}
            />
            {t("app.student.settings.fields.lessonReminders")}
      </label>

          <label className="checkbox-label">
            <input
              type="checkbox"
              checked={notifications.changeNotifications}
              onChange={() => toggleNotification('changeNotifications')}
            />
            {t("app.student.settings.fields.changeNotifications")}
      </label>
        </div>

        <div className="settings-actions">
          <Button variant="primary" onClick={handleSaveSettings}>
            {t("app.student.settings.actions.saveChanges")}
          </Button>
        </div>
      </div>

      {/* Account controls */}
      <div className="settings-section">
        <h3>{t("app.settings.sections.accountControls")}</h3>
        {confirmAction && (
          <Alert variant="warning">
            {confirmAction === 'deactivate' && t("app.settings.confirmations.deactivate")}
            {confirmAction === 'activate' && t("app.settings.confirmations.activate")}
            {confirmAction === 'delete' && t("app.settings.confirmations.delete")}
            <div style={{ marginTop: 12, display: 'flex', gap: 8 }}>
              <Button
                size="small"
                variant={confirmAction === 'delete' ? 'danger' : 'primary'}
                disabled={working}
                onClick={() => (
                  confirmAction === 'deactivate' ? performDeactivate() :
                  confirmAction === 'activate' ? performActivate() :
                  performDelete()
                )}
              >
                OK
              </Button>
              <Button size="small" variant="secondary" disabled={working} onClick={() => setConfirmAction(null)}>
                {t("actions.cancel", { defaultValue: "Cancel" })}
              </Button>
            </div>
          </Alert>
        )}
        <div className="settings-actions">
          <Button variant="secondary" disabled={working} onClick={() => setConfirmAction('deactivate')}>
            {t("app.settings.actions.deactivate")}
          </Button>
          <Button variant="secondary" disabled={working} onClick={() => setConfirmAction('activate')}>
            {t("app.settings.actions.activate")}
          </Button>
          <Button variant="danger" disabled={working} onClick={() => setConfirmAction('delete')}>
            {t("app.settings.actions.delete")}
          </Button>
        </div>
      </div>
    </div>
  );
}
