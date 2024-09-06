import Keycloak from 'keycloak-js'
import { useAuthStore } from '@/stores/common/authStore'
import type { KeycloakInitOptions } from 'keycloak-js'
import { KEYCLOAK } from '@/constants/constants'
import { Util } from '@/utils/util'

let keycloakInstance: Keycloak | null = null

const createKeycloakInstance = (): Keycloak => {
  if (!keycloakInstance) {
    keycloakInstance = new Keycloak({
      url: `${import.meta.env.VITE_SSO_AUTH_SERVER_URL}` as string,
      realm: `${import.meta.env.VITE_SSO_REALM}` as string,
      clientId: `${import.meta.env.VITE_SSO_CLIENT_ID}` as string,
    })
  }
  return keycloakInstance
}

const initOptions: KeycloakInitOptions = {
  pkceMethod: KEYCLOAK.PKCE_METHOD,
  checkLoginIframe: KEYCLOAK.CHECK_LOGIN_IFRAME,
  onLoad: KEYCLOAK.ONLOAD,
  silentCheckSsoRedirectUri: `${location.origin}${KEYCLOAK.SILENT_CHECK_SSO_REDIRECT_PAGE}`,
  enableLogging: KEYCLOAK.ENABLE_LOGGING,
}

const loginOptions = {
  redirectUri: import.meta.env.VITE_SSO_REDIRECT_URI as string,
}

export const initializeKeycloak = async (): Promise<Keycloak | undefined> => {
  try {
    keycloakInstance = createKeycloakInstance()

    const authStore = useAuthStore()

    // to avoid making a KeyCloak API on every request
    authStore.loadUserFromStorage()
    if (
      authStore.authenticated &&
      authStore.user?.accessToken &&
      authStore.user.refToken &&
      authStore.user.idToken
    ) {
      keycloakInstance.token = authStore.user.accessToken
      keycloakInstance.refreshToken = authStore.user.refToken
      keycloakInstance.idToken = authStore.user.idToken
      keycloakInstance.authenticated = true

      // Perform token validation
      if (!validateAccessToken(keycloakInstance.token)) {
        console.error('Token validation failed.')
        logout()
        return undefined
      }

      return keycloakInstance
    }

    const auth = await keycloakInstance.init(initOptions)

    if (
      auth &&
      keycloakInstance.token &&
      keycloakInstance.refreshToken &&
      keycloakInstance.idToken
    ) {
      const tokenParsed = JSON.parse(atob(keycloakInstance.token.split('.')[1]))
      // do validate the IDP in the JWT
      if (tokenParsed.identity_provider !== KEYCLOAK.IDP_AZUR_IDIR) {
        console.error('Authentication failed: Invalid identity provider.')
        logout()
        return undefined
      }

      // Perform token validation
      if (!validateAccessToken(keycloakInstance.token)) {
        console.error('Token validation failed.')
        logout()
        return undefined
      }

      authStore.setUser({
        accessToken: keycloakInstance.token,
        refToken: keycloakInstance.refreshToken,
        idToken: keycloakInstance.idToken,
      })

      return keycloakInstance
    } else {
      console.error('Keycloak initialization failed.')
      keycloakInstance.login(loginOptions)
    }
  } catch (err) {
    console.error('Keycloak initialization failed:', err)
    keycloakInstance = null // Reset the instance on failure
    throw err
  }
}

const validateAccessToken = (accessToken: string): boolean => {
  try {
    const tokenParsed = JSON.parse(atob(accessToken.split('.')[1]))

    // Validate issuer
    if (
      tokenParsed.iss !==
      `${import.meta.env.VITE_SSO_AUTH_SERVER_URL}/realms/${import.meta.env.VITE_SSO_REALM}`
    ) {
      console.error('Invalid token issuer.')
      return false
    }

    // validate subject
    if (!tokenParsed.sub) {
      console.error('Token subject is missing.')
      return false
    }

    return true
  } catch (error) {
    console.error('Failed to validate token:', error)
    return false
  }
}

/**
 * checks if the access token is valid, refreshes it if not, and stores it afterwards.
 * @returns null if valid, or null if the token has been refreshed. If the token fails to refresh or it is not available, return an error message.
 */
export const handleTokenValidation = async (): Promise<void> => {
  try {
    if (!keycloakInstance) {
      keycloakInstance = createKeycloakInstance()
    }

    const authStore = useAuthStore()

    // not initialized, the token not be refreshed
    if (!keycloakInstance.clientId) {
      if (
        !authStore.user?.accessToken ||
        !authStore.user?.refToken ||
        !authStore.user?.idToken
      ) {
        console.error('Auth load failed')
        logout() // force to logout
        return
      }

      const auth = await keycloakInstance.init({
        token: authStore.user.accessToken,
        refreshToken: authStore.user.refToken,
        idToken: authStore.user.idToken,
      })

      if (!auth || !keycloakInstance.token) {
        console.error('Keycloak initialization failed')
        logout()
        return
      }
    }

    const currentTime = Date.now()
    const authTimeInUnixTime = getAuthTimeInUnixTime(
      authStore.user!.accessToken,
    )
    const sessionDuration = currentTime - authTimeInUnixTime

    // force session logout when the maximum session time is exceeded
    if (sessionDuration > KEYCLOAK.MAX_SESSION_DURATION) {
      console.warn('Session has exceeded the maximum duration, logging out.')
      logout()
      return
    }

    console.log(
      `access token expired date: ${getTokenExpirationDate(authStore.user!.accessToken)}`,
    )
    console.log(`access token expired!: ${keycloakInstance.isTokenExpired()}`)

    if (keycloakInstance.isTokenExpired()) {
      try {
        const refreshed = await keycloakInstance.updateToken(
          KEYCLOAK.UPDATE_TOKEN_MIN_VALIDITY,
        )
        console.log(refreshed ? 'Token was refreshed' : 'Token is still valid')
        if (refreshed) {
          authStore.setUser({
            accessToken: keycloakInstance.token!,
            refToken: keycloakInstance.refreshToken!,
            idToken: keycloakInstance.idToken!,
          })
        } else {
          console.error('Token refresh failed')
          logout()
          return
        }
      } catch (error) {
        console.error('Error refreshing token:', error)
        logout()
        return
      }
    }
  } catch (error) {
    console.error('Error during token validation:', error)
    logout()
    return
  }
}

export const logout = (): void => {
  const authStore = useAuthStore()
  authStore.clearUser()
  window.location.href = `https://logon7.gov.bc.ca/clp-cgi/logoff.cgi?retnow=1&returl=${encodeURIComponent(
    `${import.meta.env.VITE_SSO_AUTH_SERVER_URL}/realms/${import.meta.env.VITE_SSO_REALM}/protocol/openid-connect/logout?post_logout_redirect_uri=` +
      import.meta.env.VITE_SSO_REDIRECT_URI +
      '&client_id=' +
      import.meta.env.VITE_SSO_CLIENT_ID,
  )}`
}

const getAuthTimeInUnixTime = (accessToken: string): number => {
  try {
    const accessTokenParsed = JSON.parse(atob(accessToken.split('.')[1]))
    console.log(
      `auth_time: ${accessTokenParsed.auth_time} (${Util.formatUnixTimestampToDate(accessTokenParsed.auth_time)})`,
    )
    return accessTokenParsed.auth_time * 1000 // convert to milliseconds
  } catch (error) {
    console.error('Failed to parse auth_time from ID Token:', error)
    return Date.now() // the current time on failure
  }
}

const getTokenExpirationDate = (token: string): Date | null => {
  try {
    const tokenParsed = JSON.parse(atob(token.split('.')[1]))
    if (!tokenParsed.exp) {
      return null
    }

    const expirationDate = Util.formatUnixTimestampToDate(tokenParsed.exp)
    return expirationDate
  } catch (error) {
    console.error('Failed to parse token expiration:', error)
    return null
  }
}
