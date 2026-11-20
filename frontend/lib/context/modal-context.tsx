'use client'

import { createContext, useContext, useState, useCallback } from 'react'

interface ModalContextValue {
  openNewReserva: () => void
  isNewReservaOpen: boolean
  closeNewReserva: () => void
  /** Sube cada vez que se crea una reserva desde la barra superior, para que el listado se recargue. */
  reservasVersion: number
  notifyReservaCreada: () => void
}

const ModalContext = createContext<ModalContextValue | null>(null)

export function ModalProvider({ children }: { children: React.ReactNode }) {
  const [isNewReservaOpen, setNewReserva] = useState(false)
  const openNewReserva = useCallback(() => setNewReserva(true), [])
  const closeNewReserva = useCallback(() => setNewReserva(false), [])
  const [reservasVersion, setReservasVersion] = useState(0)
  const notifyReservaCreada = useCallback(() => setReservasVersion((v) => v + 1), [])

  return (
    <ModalContext.Provider
      value={{ openNewReserva, isNewReservaOpen, closeNewReserva, reservasVersion, notifyReservaCreada }}
    >
      {children}
    </ModalContext.Provider>
  )
}

export function useModalContext() {
  const ctx = useContext(ModalContext)
  if (!ctx) throw new Error('useModalContext must be used inside ModalProvider')
  return ctx
}
