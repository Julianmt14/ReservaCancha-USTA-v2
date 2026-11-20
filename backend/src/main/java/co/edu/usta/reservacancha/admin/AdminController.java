package co.edu.usta.reservacancha.admin;

import co.edu.usta.reservacancha.pago.PagoRepository;
import co.edu.usta.reservacancha.reserva.ReservaRepository;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN','PROPIETARIO')")
public class AdminController {

  private final ReservaRepository reservas;
  private final PagoRepository pagos;
  private final MetricasService metricas;

  public AdminController(
      ReservaRepository reservas, PagoRepository pagos, MetricasService metricas) {
    this.reservas = reservas;
    this.pagos = pagos;
    this.metricas = metricas;
  }

  @GetMapping("/resumen")
  public Map<String, Object> resumen() {
    return Map.of(
        "totalReservas", reservas.count(),
        "totalPagos", pagos.count());
  }

  /** Agenda: la ven también los empleados (quién reservó, su celular y si pagó el abono). */
  @GetMapping("/reservas")
  @PreAuthorize("hasAnyRole('ADMIN','PROPIETARIO','EMPLEADO')")
  public Object todas() {
    return reservas.findAll();
  }

  @GetMapping("/pagos")
  public Object pagos() {
    return pagos.findAll();
  }

  /**
   * Reservas, ingresos, abonos cobrados, ocupación por cancha y horas más pedidas (máx. 92 días).
   */
  @GetMapping("/metricas")
  public MetricasService.Metricas metricas(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate hasta) {
    LocalDate hoy = LocalDate.now();
    return metricas.calcular(
        desde != null ? desde : hoy.minusDays(6), hasta != null ? hasta : hoy.plusDays(7));
  }
}
