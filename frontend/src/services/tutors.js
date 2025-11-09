const toQueryString = (params = {}) => {
  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null) return;
    if (Array.isArray(value)) {
      value.forEach((item) => {
        if (item !== undefined && item !== null && `${item}`.trim() !== "") {
          searchParams.append(key, `${item}`.trim());
        }
      });
    } else if (`${value}`.trim() !== "") {
      searchParams.set(key, `${value}`.trim());
    }
  });
  const query = searchParams.toString();
  return query ? `?${query}` : "";
};

export async function fetchTutors(filters = {}) {
  const query = toQueryString(filters);
  const response = await fetch(`/api/tutors${query}`);
  if (!response.ok) {
    const error = await safeJson(response);
    throw new Error(error?.error || "Failed to load tutors");
  }
  return response.json();
}

export async function fetchTutor(id) {
  const response = await fetch(`/api/tutors/${id}`);
  if (!response.ok) {
    const error = await safeJson(response);
    throw new Error(error?.error || "Tutor not found");
  }
  return response.json();
}

export async function fetchTutorBusyTimes(id) {
  const response = await fetch(`/api/calendar/public/${id}`);
  if (!response.ok) {
    const error = await safeJson(response);
    throw new Error(error?.error || "Failed to load tutor calendar");
  }
  return response.json();
}

export async function bookTutor(id, payload, token) {
  const response = await fetch(`/api/tutors/${id}/bookings`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const error = await safeJson(response);
    throw new Error(error?.error || "Failed to book lesson");
  }

  return response.json();
}

async function safeJson(response) {
  try {
    return await response.json();
  } catch (error) {
    return null;
  }
}


