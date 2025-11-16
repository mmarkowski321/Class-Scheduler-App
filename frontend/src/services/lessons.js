const authHeaders = (token, options = {}) => {
  const headers = {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  };
  if (options.language) {
    headers["Accept-Language"] = options.language;
  }
  return headers;
};

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

export async function declineTutorBooking(tutorId, lessonId, token, body = {}, language) {
  const response = await fetch(`/api/tutors/${tutorId}/bookings/${lessonId}/decline`, {
    method: "POST",
    headers: authHeaders(token, { language }),
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    const error = await safeJson(response);
    throw new Error(error?.error || "Failed to decline booking");
  }
  return response.json();
}

export async function proposeTutorBooking(tutorId, lessonId, payload, token, language) {
  const response = await fetch(`/api/tutors/${tutorId}/bookings/${lessonId}/propose`, {
    method: "POST",
    headers: authHeaders(token, { language }),
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    const error = await safeJson(response);
    throw new Error(error?.error || "Failed to propose new time");
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

export async function fetchStudentOverview(token, language) {
  const response = await fetch(`/api/students/me/overview`, {
    headers: authHeaders(token, { language }),
  });
  if (!response.ok) {
    const error = await safeJson(response);
    throw new Error(error?.error || "Failed to load student overview");
  }
  return response.json();
}

export async function fetchStudentLessons(token, language) {
  const response = await fetch(`/api/students/me/lessons`, {
    headers: authHeaders(token, { language }),
  });
  if (!response.ok) {
    const error = await safeJson(response);
    throw new Error(error?.error || "Failed to load student lessons");
  }
  return response.json();
}

export async function cancelStudentLesson(lessonId, token, body = {}, language) {
  const response = await fetch(`/api/students/me/lessons/${lessonId}/cancel`, {
    method: "POST",
    headers: authHeaders(token, { language }),
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    const error = await safeJson(response);
    throw new Error(error?.error || "Failed to cancel lesson");
  }
  return response.json();
}

export async function rescheduleStudentLesson(lessonId, payload, token, language) {
  const response = await fetch(`/api/students/me/lessons/${lessonId}/reschedule`, {
    method: "POST",
    headers: authHeaders(token, { language }),
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    const error = await safeJson(response);
    throw new Error(error?.error || "Failed to reschedule lesson");
  }
  return response.json();
}

export async function submitTutorReview(lessonId, payload, token, language) {
  const response = await fetch(`/api/reviews/lesson/${lessonId}/tutor`, {
    method: "POST",
    headers: authHeaders(token, { language }),
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    const error = await safeJson(response);
    throw new Error(error?.error || "Failed to submit tutor review");
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


