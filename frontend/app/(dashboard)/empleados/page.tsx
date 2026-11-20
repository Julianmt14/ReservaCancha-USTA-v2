import EmpleadosView from '@/components/app/empleados/EmpleadosView'

export const metadata = { title: 'Empleados · ReservaCancha' }

export default function EmpleadosPage() {
  return (
    <div className="flex flex-col gap-6 p-6">
      <div>
        <h1 className="text-xl font-bold" style={{ color: 'var(--text)', fontFamily: 'var(--font-sans)' }}>
          Empleados
        </h1>
        <p className="text-sm mt-0.5" style={{ color: 'var(--text-3)' }}>
          Cuentas del personal. Un empleado ve la agenda, reserva por teléfono y cancela; nada más.
        </p>
      </div>

      <EmpleadosView />
    </div>
  )
}
