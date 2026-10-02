package co.edu.usta.reservacancha.reserva;

import co.edu.usta.reservacancha.user.Usuario;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

  private final ReservaService service;
  private final ReservaRepository repo;

  public ReservaController(ReservaService service, ReservaRepository repo) {
    this.service = service;
    this.repo = repo;
  }

  @PostMapping
  public Reserva crear(@AuthenticationPrincipal Usuario usuario, @RequestBody CrearReserva req) {
    return service.crear(usuario, req.canchaId(), req.fecha(), req.horaInicio(), req.horaFin());
  }

  @GetMapping("/mias")
  public List<Reserva> mias(@AuthenticationPrincipal Usuario usuario) {
    return repo.findByUsuarioId(usuario.getId());
  }

  @PostMapping("/{id}/cancelar")
  public Reserva cancelar(@PathVariable Long id, @AuthenticationPrincipal Usuario usuario) {
    return service.cancelar(id, usuario);
  }

  @GetMapping("/por-cancha-fecha")
  public List<Map<String, Object>> porCanchaFecha(
      @RequestParam Long canchaId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
    return repo.findByCanchaIdAndFecha(canchaId, fecha).stream()
        .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
        .map(r -> Map.<String, Object>of(
            "inicio", r.getHoraInicio().toString(),
            "fin", r.getHoraFin().toString(),
            "estado", r.getEstado().name()))
        .toList();
  }

  public record CrearReserva(
      @NotNull Long canchaId,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin) {}
}
