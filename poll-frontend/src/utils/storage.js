const TOKEN_KEY = 'pollapp_token'

export const getToken = () => {
  const t = localStorage.getItem(TOKEN_KEY)
  return t ? t.trim() : null
}
export const setToken = (t) => localStorage.setItem(TOKEN_KEY, (t || '').trim())
export const clearToken = () => localStorage.removeItem(TOKEN_KEY)
