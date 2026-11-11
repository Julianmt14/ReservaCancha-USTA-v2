// Ingreso y registro: si ya hay sesión, mandan al panel
export const PUBLIC_ROUTES = ['/login', '/registro']

// HU-13: la reserva sin cuenta se abre con o sin sesión
export const OPEN_ROUTES = ['/reservar', '/img']

export const DEFAULT_AUTHENTICATED_REDIRECT = '/'
export const DEFAULT_UNAUTHENTICATED_REDIRECT = '/login'
// Quien abre la dirección pública sin sesión llega a la reserva, no al ingreso
export const HOME_WITHOUT_SESSION = '/reservar'

export function isPublicRoute(pathname: string): boolean {
  return PUBLIC_ROUTES.some((route) => pathname.startsWith(route))
}

export function isOpenRoute(pathname: string): boolean {
  return OPEN_ROUTES.some((route) => pathname === route || pathname.startsWith(route + '/'))
}
