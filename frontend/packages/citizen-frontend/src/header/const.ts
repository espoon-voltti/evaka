export const getLoginUri = () =>
  `/api/application/auth/saml/login?RelayState=${encodeURIComponent(
    `${window.location.pathname}${window.location.search}${window.location.hash}`
  )}`

export const headerHeight = '52px'
