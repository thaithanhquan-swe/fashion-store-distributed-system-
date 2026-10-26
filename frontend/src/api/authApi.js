import { client } from './client'

export const authApi = {
  login: ({ username, password }) =>
    client.post('/api/auth/login', { username, password }),

  getCurrentUser: () =>
    client.get('/api/auth/me'),

  changePassword: ({ currentPassword, newPassword }) =>
    client.put('/api/auth/change-password', { currentPassword, newPassword }),

  refreshToken: (refreshToken) =>
    client.post('/api/auth/refresh_token', { refreshToken }),
}

export default authApi
