import "./alert.css";

const variantIcons = {
  info: "ℹ️",
  success: "✅",
  warning: "⚠️",
  error: "🚫",
};

export default function Alert({ variant = "info", children }) {
  if (!children) return null;
  const icon = variantIcons[variant] || variantIcons.info;

  return (
    <div className={`alert alert-${variant}`}>
      <span className="alert-icon" aria-hidden="true">
        {icon}
      </span>
      <span className="alert-message">{children}</span>
    </div>
  );
}





