const API_BASE = import.meta.env.VITE_API_BASE || 'http://localhost:9090'

async function request(path, options = {}) {
  const isFormData = options.body instanceof FormData
  const headers = {
    ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
    ...(options.headers || {}),
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  })

  const contentType = response.headers.get('content-type') || ''
  const payload = contentType.includes('application/json')
    ? await response.json()
    : await response.text()

  if (!response.ok) {
    const message = typeof payload === 'string'
      ? payload
      : payload?.msg || payload?.message || 'Request failed'
    throw new Error(message)
  }

  return payload
}

export const api = {
  getJson(path) {
    return request(path)
  },
  postJson(path, body) {
    return request(path, {
      method: 'POST',
      body: JSON.stringify(body),
    })
  },
  postForm(path, formData) {
    return request(path, {
      method: 'POST',
      body: formData,
    })
  },
  async postText(path) {
    const payload = await request(path, { method: 'POST' })
    return typeof payload === 'string' ? payload : JSON.stringify(payload)
  },
  async deleteText(path) {
    const payload = await request(path, { method: 'DELETE' })
    return typeof payload === 'string' ? payload : JSON.stringify(payload)
  },
}
