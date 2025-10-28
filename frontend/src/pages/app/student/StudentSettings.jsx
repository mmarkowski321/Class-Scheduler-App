import { useState } from "react";
import { useTranslation } from "react-i18next";
import Button from "../../../components/ui/Button";
import "./student-settings.css";

export default function StudentSettings() {
  const { t } = useTranslation("common");
  
  // Account Information
  const [currentEmail, setCurrentEmail] = useState("student@example.com");
  const [newEmail, setNewEmail] = useState("");
  const [confirmEmail, setConfirmEmail] = useState("");
  
  // Security
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  
  // Phone Numbers
  const [primaryPhone, setPrimaryPhone] = useState("+48 123 456 789");
  const [additionalPhones, setAdditionalPhones] = useState([
    { id: 1, label: "Rodzice", number: "+48 987 654 321" }
  ]);
  const [newPhoneLabel, setNewPhoneLabel] = useState("");
  const [newPhoneNumber, setNewPhoneNumber] = useState("");
  
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
    
    // TODO: API call to change email
    console.log("Changing email:", { newEmail, currentPassword });
    setSuccessMessage(t("app.student.settings.success.emailChanged"));
    setNewEmail("");
    setConfirmEmail("");
    setCurrentPassword("");
    setErrors({});
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
    
    // TODO: API call to change password
    console.log("Changing password:", { currentPassword, newPassword });
    setSuccessMessage(t("app.student.settings.success.passwordChanged"));
    setCurrentPassword("");
    setNewPassword("");
    setConfirmPassword("");
    setErrors({});
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

  const handleSaveSettings = async () => {
    // TODO: API call to save all settings
    console.log("Saving settings:", {
      notifications,
      emailLanguage,
      primaryPhone,
      additionalPhones
    });
    setSuccessMessage(t("app.student.settings.success.settingsSaved"));
  };

  const toggleNotification = (key) => {
    setNotifications(prev => ({ ...prev, [key]: !prev[key] }));
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

        <div className="settings-actions">
          <Button variant="primary" onClick={handleSaveSettings}>
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
    </div>
  );
}
