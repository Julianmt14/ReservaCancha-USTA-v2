'use client'

import { useEffect, useState } from 'react'
import Modal from '@/components/app/ui/Modal'
import { FormField, Input, SubmitRow } from '@/components/app/ui/FormField'
import { IconPlus } from '@/components/app/icons'
import { ApiError } from '@/lib/api/client'
import { cambiarActivo, crearEmpleado, getPersonal, type Personal } from '@/lib/api/empleados'

const ROL: Record<Personal['rol'], string> = {
  ADMIN: 'Administrador',
  PROPIETARIO: 'Propietario',
  EMPLEADO: 'Empleado',
}

function NuevoEmpleadoModal({
  open,
  onClose,
  onSaved,
}: {
  open: boolean
  onClose: () => void
  onSaved: (p: Personal) => void
}) {
  const [nombre, setNombre] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (password.length < 8) {
      setError('La clave debe tener al menos 8 caracteres.')
      return
    }
    setError(null)
    setLoading(true)
    try {
      onSaved(await crearEmpleado({ nombre: nombre.trim(), email: email.trim(), password }))
      onClose()
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) setError('Ya existe una cuenta con ese correo.')
      else if (err instanceof ApiError && err.status === 400)
        setError('Revisa el nombre, el correo y la clave.')
      else setError('No se pudo crear la cuenta. Intenta de nuevo.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal open={open} onClose={onClose} title="Nuevo empleado">
      <form onSubmit={handleSubmit}>
        <div className="px-6 py-5 flex flex-col gap-4">
          <FormField label="Nombre">
            <Input value={nombre} maxLength={60} onChange={(e) => setNombre(e.target.value)} required />
          </FormField>
          <FormField label="Correo">
            <Input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </FormField>
          <FormField label="Clave (mínimo 8 caracteres)">
            <Input
              type="password"
              autoComplete="new-password"
              value={password}
              maxLength={72}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </FormField>
          <p className="text-xs" style={{ color: 'var(--text-3)' }}>
            Entrégale la clave en persona. El empleado entra por «Ingreso del personal».
          </p>
          {error && (
            <p
              className="text-xs rounded-lg px-3 py-2"
              style={{ color: 'var(--red)', background: 'var(--red-soft)' }}
            >
              {error}
            </p>
          )}
        </div>
        <SubmitRow onCancel={onClose} loading={loading} label="Crear cuenta" />
      </form>
    </Modal>
  )
}

export default function EmpleadosView() {
  const [personal, setPersonal] = useState<Personal[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState(false)
  const [nuevo, setNuevo] = useState(false)
  // Cada apertura monta el formulario de nuevo, vacío
  const [formulario, setFormulario] = useState(0)
  const [cambiando, setCambiando] = useState<number | null>(null)

  useEffect(() => {
    getPersonal()
      .then(setPersonal)
      .catch(() => setLoadError(true))
      .finally(() => setLoading(false))
  }, [])

  async function alternar(p: Personal) {
    setCambiando(p.id)
    try {
      const actualizado = await cambiarActivo(p.id, !p.activo)
      setPersonal((prev) => prev.map((x) => (x.id === p.id ? actualizado : x)))
    } catch {
      // se deja como estaba
    } finally {
      setCambiando(null)
    }
  }

  return (
    <div className="flex flex-col gap-4">
      <div>
        <button
          onClick={() => {
            setFormulario((n) => n + 1)
            setNuevo(true)
          }}
          className="inline-flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-semibold cursor-pointer"
          style={{ background: 'var(--green)', color: '#fff' }}
        >
          <IconPlus />
          Nuevo empleado
        </button>
      </div>

      <div className="rounded-[14px] overflow-hidden" style={{ border: '1px solid var(--line)' }}>
        {loading ? (
          <p className="px-4 py-10 text-center text-xs" style={{ color: 'var(--text-3)' }}>
            Cargando…
          </p>
        ) : loadError ? (
          <p className="px-4 py-10 text-center text-xs" style={{ color: 'var(--red)' }}>
            No se pudo cargar el personal.
          </p>
        ) : (
          <ul>
            {personal.map((p, i) => (
              <li
                key={p.id}
                className="flex flex-wrap items-center justify-between gap-3 px-4 py-3"
                style={{
                  background: i % 2 === 0 ? 'var(--panel)' : 'var(--panel-2)',
                  borderTop: i === 0 ? 'none' : '1px solid var(--line)',
                }}
              >
                <div className="min-w-0">
                  <p className="text-sm font-semibold" style={{ color: 'var(--text)' }}>
                    {p.nombre}
                  </p>
                  <p className="font-mono text-xs truncate" style={{ color: 'var(--text-3)' }}>
                    {p.email}
                  </p>
                </div>
                <div className="flex items-center gap-3">
                  <span className="text-xs" style={{ color: 'var(--text-2)' }}>
                    {ROL[p.rol] ?? p.rol}
                  </span>
                  <span
                    className="text-[11px] font-semibold px-2 py-0.5 rounded-full"
                    style={{
                      color: p.activo ? 'var(--green)' : 'var(--text-3)',
                      background: p.activo ? 'rgba(27,158,75,0.14)' : 'var(--bg-2)',
                    }}
                  >
                    {p.activo ? 'Activo' : 'Inactivo'}
                  </span>
                  {p.rol === 'EMPLEADO' && (
                    <button
                      onClick={() => alternar(p)}
                      disabled={cambiando === p.id}
                      className="text-xs font-medium px-3 py-1.5 rounded-md cursor-pointer disabled:opacity-40"
                      style={
                        p.activo
                          ? { color: 'var(--red)', background: 'var(--red-soft)' }
                          : {
                              color: 'var(--text)',
                              background: 'var(--bg-2)',
                              border: '1px solid var(--line)',
                            }
                      }
                    >
                      {cambiando === p.id ? '…' : p.activo ? 'Desactivar' : 'Activar'}
                    </button>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </div>

      <NuevoEmpleadoModal
        key={formulario}
        open={nuevo}
        onClose={() => setNuevo(false)}
        onSaved={(p) => setPersonal((prev) => [...prev, p])}
      />
    </div>
  )
}
