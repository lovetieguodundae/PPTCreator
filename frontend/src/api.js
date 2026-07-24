async function request(path, options = {}) {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json', ...(options.headers || {}) },
    ...options
  })
  if (!response.ok) {
    let message = `请求失败（${response.status}）`
    try {
      const body = await response.json()
      message = body.message || message
    } catch {
      // Keep the status-based message when the body is not JSON.
    }
    throw new Error(message)
  }
  if (response.status === 204) return null
  return response.json()
}

export const api = {
  listSessions: () => request('/api/sessions'),
  getSession: (id) => request(`/api/sessions/${id}`),
  createSession: (payload) =>
    request('/api/sessions', { method: 'POST', body: JSON.stringify(payload) }),
  sendMessage: (id, message) =>
    request(`/api/sessions/${id}/messages`, {
      method: 'POST',
      body: JSON.stringify({ message })
    }),
  generate: (id) => request(`/api/sessions/${id}/generate`, { method: 'POST' }),
  removeSession: (id) => request(`/api/sessions/${id}`, { method: 'DELETE' })
}

