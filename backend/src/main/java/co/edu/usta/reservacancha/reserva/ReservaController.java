package co.edu.usta.reservacancha.reserva;

import co.edu.usta.reservacancha.auth.AuthController.AuthResponse;
import co.edu.usta.reservacancha.security.JwtService;
import co.edu.usta.reservacancha.user.Clientes;
import co.edu.usta.reservacancha.user.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

  private final ReservaService service;
  private final ReservaRepository repo;
  private final Clientes clientes;
  private final JwtService jwt;
  private final String whatsapp;

  public ReservaController(
      ReservaService service,
      ReservaRepository repo,
      Clientes clientes,
      JwtService jwt,
      @Value("${app.whatsapp.numero:}") String whatsapp) {
    this.service = service;
    this.repo = repo;
    this.clientes = clientes;
    this.jwt = jwt;
    this.whatsapp = whatsapp;
  }

  @PostMapping
  public Reserva crear(
      @AuthenticationPrincipal Usuario usuario, @Valid @RequestBody CrearReserva req) {
    return service.crear(usuario, req.canchaId(), req.fecha(), req.horaInicio(), req.horaFin());
  }

  /**
   * Reserva sin cuenta (HU-13): con nombre y celular. Se crea, o se reutiliza, el jugador de ese
   * celular y, además de la reserva, se devuelve su sesión para ver «Mis reservas», cancelar y
   * pagar, y el WhatsApp del complejo para el botón de confirmar (vacío si no está configurado).
   */
  @PostMapping("/rapida")
  public ReservaRapidaVista rapida(@Valid @RequestBody ReservaRapida req) {
    Usuario u = clientes.porCelular(req.nombre(), req.celular());
    Reserva r = service.crear(u, req.canchaId(), req.fecha(), req.horaInicio(), req.horaFin());
    AuthResponse sesion =
        new AuthResponse(
            jwt.generate(u), u.getEmail(), u.getRol().name(), u.getId(), u.getNombre());
    return new ReservaRapidaVista(r, sesion, whatsapp.isBlank() ? null : whatsapp);
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
        .map(
            r ->
                Map.<String, Object>of(
                    "inicio", r.getHoraInicio().toString(),
                    "fin", r.getHoraFin().toString(),
                    "estado", r.getEstado().name()))
        .toList();
  }

  /**
   * Ocupacion de una cancha desde hoy. Los jugadores solo ven los bloques ocupados; el personal
   * (administrador, propietario y empleado) ve ademas quien reservo y los ultimos 30 dias.
   */
  @GetMapping("/cancha/{canchaId}")
  public List<Map<String, Object>> porCancha(
      @PathVariable Long canchaId, @AuthenticationPrincipal Usuario usuario) {
    boolean gestor = usuario.getRol().esPersonal();
    LocalDate desde = gestor ? LocalDate.now().minusDays(30) : LocalDate.now();
    return repo
        .findByCanchaIdAndFechaGreaterThanEqualOrderByFechaAscHoraInicioAsc(canchaId, desde)
        .stream()
        .map(
            r -> {
              Map<String, Object> m = new LinkedHashMap<>();
              m.put("id", r.getId());
              m.put("canchaId", canchaId);
              m.put("fecha", r.getFecha().toString());
              m.put("horaInicio", r.getHoraInicio().toString());
              m.put("horaFin", r.getHoraFin().toString());
              m.put("estado", r.getEstado().name());
              m.put("usuarioId", r.getUsuario().getId());
              if (gestor) {
                m.put("jugador", r.getUsuario().getNombre());
                // HU-13: el personal ve el celular de quien reservó sin cuenta
                m.put("celular", r.getUsuario().getCelular());
              }
              return m;
            })
        .toList();
  }

  public record CrearReserva(
      @NotNull Long canchaId,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin) {}

  public record ReservaRapida(
      @NotBlank @Size(max = 60) String nombre,
      @NotBlank @Pattern(regexp = "3\\d{9}", message = "debe tener 10 dígitos y empezar por 3")
          String celular,
      @NotNull Long canchaId,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
      @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin) {}

  public record ReservaRapidaVista(Reserva reserva, AuthResponse sesion, String whatsapp) {}
}
