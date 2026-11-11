'use client'

import { useEffect, useMemo, useState } from 'react'
import Link from 'next/link'
import { useAuth } from '@/lib/auth/context'
import { homeFor } from '@/lib/auth/routes'
import { ApiError } from '@/lib/api/client'
import {
  abonoDe,
  getCanchas,
  getDisponibilidad,
  reservarRapido,
  type CanchaPublica,
  type Disponibilidad,
  type ReservaLista,
} from '@/lib/api/reservaRapida'
import WompiCheckoutModal from '@/components/app/pagos/WompiCheckoutModal'

// ─── Canchas Mónaco (cliente del proyecto) ──────────────────────────────────
const COMPLEJO = {
  nombre: 'Canchas Mónaco',
  lugar: 'Villavicencio, Meta',
  horario: 'todos los días de 2:00 p. m. a 10:00 p. m.',
  mapa: 'https://www.google.com/maps/search/?api=1&query=Canchas+Monaco+Villavicencio',
  instagram: 'https://www.instagram.com/monacovillavicencio/',
}
const DIAS_A_MOSTRAR = 14
const CELULAR = /^3\d{9}$/
const RECORDAR = 'rc-jugador'

// ─── formato ────────────────────────────────────────────────────────────────
const pesos = (n: number) => '$' + Math.round(n).toLocaleString('es-CO')

function isoLocal(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function hora12(h: number) {
  const sufijo = h >= 12 ? 'p. m.' : 'a. m.'
  const h12 = h % 12 === 0 ? 12 : h % 12
  return `${h12}:00 ${sufijo}`
}

function rango(h: number, horas: number) {
  return `${hora12(h).replace(/ [ap]\. m\.$/, '')} a ${hora12(h + horas)}`
}

function fechaCorta(iso: string) {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(y, m - 1, d)
    .toLocaleDateString('es-CO', { weekday: 'short', day: 'numeric', month: 'short' })
    .replace(/\./g, '')
}

const horaDe = (t: string) => parseInt(t.slice(0, 2), 10)

function deporte(c: CanchaPublica) {
  const t = (c.tipo ?? '').toLowerCase()
  if (t.includes('vóley') || t.includes('voley') || t.includes('voleibol')) return 'Vóley'
  if (t.includes('fútbol') || t.includes('futbol')) return 'Fútbol'
  return c.tipo ?? 'Cancha'
}

/** «Cancha 2 — Fútbol 7» → «Cancha 2». */
const numeroDe = (c: CanchaPublica) => c.nombre.split('—')[0].trim()

interface Dia {
  iso: string
  arriba: string
  numero: number
  mes: string
}

function proximosDias(): Dia[] {
  const hoy = new Date()
  return Array.from({ length: DIAS_A_MOSTRAR }, (_, i) => {
    const d = new Date(hoy.getFullYear(), hoy.getMonth(), hoy.getDate() + i)
    const semana = d.toLocaleDateString('es-CO', { weekday: 'short' }).replace('.', '')
    return {
      iso: isoLocal(d),
      arriba: i === 0 ? 'Hoy' : i === 1 ? 'Mañana' : semana.charAt(0).toUpperCase() + semana.slice(1),
      numero: d.getDate(),
      mes: d.toLocaleDateString('es-CO', { month: 'short' }).replace('.', ''),
    }
  })
}

/** Horas del día (las libres y las ocupadas) y cuáles se pueden tomar para 1 o 2 horas. */
function horasDelDia(disp: Disponibilidad, horas: number, esHoy: boolean) {
  const libres = new Set(disp.libres.map((b) => horaDe(b.inicio)))
  const todas = new Set(libres)
  disp.ocupadas.forEach((b) => {
    for (let h = horaDe(b.inicio); h < horaDe(b.fin); h++) todas.add(h)
  })
  const ahora = new Date().getHours()
  return [...todas]
    .sort((a, b) => a - b)
    .map((h) => {
      const pasada = esHoy && h <= ahora
      let libre = !pasada
      for (let k = 0; k < horas && libre; k++) libre = libres.has(h + k)
      return { h, libre, ocupada: !libres.has(h) }
    })
}

function mensajeDeError(e: unknown) {
  if (e instanceof ApiError) {
    if (e.status === 409) return 'Esa hora la acaba de tomar otra persona. Elige otra.'
    return e.message
  }
  return 'No pudimos conectar con el servidor. Intenta de nuevo en un momento.'
}

// ─── piezas ─────────────────────────────────────────────────────────────────
function Paso({ n, titulo, children }: { n: number; titulo: string; children: React.ReactNode }) {
  return (
    <section className="flex flex-col gap-3">
      <h2 className="flex items-center gap-3 text-[17px] font-bold" style={{ color: 'var(--text)' }}>
        <span
          className="inline-flex h-7 w-7 items-center justify-center rounded-full text-[13px] font-bold"
          style={{ background: 'var(--green)', color: '#fff' }}
        >
          {n}
        </span>
        {titulo}
      </h2>
      {children}
    </section>
  )
}

function Chip({
  activo,
  deshabilitado,
  onClick,
  children,
  tachado,
  ancho,
}: {
  activo: boolean
  deshabilitado?: boolean
  onClick: () => void
  children: React.ReactNode
  tachado?: boolean
  ancho?: string
}) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={deshabilitado}
      aria-pressed={activo}
      className={`shrink-0 rounded-xl px-3 py-2.5 text-sm font-semibold transition-colors disabled:cursor-not-allowed ${ancho ?? ''}`}
      style={{
        background: activo ? 'var(--green)' : 'var(--panel)',
        color: activo ? '#fff' : deshabilitado ? 'var(--text-3)' : 'var(--text)',
        border: `1px solid ${activo ? 'var(--green)' : 'var(--line-2)'}`,
        textDecoration: tachado ? 'line-through' : 'none',
        opacity: deshabilitado ? 0.55 : 1,
      }}
    >
      {children}
    </button>
  )
}

// ─── pantalla ───────────────────────────────────────────────────────────────
export default function ReservaRapida() {
  const { user, startSession, logout } = useAuth()
  const dias = useMemo(() => proximosDias(), [])

  const [canchas, setCanchas] = useState<CanchaPublica[] | null>(null)
  const [errorCarga, setErrorCarga] = useState<string | null>(null)
  const [intentoCanchas, setIntentoCanchas] = useState(0)
  const [canchaId, setCanchaId] = useState<number | null>(null)
  const [fecha, setFecha] = useState(dias[0].iso)
  const [horas, setHoras] = useState(1)
  const [hora, setHora] = useState<number | null>(null)
  const [disp, setDisp] = useState<Disponibilidad | null>(null)
  const [errorHoras, setErrorHoras] = useState(false)
  const [recargaHoras, setRecargaHoras] = useState(0)
  const [nombre, setNombre] = useState('')
  const [celular, setCelular] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [lista, setLista] = useState<ReservaLista | null>(null)
  const [pagando, setPagando] = useState(false)
  const [abonoPagado, setAbonoPagado] = useState(false)

  useEffect(() => {
    getCanchas()
      .then((cs) => {
        const ordenadas = [...cs].sort((a, b) => a.id - b.id)
        setErrorCarga(null)
        setCanchas(ordenadas)
        setCanchaId((actual) => actual ?? ordenadas[0]?.id ?? null)
      })
      .catch(() =>
        setErrorCarga(
          'El servidor está despertando (puede tardar hasta un minuto la primera vez). Intenta de nuevo.'
        )
      )
  }, [intentoCanchas])

  // El celular recuerda nombre y número para la próxima reserva. Se lee después de montar la
  // página para que el HTML del servidor y el del navegador coincidan.
  useEffect(() => {
    try {
      const guardado = JSON.parse(localStorage.getItem(RECORDAR) ?? 'null')
      // eslint-disable-next-line react-hooks/set-state-in-effect -- dato del navegador, solo existe al montar
      if (guardado?.nombre) setNombre(guardado.nombre)
      if (guardado?.celular) setCelular(guardado.celular)
    } catch {
      // sin datos guardados
    }
  }, [])

  // Horas libres y ocupadas de la cancha y el día elegidos
  useEffect(() => {
    if (canchaId == null) return
    let vigente = true
    getDisponibilidad(canchaId, fecha)
      .then((d) => {
        if (!vigente) return
        setErrorHoras(false)
        setDisp(d)
      })
      .catch(() => {
        if (!vigente) return
        setErrorHoras(true)
        setDisp({ canchaId, fecha, libres: [], ocupadas: [] })
      })
    return () => {
      vigente = false
    }
  }, [canchaId, fecha, recargaHoras])

  const cargandoHoras = canchaId != null && (disp?.canchaId !== canchaId || disp?.fecha !== fecha)

  function elegirCancha(id: number) {
    setCanchaId(id)
    setHora(null)
  }

  function elegirFecha(iso: string) {
    setFecha(iso)
    setHora(null)
  }

  function cargarHoras() {
    setRecargaHoras((n) => n + 1)
  }

  function reintentarCanchas() {
    setErrorCarga(null)
    setIntentoCanchas((n) => n + 1)
  }

  const cancha = canchas?.find((c) => c.id === canchaId) ?? null
  const bloques = useMemo(
    () => (disp ? horasDelDia(disp, horas, fecha === dias[0].iso) : []),
    [disp, horas, fecha, dias]
  )
  const total = cancha ? Number(cancha.precioHora) * horas : 0
  const porcentaje = cancha ? abonoDe(cancha) : 0
  const abono = Math.round((total * porcentaje) / 100)
  const celularValido = CELULAR.test(celular)
  const listoParaReservar = cancha != null && hora != null && nombre.trim() !== '' && celularValido

  function elegirHoras(n: number) {
    setHoras(n)
    setHora(null)
  }

  async function reservar() {
    if (!cancha || hora == null) return
    setEnviando(true)
    setError(null)
    try {
      const r = await reservarRapido({
        nombre: nombre.trim(),
        celular,
        canchaId: cancha.id,
        fecha,
        horaInicio: `${String(hora).padStart(2, '0')}:00:00`,
        horaFin: `${String(hora + horas).padStart(2, '0')}:00:00`,
      })
      try {
        localStorage.setItem(RECORDAR, JSON.stringify({ nombre: nombre.trim(), celular }))
      } catch {
        // almacenamiento no disponible
      }
      startSession(r.sesion)
      setAbonoPagado(false)
      setLista(r)
      window.scrollTo({ top: 0 })
    } catch (e) {
      setError(mensajeDeError(e))
      if (e instanceof ApiError && e.status === 409) {
        setHora(null)
        cargarHoras()
      }
    } finally {
      setEnviando(false)
    }
  }

  function otraReserva() {
    setLista(null)
    setHora(null)
    cargarHoras()
  }

  const encabezado = (
    <header
      className="sticky top-0 z-20 flex items-center justify-between px-4 py-3"
      style={{ background: 'var(--sidebar-bg)', borderBottom: '1px solid var(--line)' }}
    >
      <Link href="/reservar" className="text-[20px] font-extrabold tracking-[-0.02em]">
        <span style={{ color: 'var(--text)' }}>reserva</span>
        <span style={{ color: 'var(--green)' }}>cancha</span>
      </Link>
      <nav className="flex items-center gap-4 text-sm font-semibold">
        {user?.role === 'JUGADOR' ? (
          <>
            <Link href="/reservas" style={{ color: 'var(--text)' }}>
              Mis reservas
            </Link>
            <button type="button" onClick={logout} className="underline" style={{ color: 'var(--text-2)' }}>
              Salir
            </button>
          </>
        ) : user ? (
          <Link href={homeFor(user.role)} className="underline" style={{ color: 'var(--text-2)' }}>
            Panel
          </Link>
        ) : (
          <Link href="/login" className="underline" style={{ color: 'var(--text-2)' }}>
            Personal
          </Link>
        )}
      </nav>
    </header>
  )

  // ─── confirmación ─────────────────────────────────────────────────────────
  if (lista) {
    const r = lista.reserva
    const p = abonoDe(r.cancha)
    const valor = Number(r.valorTotal)
    const abonoReserva = Math.round((valor * p) / 100)
    const h = horaDe(r.horaInicio)
    const duracion = horaDe(r.horaFin) - h
    const mensaje =
      `Hola, ${COMPLEJO.nombre}. Soy ${r.usuario.nombre}. Reservé la ${r.cancha.nombre} ` +
      `el ${fechaCorta(r.fecha)}, de ${rango(h, duracion)} (reserva #${r.id}).`
    return (
      <div className="min-h-full" style={{ background: 'var(--bg)' }}>
        {encabezado}
        <main className="mx-auto flex max-w-xl flex-col gap-4 px-4 py-6">
          <div
            className="flex flex-col items-center gap-4 rounded-2xl p-6 text-center"
            style={{ background: 'var(--panel)', border: '1px solid var(--line-2)' }}
          >
            <div
              className="flex h-16 w-16 items-center justify-center rounded-full text-3xl"
              style={{ background: 'var(--green)', color: '#fff' }}
              aria-hidden
            >
              ✓
            </div>
            <div>
              <h1 className="text-2xl font-extrabold" style={{ color: 'var(--text)' }}>
                ¡Reserva lista!
              </h1>
              <p className="mt-1 text-sm" style={{ color: 'var(--text-2)' }}>
                Reserva #{r.id} a nombre de {r.usuario.nombre}
              </p>
            </div>
            <dl className="grid w-full grid-cols-[auto_1fr] gap-x-6 gap-y-2 text-left text-[15px]">
              <dt style={{ color: 'var(--text-3)' }}>Cancha</dt>
              <dd className="font-semibold" style={{ color: 'var(--text)' }}>
                {r.cancha.nombre}
              </dd>
              <dt style={{ color: 'var(--text-3)' }}>Día</dt>
              <dd className="font-semibold" style={{ color: 'var(--text)' }}>
                {fechaCorta(r.fecha)}
              </dd>
              <dt style={{ color: 'var(--text-3)' }}>Hora</dt>
              <dd className="font-semibold" style={{ color: 'var(--text)' }}>
                {rango(h, duracion)}
              </dd>
              <dt style={{ color: 'var(--text-3)' }}>Valor</dt>
              <dd className="font-semibold" style={{ color: 'var(--text)' }}>
                {pesos(valor)}
              </dd>
            </dl>

            {abonoPagado ? (
              <p
                className="w-full rounded-xl px-4 py-3 text-sm font-semibold"
                style={{ background: 'var(--green-soft)', color: 'var(--green)' }}
              >
                Abono pagado.{' '}
                {p < 100 ? `El resto (${pesos(valor - abonoReserva)}) lo pagas en la cancha.` : ''}
              </p>
            ) : p > 0 ? (
              <p
                className="w-full rounded-xl px-4 py-3 text-sm"
                style={{ background: 'var(--amber-soft)', color: 'var(--amber)' }}
              >
                {p < 100
                  ? `Esta cancha pide abono del ${p} %: paga ${pesos(abonoReserva)} ahora y el resto en la cancha.`
                  : `Paga ${pesos(abonoReserva)} en línea para confirmar tu reserva.`}
              </p>
            ) : (
              <p
                className="w-full rounded-xl px-4 py-3 text-sm"
                style={{ background: 'var(--green-soft)', color: 'var(--green)' }}
              >
                Esta cancha no pide abono: pagas todo en la cancha.
              </p>
            )}

            {p > 0 && !abonoPagado && (
              <button
                type="button"
                onClick={() => setPagando(true)}
                className="w-full rounded-xl py-3.5 text-base font-bold"
                style={{ background: 'var(--green)', color: '#fff' }}
              >
                {p < 100 ? `Pagar abono ${pesos(abonoReserva)}` : `Pagar ${pesos(abonoReserva)}`}
              </button>
            )}
            {lista.whatsapp && (
              <a
                href={`https://wa.me/${lista.whatsapp}?text=${encodeURIComponent(mensaje)}`}
                target="_blank"
                rel="noopener noreferrer"
                className="block w-full rounded-xl py-3.5 text-center text-base font-bold"
                style={{ background: '#25d366', color: '#0b2a17' }}
              >
                Confirmar por WhatsApp
              </a>
            )}
            <Link
              href="/reservas"
              className="block w-full rounded-xl py-3.5 text-center text-base font-semibold"
              style={{ border: '1px solid var(--line-2)', color: 'var(--text)' }}
            >
              Ver mis reservas
            </Link>
            <button
              type="button"
              onClick={otraReserva}
              className="text-sm font-semibold underline"
              style={{ color: 'var(--text-2)' }}
            >
              Hacer otra reserva
            </button>
          </div>
        </main>
        <WompiCheckoutModal
          open={pagando}
          bookingId={r.id}
          bookingCode={`RC-${r.id}`}
          onClose={() => setPagando(false)}
          onPaymentDone={(pago) => {
            if (pago.status === 'APPROVED') setAbonoPagado(true)
          }}
        />
      </div>
    )
  }

  // ─── reservar en una pantalla ───────────────────────────────────────────────
  const tipos = canchas ? [...new Set(canchas.map((c) => c.tipo).filter(Boolean))].join(' · ') : ''

  return (
    <div className="min-h-full pb-40" style={{ background: 'var(--bg)' }}>
      {encabezado}

      {/* Portada del complejo */}
      <section
        className="px-4 py-6"
        style={{
          background: 'linear-gradient(135deg, #0a1623 0%, #12302a 100%)',
          borderBottom: '1px solid var(--line)',
        }}
      >
        <div className="mx-auto flex max-w-xl items-start gap-4">
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img
            src="/img/monaco.png"
            alt={`Logo de ${COMPLEJO.nombre}`}
            width={88}
            height={88}
            className="h-[88px] w-[88px] shrink-0 rounded-2xl object-cover"
            style={{ border: '2px solid var(--green-line)' }}
          />
          <div className="flex flex-col gap-1.5">
            <p
              className="text-[12px] font-bold uppercase tracking-[0.14em]"
              style={{ color: 'var(--green)' }}
            >
              Reserva en línea
            </p>
            <h1 className="text-[26px] font-extrabold leading-tight" style={{ color: 'var(--text)' }}>
              {COMPLEJO.nombre}
            </h1>
            {tipos && (
              <p className="text-[15px] font-semibold" style={{ color: 'var(--text)' }}>
                {tipos}
              </p>
            )}
            <p className="text-sm" style={{ color: 'var(--text-2)' }}>
              {COMPLEJO.lugar} · {COMPLEJO.horario}
            </p>
            <div className="mt-2 flex flex-wrap gap-2">
              {[
                { href: COMPLEJO.mapa, texto: 'Cómo llegar' },
                { href: COMPLEJO.instagram, texto: 'Instagram' },
              ].map((l) => (
                <a
                  key={l.texto}
                  href={l.href}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="rounded-full px-4 py-1.5 text-sm font-semibold"
                  style={{ border: '1px solid var(--line-2)', color: 'var(--text)' }}
                >
                  {l.texto}
                </a>
              ))}
            </div>
          </div>
        </div>
      </section>

      <main className="mx-auto flex max-w-xl flex-col gap-7 px-4 py-6">
        <div>
          <h2 className="text-[26px] font-extrabold" style={{ color: 'var(--text)' }}>
            Reserva tu cancha
          </h2>
          <p className="mt-1 text-[15px]" style={{ color: 'var(--text-2)' }}>
            Sin crear cuenta: solo tu nombre y tu celular.
          </p>
        </div>

        {errorCarga && (
          <div
            className="flex flex-col gap-3 rounded-xl px-4 py-3 text-sm"
            style={{ background: 'var(--amber-soft)', color: 'var(--amber)' }}
          >
            {errorCarga}
            <button type="button" onClick={reintentarCanchas} className="w-fit font-bold underline">
              Reintentar
            </button>
          </div>
        )}

        <Paso n={1} titulo="Cancha">
          <div className="-mx-4 flex snap-x gap-3 overflow-x-auto px-4 pb-1">
            {canchas === null && !errorCarga
              ? Array.from({ length: 3 }).map((_, i) => (
                  <div
                    key={i}
                    className="h-[150px] w-[220px] shrink-0 animate-pulse rounded-2xl"
                    style={{ background: 'var(--panel)' }}
                  />
                ))
              : canchas?.map((c) => {
                  const activa = c.id === canchaId
                  const p = abonoDe(c)
                  return (
                    <button
                      key={c.id}
                      type="button"
                      onClick={() => elegirCancha(c.id)}
                      aria-pressed={activa}
                      className="flex w-[220px] shrink-0 snap-start flex-col gap-1 rounded-2xl p-4 text-left"
                      style={{
                        background: 'var(--panel)',
                        border: `2px solid ${activa ? 'var(--green)' : 'var(--line-2)'}`,
                      }}
                    >
                      <span
                        className="text-[12px] font-bold uppercase tracking-[0.1em]"
                        style={{ color: 'var(--text-3)' }}
                      >
                        {deporte(c)}
                      </span>
                      <span className="text-[19px] font-extrabold" style={{ color: 'var(--text)' }}>
                        {c.tipo ?? c.nombre}
                      </span>
                      <span className="text-[13px]" style={{ color: 'var(--text-2)' }}>
                        {numeroDe(c)}
                        {c.superficie ? ` · ${c.superficie}` : ''}
                      </span>
                      <span className="mt-1 text-[19px] font-extrabold" style={{ color: 'var(--text)' }}>
                        {pesos(Number(c.precioHora))}
                        <span className="text-[13px] font-normal" style={{ color: 'var(--text-3)' }}>
                          {' '}
                          /hora
                        </span>
                      </span>
                      <span
                        className="mt-1 w-fit rounded-full px-2.5 py-0.5 text-[12px] font-bold"
                        style={
                          p === 0
                            ? { background: 'var(--green-soft)', color: 'var(--green)' }
                            : { background: 'var(--amber-soft)', color: 'var(--amber)' }
                        }
                      >
                        {p === 0 ? 'Sin abono' : p < 100 ? `Abono ${p} %` : 'Pago en línea'}
                      </span>
                    </button>
                  )
                })}
          </div>
        </Paso>

        <Paso n={2} titulo="Día">
          <div className="-mx-4 flex snap-x gap-2 overflow-x-auto px-4 pb-1">
            {dias.map((d) => (
              <Chip key={d.iso} activo={fecha === d.iso} onClick={() => elegirFecha(d.iso)} ancho="w-[72px]">
                <span className="block text-[12px] font-medium">{d.arriba}</span>
                <span className="block text-[20px] font-extrabold leading-tight">{d.numero}</span>
                <span className="block text-[12px] font-medium">{d.mes}</span>
              </Chip>
            ))}
          </div>
        </Paso>

        <Paso n={3} titulo="Hora">
          <div className="flex gap-2">
            {[1, 2].map((n) => (
              <Chip key={n} activo={horas === n} onClick={() => elegirHoras(n)}>
                {n === 1 ? '1 hora' : '2 horas'}
              </Chip>
            ))}
          </div>
          {cargandoHoras ? (
            <p className="text-sm" style={{ color: 'var(--text-3)' }}>
              Buscando horas libres…
            </p>
          ) : errorHoras ? (
            <p className="text-sm" style={{ color: 'var(--amber)' }}>
              No pudimos cargar las horas.{' '}
              <button type="button" onClick={cargarHoras} className="font-bold underline">
                Reintentar
              </button>
            </p>
          ) : bloques.length === 0 ? (
            <p className="text-sm" style={{ color: 'var(--text-3)' }}>
              {canchaId == null ? 'Elige una cancha.' : 'Ese día la cancha no abre.'}
            </p>
          ) : (
            <div className="grid grid-cols-3 gap-2 sm:grid-cols-4">
              {bloques.map((b) => (
                <Chip
                  key={b.h}
                  activo={hora === b.h}
                  deshabilitado={!b.libre}
                  tachado={b.ocupada}
                  onClick={() => setHora(b.h)}
                >
                  {hora12(b.h)}
                </Chip>
              ))}
            </div>
          )}
          {!cargandoHoras && bloques.some((b) => b.ocupada) && (
            <p className="text-[12px]" style={{ color: 'var(--text-3)' }}>
              Las horas tachadas ya están reservadas.
            </p>
          )}
        </Paso>

        <Paso n={4} titulo="Tus datos">
          <label className="flex flex-col gap-1.5 text-sm font-medium" style={{ color: 'var(--text-2)' }}>
            Nombre
            <input
              value={nombre}
              onChange={(e) => setNombre(e.target.value)}
              maxLength={60}
              autoComplete="name"
              placeholder="Tu nombre"
              className="rounded-xl px-3.5 py-3 text-base outline-none"
              style={{ background: 'var(--panel)', border: '1px solid var(--line-2)', color: 'var(--text)' }}
            />
          </label>
          <label className="flex flex-col gap-1.5 text-sm font-medium" style={{ color: 'var(--text-2)' }}>
            Celular
            <input
              value={celular}
              onChange={(e) => setCelular(e.target.value.replace(/\D/g, '').slice(0, 10))}
              inputMode="numeric"
              autoComplete="tel-national"
              placeholder="3001234567"
              className="rounded-xl px-3.5 py-3 text-base outline-none"
              style={{
                background: 'var(--panel)',
                border: `1px solid ${celular && !celularValido ? 'var(--red)' : 'var(--line-2)'}`,
                color: 'var(--text)',
              }}
            />
            {celular && !celularValido && (
              <span className="text-[12px]" style={{ color: 'var(--red)' }}>
                Debe tener 10 dígitos y empezar por 3.
              </span>
            )}
          </label>
          <p className="text-[12px]" style={{ color: 'var(--text-3)' }}>
            Con este celular ves y cancelas tus reservas desde este teléfono.
          </p>
        </Paso>

        {error && (
          <p
            role="alert"
            className="rounded-xl px-4 py-3 text-sm"
            style={{ background: 'var(--red-soft)', color: 'var(--red)' }}
          >
            {error}
          </p>
        )}
      </main>

      {/* Barra fija con el resumen y el botón */}
      <div
        className="fixed inset-x-0 bottom-0 z-30 px-4 pb-4 pt-3"
        style={{ background: 'var(--sidebar-bg)', borderTop: '1px solid var(--line-2)' }}
      >
        <div className="mx-auto flex max-w-xl flex-col gap-2">
          <div className="flex items-baseline justify-between gap-3 text-sm">
            <span className="font-semibold" style={{ color: 'var(--text)' }}>
              {cancha && hora != null
                ? `${cancha.tipo ?? cancha.nombre} · ${fechaCorta(fecha)} · ${rango(hora, horas)}`
                : 'Elige la hora'}
            </span>
            {cancha && hora != null && (
              <span className="shrink-0 font-extrabold" style={{ color: 'var(--text)' }}>
                {pesos(total)}
              </span>
            )}
          </div>
          {cancha && hora != null && (
            <span
              className="text-[12px]"
              style={{ color: porcentaje === 0 ? 'var(--green)' : 'var(--amber)' }}
            >
              {porcentaje === 0
                ? 'Sin abono: pagas todo en la cancha'
                : porcentaje < 100
                  ? `Abono en línea: ${pesos(abono)} · el resto en la cancha`
                  : `Pago en línea: ${pesos(abono)}`}
            </span>
          )}
          <button
            type="button"
            onClick={reservar}
            disabled={!listoParaReservar || enviando}
            className="w-full rounded-xl py-3.5 text-base font-bold transition-opacity disabled:opacity-45"
            style={{ background: 'var(--green)', color: '#fff' }}
          >
            {enviando ? 'Reservando…' : 'Reservar'}
          </button>
        </div>
      </div>
    </div>
  )
}
