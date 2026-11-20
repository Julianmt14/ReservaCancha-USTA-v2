'use client'

import { useState, useEffect } from 'react'
import Modal from '@/components/app/ui/Modal'
import { FormField, Input, Select, SubmitRow } from '@/components/app/ui/FormField'
import { getCourts, type CourtResponse } from '@/lib/api/courts'
import { createPhoneBooking, type BookingResponse } from '@/lib/api/bookings'
import { ApiError } from '@/lib/api/client'

interface Props {
  open: boolean
  onClose: () => void
  onSaved: (booking: BookingResponse) => void
  preselectedCourtId?: number
  preselectedDate?: string
  preselectedStart?: string
  preselectedEnd?: string
}

export default function CreateReservaModal({
  open,
  onClose,
  onSaved,
  preselectedCourtId,
  preselectedDate,
  preselectedStart,
  preselectedEnd,
}: Props) {
  const [courts, setCourts] = useState<CourtResponse[]>([])
  const [courtId, setCourtId] = useState('')
  const [date, setDate] = useState('')
  const [startTime, setStart] = useState('')
  const [endTime, setEnd] = useState('')
  const [playerName, setPlayerName] = useState('')
  const [playerPhone, setPlayerPhone] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!open) return
    setError(null)
    setPlayerName('')
    setPlayerPhone('')
    getCourts(0, 50)
      .then((p) => {
        const active = p.content.filter((c) => c.active)
        setCourts(active)
        if (preselectedCourtId) {
          setCourtId(String(preselectedCourtId))
        } else if (active.length > 0) {
          setCourtId(String(active[0].id))
        }
      })
      .catch(() => {})

    const now = new Date()
    const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    setDate(preselectedDate || today)
    setStart(preselectedStart || '14:00')
    setEnd(preselectedEnd || '15:00')
  }, [open, preselectedCourtId])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!courtId) {
      setError('Selecciona una cancha.')
      return
    }
    if (!date) {
      setError('Selecciona una fecha.')
      return
    }
    if (startTime >= endTime) {
      setError('La hora de fin debe ser posterior a la de inicio.')
      return
    }
    const celular = playerPhone.replace(/\D/g, '')
    if (!playerName.trim()) {
      setError('Escribe el nombre de quien reserva.')
      return
    }
    if (!/^3\d{9}$/.test(celular)) {
      setError('El celular debe tener 10 dígitos y empezar por 3.')
      return
    }

    const startAt = `${date}T${startTime}:00`
    const endAt = `${date}T${endTime}:00`

    setError(null)
    setLoading(true)
    try {
      const booking = await createPhoneBooking({
        courtId: Number(courtId),
        startAt,
        endAt,
        playerName: playerName.trim(),
        playerPhone: celular,
      })
      onSaved(booking)
      onClose()
    } catch (err) {
      // El backend explica el motivo: hora ocupada, solo 1 o 2 horas, fuera del horario…
      if (err instanceof ApiError && (err.status === 409 || err.status === 400)) setError(err.message)
      else setError('No se pudo crear la reserva. Intenta de nuevo.')
    } finally {
      setLoading(false)
    }
  }

  const now = new Date()
  const today = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`

  return (
    <Modal open={open} onClose={onClose} title="Nueva reserva">
      <form onSubmit={handleSubmit}>
        <div className="px-6 py-5 flex flex-col gap-4">
          <p className="text-xs" style={{ color: 'var(--text-3)' }}>
            Reserva por teléfono: queda a nombre del jugador y él la ve en «Mis reservas» con su celular.
          </p>
          <div className="grid grid-cols-2 gap-4">
            <FormField label="Nombre del jugador">
              <Input
                value={playerName}
                maxLength={60}
                onChange={(e) => setPlayerName(e.target.value)}
                placeholder="Nombre y apellido"
                required
              />
            </FormField>
            <FormField label="Celular">
              <Input
                type="tel"
                inputMode="numeric"
                value={playerPhone}
                onChange={(e) => setPlayerPhone(e.target.value)}
                placeholder="3001234567"
                required
              />
            </FormField>
          </div>
          <FormField label="Cancha">
            <Select value={courtId} onChange={(e) => setCourtId(e.target.value)} required>
              {courts.length === 0 ? (
                <option value="">Cargando canchas…</option>
              ) : (
                courts.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))
              )}
            </Select>
          </FormField>

          <FormField label="Fecha">
            <Input type="date" value={date} min={today} onChange={(e) => setDate(e.target.value)} required />
          </FormField>

          <div className="grid grid-cols-2 gap-4">
            <FormField label="Hora inicio">
              <Input
                type="time"
                value={startTime}
                step="3600"
                onChange={(e) => setStart(e.target.value)}
                required
              />
            </FormField>
            <FormField label="Hora fin">
              <Input
                type="time"
                value={endTime}
                step="3600"
                onChange={(e) => setEnd(e.target.value)}
                required
              />
            </FormField>
          </div>

          {error && (
            <p
              className="text-xs rounded-lg px-3 py-2"
              style={{ color: 'var(--red)', background: 'var(--red-soft)' }}
            >
              {error}
            </p>
          )}
        </div>

        <SubmitRow onCancel={onClose} loading={loading} label="Crear reserva" />
      </form>
    </Modal>
  )
}
