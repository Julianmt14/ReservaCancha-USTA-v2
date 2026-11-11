import { apiFetch } from './client'
import { remainingMs, type AuthResponse } from './auth'

/** Cancha como la entrega GET /api/canchas (público). */
export interface CanchaPublica {
  id: number
  nombre: string
  tipo: string | null
  superficie: string | null
  precioHora: number
  /** Abono que se paga en línea al reservar; 0 = todo en la cancha; null = valor completo. */
  porcentajeAbono: number | null
  activa: boolean
}

export interface Bloque {
  inicio: string // "HH:mm" o "HH:mm:ss"
  fin: string
}

/** Respuesta de GET /api/disponibilidad (público). */
export interface Disponibilidad {
  canchaId: number
  fecha: string
  ocupadas: Bloque[]
  libres: Bloque[]
}

export interface ReservaRapidaRequest {
  nombre: string
  celular: string
  canchaId: number
  fecha: string // YYYY-MM-DD
  horaInicio: string // HH:mm:ss
  horaFin: string
}

interface ReservaDto {
  id: number
  fecha: string
  horaInicio: string
  horaFin: string
  valorTotal: number
  estado: string
  cancha: CanchaPublica
  usuario: { id: number; nombre: string; celular?: string | null }
}

interface SesionDto {
  token: string
  email: string
  rol: 'JUGADOR'
  id: number
  nombre: string
}

export interface ReservaLista {
  reserva: ReservaDto
  sesion: AuthResponse
  whatsapp: string | null
}

export function getCanchas(): Promise<CanchaPublica[]> {
  return apiFetch<CanchaPublica[]>('/api/canchas')
}

export function getDisponibilidad(canchaId: number, fecha: string): Promise<Disponibilidad> {
  return apiFetch<Disponibilidad>(`/api/disponibilidad?canchaId=${canchaId}&fecha=${fecha}`)
}

/** Porcentaje que se cobra en línea (las canchas sin el dato cobran todo, como antes). */
export function abonoDe(c: Pick<CanchaPublica, 'porcentajeAbono'>): number {
  return c.porcentajeAbono ?? 100
}

/** POST /api/reservas/rapida: reserva sin cuenta y devuelve la sesión del jugador de ese celular. */
export async function reservarRapido(req: ReservaRapidaRequest): Promise<ReservaLista> {
  const dto = await apiFetch<{ reserva: ReservaDto; sesion: SesionDto; whatsapp: string | null }>(
    '/api/reservas/rapida',
    { method: 'POST', body: JSON.stringify(req) },
    // Sin token: aunque haya una sesión guardada, la reserva queda a nombre del celular que se escribió
    null
  )
  return {
    reserva: dto.reserva,
    whatsapp: dto.whatsapp,
    sesion: {
      token: dto.sesion.token,
      tokenType: 'Bearer',
      expiresInMs: remainingMs(dto.sesion.token),
      userId: dto.sesion.id,
      email: dto.sesion.email,
      fullName: dto.sesion.nombre,
      role: 'JUGADOR',
    },
  }
}
