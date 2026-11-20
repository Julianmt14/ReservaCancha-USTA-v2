import { NextRequest, NextResponse } from 'next/server'
import {
  isPublicRoute,
  isOpenRoute,
  DEFAULT_AUTHENTICATED_REDIRECT,
  DEFAULT_UNAUTHENTICATED_REDIRECT,
  HOME_WITHOUT_SESSION,
} from '@/lib/auth/routes'

export function proxy(req: NextRequest) {
  const { pathname } = req.nextUrl
  const token = req.cookies.get('token')?.value

  if (isOpenRoute(pathname)) return NextResponse.next()

  if (!token && !isPublicRoute(pathname)) {
    const url = req.nextUrl.clone()
    // La portada lleva a reservar; las páginas del panel (/canchas, /reservas, /pagos) piden ingresar
    url.pathname = pathname === '/' ? HOME_WITHOUT_SESSION : DEFAULT_UNAUTHENTICATED_REDIRECT
    return NextResponse.redirect(url)
  }

  if (token && isPublicRoute(pathname)) {
    const url = req.nextUrl.clone()
    url.pathname = DEFAULT_AUTHENTICATED_REDIRECT
    return NextResponse.redirect(url)
  }

  return NextResponse.next()
}

export const config = {
  matcher: ['/((?!_next/static|_next/image|favicon.ico).*)'],
}
