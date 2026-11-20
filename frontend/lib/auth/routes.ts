// Ingreso y registro: si ya hay sesión, mandan al panel
export const PUBLIC_ROUTES = ['/login', '/registro']

// HU-13: la reserva sin cuenta se abre con o sin sesión
export const OPEN_ROUTES = ['/reservar', '/img']

export const DEFAULT_AUTHENTICATED_REDIRECT = '/'
export const DEFAULT_UNAUTHENTICATED_REDIRECT = '/login'
// Quien abre la dirección pública sin sesión llega a la reserva, no al ingreso
export const HOME_WITHOUT_SESSION = '/reservar'

// HU-15: el empleado solo atiende reservas; métricas, canchas, pagos y empleados son del administrador
export const EMPLOYEE_ROUTES = ['/reservas']

function matches(routes: string[], pathname: string): boolean {
  return routes.some((route) => pathname === route || pathname.startsWith(route + '/'))
}

/** Adónde llega cada rol al iniciar sesión. */
export function homeFor(role?: string | null): string {
  if (role === 'JUGADOR') return '/reservar'
  if (role === 'EMPLEADO') return '/reservas'
  return DEFAULT_AUTHENTICATED_REDIRECT
}

/** Si ese rol puede abrir esa página del panel (la API además responde 403). */
export function canVisit(role: string | null | undefined, pathname: string): boolean {
  if (role === 'EMPLEADO') return matches(EMPLOYEE_ROUTES, pathname)
  return true
}

export function isPublicRoute(pathname: string): boolean {
  return PUBLIC_ROUTES.some((route) => pathname.startsWith(route))
}

export function isOpenRoute(pathname: string): boolean {
  return OPEN_ROUTES.some((route) => pathname === route || pathname.startsWith(route + '/'))
}
