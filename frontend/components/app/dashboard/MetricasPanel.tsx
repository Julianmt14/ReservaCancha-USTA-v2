'use client'

import { useEffect, useState } from 'react'
import { ApiError } from '@/lib/api/client'
import { getMetricas, type Metricas } from '@/lib/api/metricas'

function iso(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function masDias(n: number) {
  const d = new Date()
  d.setDate(d.getDate() + n)
  return iso(d)
}

const pesos = (n: number) =>
  new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(n)

function hora12(h: number) {
  const s = h < 12 ? 'a. m.' : 'p. m.'
  return `${h % 12 === 0 ? 12 : h % 12}:00 ${s}`
}

function Barra({
  etiqueta,
  valor,
  max,
  texto,
}: {
  etiqueta: string
  valor: number
  max: number
  texto: string
}) {
  const ancho = max > 0 ? Math.max((valor / max) * 100, valor > 0 ? 2 : 0) : 0
  return (
    <div className="flex items-center gap-3 text-[12.5px]">
      <span className="w-40 shrink-0 truncate" style={{ color: 'var(--text-2)' }} title={etiqueta}>
        {etiqueta}
      </span>
      <div className="h-2.5 flex-1 rounded-full" style={{ background: 'var(--bg-2)' }}>
        <div className="h-full rounded-full" style={{ width: `${ancho}%`, background: 'var(--green)' }} />
      </div>
      <span className="w-24 shrink-0 text-right font-mono text-[12px]" style={{ color: 'var(--text)' }}>
        {texto}
      </span>
    </div>
  )
}

/** HU-15: métricas del administrador en un rango de fechas (reservas, ingresos, abonos y ocupación). */
export default function MetricasPanel() {
  const [desde, setDesde] = useState(masDias(-6))
  const [hasta, setHasta] = useState(masDias(7))
  const [m, setM] = useState<Metricas | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!desde || !hasta) return
    let vigente = true
    getMetricas(desde, hasta)
      .then((r) => {
        if (!vigente) return
        setM(r)
        setError(null)
      })
      .catch((e) => {
        if (vigente) setError(e instanceof ApiError ? e.message : 'No se pudieron cargar las métricas.')
      })
    return () => {
      vigente = false
    }
  }, [desde, hasta])

  const tarjetas = m
    ? [
        { titulo: 'Reservas', valor: String(m.reservas), color: 'var(--text)' },
        { titulo: 'Canceladas', valor: String(m.canceladas), color: 'var(--red)' },
        { titulo: 'Jugadores', valor: String(m.jugadores), color: 'var(--blue)' },
        { titulo: 'Ingresos', valor: pesos(m.ingresos), color: 'var(--text)' },
        { titulo: 'Abonos cobrados', valor: pesos(m.abonosCobrados), color: 'var(--green)' },
      ]
    : []
  const maxHora = m ? Math.max(0, ...m.porHora.map((h) => h.reservas)) : 0

  return (
    <section
      className="flex flex-col gap-5 rounded-[14px] p-5"
      style={{ background: 'var(--panel)', border: '1px solid var(--line)' }}
    >
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h2 className="text-[16px] font-bold" style={{ color: 'var(--text)' }}>
            Métricas
          </h2>
          <p className="text-[12.5px]" style={{ color: 'var(--text-3)' }}>
            Reservas que siguen en pie; los ingresos incluyen lo que falta cobrar en la cancha.
          </p>
        </div>
        <div className="flex items-center gap-2 text-[12px]" style={{ color: 'var(--text-3)' }}>
          <label className="flex items-center gap-1.5">
            Desde
            <input
              type="date"
              value={desde}
              onChange={(e) => setDesde(e.target.value)}
              className="rounded-lg px-2 py-1 outline-none"
              style={{ background: 'var(--bg-2)', border: '1px solid var(--line)', color: 'var(--text-2)' }}
            />
          </label>
          <label className="flex items-center gap-1.5">
            Hasta
            <input
              type="date"
              value={hasta}
              onChange={(e) => setHasta(e.target.value)}
              className="rounded-lg px-2 py-1 outline-none"
              style={{ background: 'var(--bg-2)', border: '1px solid var(--line)', color: 'var(--text-2)' }}
            />
          </label>
        </div>
      </div>

      {error && (
        <p
          className="text-xs rounded-lg px-3 py-2"
          style={{ color: 'var(--red)', background: 'var(--red-soft)' }}
        >
          {error}
        </p>
      )}

      {m && (
        <>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
            {tarjetas.map((t) => (
              <div key={t.titulo} className="rounded-lg px-3 py-2.5" style={{ background: 'var(--bg-2)' }}>
                <div className="text-[10px] uppercase tracking-wide mb-1" style={{ color: 'var(--text-3)' }}>
                  {t.titulo}
                </div>
                <div className="font-mono text-[15px] font-semibold" style={{ color: t.color }}>
                  {t.valor}
                </div>
              </div>
            ))}
          </div>

          <div className="grid gap-6 lg:grid-cols-2">
            <div className="flex flex-col gap-2.5">
              <h3
                className="text-[12px] font-semibold uppercase tracking-wide"
                style={{ color: 'var(--text-3)' }}
              >
                Ocupación por cancha
              </h3>
              {m.porCancha.map((c) => (
                <Barra
                  key={c.canchaId}
                  etiqueta={c.nombre}
                  valor={c.ocupacion}
                  max={100}
                  texto={`${c.ocupacion.toLocaleString('es-CO')} % · ${c.horas.toLocaleString('es-CO')} h`}
                />
              ))}
            </div>
            <div className="flex flex-col gap-2.5">
              <h3
                className="text-[12px] font-semibold uppercase tracking-wide"
                style={{ color: 'var(--text-3)' }}
              >
                Horas más pedidas
              </h3>
              {m.porHora.length === 0 ? (
                <p className="text-xs" style={{ color: 'var(--text-3)' }}>
                  Sin reservas en estas fechas.
                </p>
              ) : (
                m.porHora.map((h) => (
                  <Barra
                    key={h.hora}
                    etiqueta={hora12(h.hora)}
                    valor={h.reservas}
                    max={maxHora}
                    texto={`${h.reservas} ${h.reservas === 1 ? 'reserva' : 'reservas'}`}
                  />
                ))
              )}
            </div>
          </div>
        </>
      )}
    </section>
  )
}
