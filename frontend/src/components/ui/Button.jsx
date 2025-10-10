import { useState } from 'react';
import './Button.css';

function Button({
  children,
  variant = 'primary',
  size = 'medium',
  onClick,
  type = 'button',
  disabled = false,
  className = '',
}) {
  const [ripples, setRipples] = useState([]);
  const buttonClass = `btn btn-${variant} btn-${size} ${className}`.trim();

  const handleClick = (e) => {
    const rect = e.target.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    const newRipple = { x, y, id: Date.now() };
    setRipples((prev) => [...prev, newRipple]);

    if (onClick) onClick(e);

    // Usuń efekt po 600ms
    setTimeout(() => {
      setRipples((prev) => prev.filter((r) => r.id !== newRipple.id));
    }, 600);
  };

  return (
    <button
      className={buttonClass}
      onClick={handleClick}
      type={type}
      disabled={disabled}
    >
      {children}
      {ripples.map((r) => (
        <span
          key={r.id}
          className="ripple"
          style={{ top: r.y, left: r.x }}
        />
      ))}
    </button>
  );
}

export default Button;
