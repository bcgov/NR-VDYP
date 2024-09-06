import { defineStore } from 'pinia'
import { logout as keycloakLogout } from '@/services/keycloak'

interface User {
  accessToken: string
  refToken: string
  idToken: string
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    authenticated: false,
    user: null as User | null,
  }),
  actions: {
    setUser(user: User) {
      this.user = user
      this.authenticated = true
      sessionStorage.setItem('authUser', JSON.stringify(user))
    },
    clearUser() {
      this.user = null
      this.authenticated = false
      sessionStorage.removeItem('authUser')
    },
    loadUserFromStorage() {
      const user = sessionStorage.getItem('authUser')
      if (user) {
        this.user = JSON.parse(user)
        this.authenticated = true
      }
    },
    parseIdToken() {
      if (!this.user?.idToken) return null

      try {
        const idTokenParsed = JSON.parse(atob(this.user.idToken.split('.')[1]))
        return {
          auth_time: idTokenParsed.auth_time,
          client_roles: idTokenParsed.client_roles,
          display_name: idTokenParsed.display_name,
          email: idTokenParsed.email,
          exp: idTokenParsed.exp,
          family_name: idTokenParsed.family_name,
          given_name: idTokenParsed.given_name,
          idir_username: idTokenParsed.idir_username,
          name: idTokenParsed.name,
          preferred_username: idTokenParsed.preferred_username,
          user_principal_name: idTokenParsed.user_principal_name,
        }
      } catch (error) {
        console.error('Failed to parse ID Token:', error)
        return null
      }
    },
    getAllRoles(): string[] {
      const parsedToken = this.parseIdToken()
      return parsedToken?.client_roles || []
    },
    hasRole(role: string): boolean {
      const roles = this.getAllRoles()
      return roles.includes(role)
    },
    async logout() {
      this.clearUser()
      keycloakLogout()
    },
  },
})
