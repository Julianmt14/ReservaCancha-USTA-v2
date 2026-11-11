import { apiFetch } from './client'

export interface RegisterRequest {
  fullName: string
  email: string
  phone: string
  password: string
}

/**
 * Registro público: solo crea jugadores (TEC-05). El backend responde 403 a cualquier otro rol; las
 * cuentas del personal las crea el administrador en «Empleados». El backend no guarda el teléfono.
 */
export function registerPlayer(data: RegisterRequest) {
  return apiFetch('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify({
      nombre: data.fullName,
      email: data.email,
      password: data.password,
      rol: 'JUGADOR',
    }),
  })
}
