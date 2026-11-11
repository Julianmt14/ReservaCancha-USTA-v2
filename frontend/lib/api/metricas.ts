import { apiFetch } from './client'

/** Respuesta de GET /api/admin/metricas (solo administrador, máximo 92 días). */
export interface Metricas {
  desde: string
  hasta: string
  reservas: number
  canceladas: number
  jugadores: number
  ingresos: number
  abonosCobrados: number
  porCancha: { canchaId: number; nombre: string; reservas: number; horas: number; ocupacion: number }[]
  porDia: { fecha: string; reservas: number }[]
  porHora: { hora: number; reservas: number }[]
}

export function getMetricas(desde: string, hasta: string): Promise<Metricas> {
  return apiFetch<Metricas>(`/api/admin/metricas?desde=${desde}&hasta=${hasta}`)
}
