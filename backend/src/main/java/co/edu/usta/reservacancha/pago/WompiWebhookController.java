package co.edu.usta.reservacancha.pago;

import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wompi")
public class WompiWebhookController {

  private final WompiService wompi;

  public WompiWebhookController(WompiService wompi) { this.wompi = wompi; }

  @PostMapping("/webhook")
  public Map<String, Object> webhook(@RequestBody Map<String, Object> body) {
    // Formato simplificado sandbox: {referencia, transactionId, status}
    String ref = String.valueOf(body.getOrDefault("referencia", ""));
    String tx = String.valueOf(body.getOrDefault("transactionId", ""));
    String status = String.valueOf(body.getOrDefault("status", "PENDING"));
    Pago p = wompi.webhook(ref, tx, status);
    return Map.of("pagoId", p.getId(), "estado", p.getEstado().name(),
        "reserva", p.getReserva().getId(), "reservaEstado", p.getReserva().getEstado().name());
  }
}
