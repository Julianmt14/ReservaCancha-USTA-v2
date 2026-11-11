import { apiFetch } from './client'

/** Cuenta del personal como la entrega GET /api/admin/empleados (solo administrador). */
export interface Personal {
  id: number
  nombre: string
  email: string
  rol: 'ADMIN' | 'PROPIETARIO' | 'EMPLEADO'
  activo: boolean
}

export interface NuevoEmpleado {
  nombre: string
  email: string
  password: string
}

export function getPersonal(): Promise<Personal[]> {
  return apiFetch<Personal[]>('/api/admin/empleados')
}

/** Crea una cuenta EMPLEADO (correo repetido → 409). */
export function crearEmpleado(data: NuevoEmpleado): Promise<Personal> {
  return apiFetch<Personal>('/api/admin/empleados', { method: 'POST', body: JSON.stringify(data) })
}

/** Un empleado desactivado ya no inicia sesión y su token deja de servir. */
export function cambiarActivo(id: number, activo: boolean): Promise<Personal> {
  return apiFetch<Personal>(`/api/admin/empleados/${id}`, {
    method: 'PATCH',
    body: JSON.stringify({ activo }),
  })
}
