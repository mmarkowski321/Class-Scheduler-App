import { useEffect } from "react";
import { useLocation } from "react-router-dom";

export default function ScrollToSection() {
  const location = useLocation();

  useEffect(() => {
    const anchor = location.state?.scrollTo || (location.hash ? location.hash.slice(1) : null);
    if (!anchor) return;

    const t = setTimeout(() => {
      const el = document.getElementById(anchor);
      if (el) {
        el.scrollIntoView({ behavior: "smooth", block: "start" });
      }
    }, 0);

    return () => clearTimeout(t);
  }, [location]);

  return null;
}