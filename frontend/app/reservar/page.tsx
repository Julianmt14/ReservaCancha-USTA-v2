import type { Metadata } from 'next'
import ReservaRapida from '@/components/app/reservar/ReservaRapida'

export const metadata: Metadata = {
  title: 'Reservar — Canchas Mónaco',
  description: 'Reserva tu cancha en Canchas Mónaco sin crear cuenta: solo tu nombre y tu celular.',
}

export default function ReservarPage() {
  return <ReservaRapida />
}
