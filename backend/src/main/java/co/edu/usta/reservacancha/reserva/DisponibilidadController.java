package co.edu.usta.reservacancha.reserva;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/disponibilidad")
public class DisponibilidadController {

  private final ReservaRepository reservas;

  public DisponibilidadController(ReservaRepository reservas) { this.reservas = reservas; }

  /** Consulta habitual de disponibilidad (RNF-01: < 2,0 s). Devuelve bloques ocupados + libres de 1h 06:00-22:00. */
  @GetMapping
  public Map<String, Object> disponibilidad(
      @RequestParam Long canchaId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
    var ocupadas = reservas.findByCanchaIdAndFecha(canchaId, fecha).stream()
        .filter(r -> r.getEstado() != EstadoReserva.CANCELADA)
        .map(r -> Map.of("inicio", r.getHoraInicio().toString(), "fin", r.getHoraFin().toString()))
        .toList();

    List<Map<String, String>> libres = new ArrayList<>();
    for (int h = 6; h < 22; h++) {
      LocalTime ini = LocalTime.of(h, 0), fin = LocalTime.of(h + 1, 0);
      boolean choca = reservas.existeTraslape(canchaId, fecha, ini, fin);
      if (!choca) libres.add(Map.of("inicio", ini.toString(), "fin", fin.toString()));
    }
    return Map.of("canchaId", canchaId, "fecha", fecha.toString(), "ocupadas", ocupadas, "libres", libres);
  }
}
