export function decodeJwtPayload(token) {
  if (!token) return null;
  const segments = token.split(".");
  if (segments.length < 2) return null;
  const base64Url = segments[1];
  const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
  try {
    const json = atob(padded);
    return JSON.parse(json);
  } catch (err) {
    console.warn("Failed to decode JWT payload", err);
    return null;
  }
}

export function extractRoleFromToken(token) {
  const payload = decodeJwtPayload(token);
  if (!payload) return null;

  const raw =
    payload.role ??
    payload.roles ??
    payload.authorities ??
    (Array.isArray(payload.authorities) ? payload.authorities[0] : null);

  let value = null;
  if (Array.isArray(raw)) {
    value = raw.length > 0 ? raw[0] : null;
  } else if (typeof raw === "string") {
    value = raw;
  }

  if (!value) return null;

  const upper = value.toUpperCase();
  if (upper.startsWith("ROLE_")) {
    return upper.substring(5);
  }
  return upper;
}

export function ensureRoleInStorage(
  token,
  storage = typeof window !== "undefined" ? window.localStorage : null
) {
  if (!storage) return null;
  const existing = storage.getItem("role");
  if (existing) return existing;
  const extracted = extractRoleFromToken(token);
  if (extracted) {
    storage.setItem("role", extracted);
  }
  return extracted;
}

export function clearAuthSession(
  storage = typeof window !== "undefined" ? window.localStorage : null
) {
  if (!storage) return;
  try {
    storage.removeItem("token");
    storage.removeItem("access_token");
    storage.removeItem("refresh_token");
    storage.removeItem("role");
    storage.removeItem("userId");
    storage.removeItem("firstName");
    storage.removeItem("lastName");
    storage.removeItem("devRole");
  } catch (err) {
    console.warn("Failed to clear auth session", err);
  }
}



