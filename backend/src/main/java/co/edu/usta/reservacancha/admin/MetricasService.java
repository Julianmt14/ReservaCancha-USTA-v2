package co.edu.usta.reservacancha.admin;

import co.edu.usta.reservacancha.cancha.Cancha;
import co.edu.usta.reservacancha.cancha.CanchaRepository;
import co.edu.usta.reservacancha.horario.Horario;
import co.edu.usta.reservacancha.horario.HorarioRepository;
import co.edu.usta.reservacancha.pago.EstadoPago;
import co.edu.usta.reservacancha.pago.PagoRepository;
import co.edu.usta.reservacancha.reserva.EstadoReserva;
import co.edu.usta.reservacancha.reserva.Reserva;
import co.edu.usta.reservacancha.reserva.ReservaRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Métricas del administrador en un rango de fechas: reservas, cancelaciones, ingresos, abonos
 * cobrados con Wompi, ocupación por cancha y reservas por día y por hora.
 */
@Service
public class MetricasService {

  private static final int MAX_DIAS = 92;

  private final ReservaRepository reservas;
  private final CanchaRepository canchas;
  private final HorarioRepository horarios;
  private final PagoRepository pagos;

  public MetricasService(
      ReservaRepository reservas,
      CanchaRepository canchas,
      HorarioRepository horarios,
      PagoRepository pagos) {
    this.reservas = reservas;
    this.canchas = canchas;
    this.horarios = horarios;
    this.pagos = pagos;
  }

  public Metricas calcular(LocalDate desde, LocalDate hasta) {
    if (hasta.isBefore(desde)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "«hasta» debe ser después de «desde»");
    }
    long dias = ChronoUnit.DAYS.between(desde, hasta) + 1;
    if (dias > MAX_DIAS) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El rango máximo es de " + MAX_DIAS + " días");
    }

    List<Reserva> delRango = reservas.findByFechaBetween(desde, hasta);
    List<Reserva> activas =
        delRango.stream().filter(r -> r.getEstado() != EstadoReserva.CANCELADA).toList();
    Set<Long> idsActivas = activas.stream().map(Reserva::getId).collect(Collectors.toSet());

    BigDecimal ingresos =
        activas.stream().map(Reserva::getValorTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    // Lo que ya entró por Wompi (abonos aprobados) de las reservas que siguen en pie
    BigDecimal abonos =
        pagos.findAll().stream()
            .filter(
                p ->
                    p.getEstado() == EstadoPago.APROBADO
                        && idsActivas.contains(p.getReserva().getId()))
            .map(p -> p.getValor())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    // Por cancha: reservas, horas reservadas y ocupación sobre las horas que abre en el rango
    List<PorCancha> porCancha = new ArrayList<>();
    for (Cancha c : canchas.findByActivaTrue()) {
      List<Reserva> deLaCancha =
          activas.stream().filter(r -> r.getCancha().getId().equals(c.getId())).toList();
      long minutos = deLaCancha.stream().mapToLong(MetricasService::minutos).sum();
      long abiertos = minutosAbiertos(horarios.findByCanchaId(c.getId()), desde, dias);
      porCancha.add(
          new PorCancha(
              c.getId(),
              c.getNombre(),
              deLaCancha.size(),
              minutos / 60.0,
              abiertos == 0 ? 0 : Math.round(minutos * 1000.0 / abiertos) / 10.0));
    }

    // Por día (todos los días del rango, aunque no tengan reservas)
    Map<LocalDate, Long> conteoDia =
        activas.stream().collect(Collectors.groupingBy(Reserva::getFecha, Collectors.counting()));
    List<PorDia> porDia = new ArrayList<>();
    for (LocalDate d = desde; !d.isAfter(hasta); d = d.plusDays(1)) {
      porDia.add(new PorDia(d, conteoDia.getOrDefault(d, 0L)));
    }

    // Horas más pedidas: cada hora que ocupa una reserva cuenta una vez
    Map<Integer, Long> conteoHora = new TreeMap<>();
    for (Reserva r : activas) {
      for (int h = r.getHoraInicio().getHour(); h < r.getHoraFin().getHour(); h++) {
        conteoHora.merge(h, 1L, Long::sum);
      }
    }
    List<PorHora> porHora =
        conteoHora.entrySet().stream().map(e -> new PorHora(e.getKey(), e.getValue())).toList();

    long jugadores = activas.stream().map(r -> r.getUsuario().getId()).distinct().count();
    return new Metricas(
        desde,
        hasta,
        activas.size(),
        delRango.size() - activas.size(),
        jugadores,
        ingresos,
        abonos,
        porCancha,
        porDia,
        porHora);
  }

  private static long minutos(Reserva r) {
    return Duration.between(r.getHoraInicio(), r.getHoraFin()).toMinutes();
  }

  /**
   * Minutos que la cancha abre en el rango, según sus horarios activos de cada día de la semana.
   */
  private static long minutosAbiertos(List<Horario> horarios, LocalDate desde, long dias) {
    long total = 0;
    for (int i = 0; i < dias; i++) {
      var dia = desde.plusDays(i).getDayOfWeek();
      for (Horario h : horarios) {
        if (h.isActivo() && h.getDiaSemana() == dia) {
          total += Duration.between(h.getHoraApertura(), h.getHoraCierre()).toMinutes();
        }
      }
    }
    return total;
  }

  public record Metricas(
      LocalDate desde,
      LocalDate hasta,
      long reservas,
      long canceladas,
      long jugadores,
      BigDecimal ingresos,
      BigDecimal abonosCobrados,
      List<PorCancha> porCancha,
      List<PorDia> porDia,
      List<PorHora> porHora) {}

  public record PorCancha(
      Long canchaId, String nombre, long reservas, double horas, double ocupacion) {}

  public record PorDia(LocalDate fecha, long reservas) {}

  public record PorHora(int hora, long reservas) {}
}
