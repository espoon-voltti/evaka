export const enduserBaseUrl =
  window.location.host === 'localhost:9094' ? 'http://localhost:9091' : ''

export const getLoginUri = () =>
  `/api/application/auth/saml/login?RelayState=${encodeURIComponent(
    `${window.location.pathname}${window.location.search}${window.location.hash}`
  )}`

export const headerHeight = '52px'
