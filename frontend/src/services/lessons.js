const authHeaders = (token) => ({
  Authorization: `Bearer ${token}`,
  "Content-Type": "application/json",
});

export async function fetchTutorBookings(tutorId, token) {
  const response = await fetch(`/api/tutors/${tutorId}/bookings`, {
    headers: authHeaders(token),
  });
  if (!response.ok) {
    const error = await safeJson(response);
    throw new Error(error?.error || "Failed to load tutor bookings");
  }
  return response.json();
}

export async function confirmTutorBooking(tutorId, lessonId, token) {
  const response = await fetch(`/api/tutors/${tutorId}/bookings/${lessonId}/confirm`, {
    method: "POST",
    headers: authHeaders(token),
  });
  if (!response.ok) {
    const error = await safeJson(response);
    throw new Error(error?.error || "Failed to confirm booking");
  }
  return response.json();
}

export async function declineTutorBooking(tutorId, lessonId, token) {
  const response = await fetch(`/api/tutors/${tutorId}/bookings/${lessonId}/decline`, {
    method: "POST",
    headers: authHeaders(token),
  });
  if (!response.ok) {
    const error = await safeJson(response);
    throw new Error(error?.error || "Failed to decline booking");
  }
  return response.json();
}

export async function fetchTutorOverview(tutorId, token) {
  const response = await fetch(`/api/tutors/${tutorId}/overview`, {
    headers: authHeaders(token),
  });
  if (!response.ok) {
    const error = await safeJson(response);
    throw new Error(error?.error || "Failed to load tutor overview");
  }
  return response.json();
}

async function safeJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}


