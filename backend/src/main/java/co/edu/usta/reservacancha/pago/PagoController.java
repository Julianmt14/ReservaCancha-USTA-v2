package co.edu.usta.reservacancha.pago;

import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

  private final WompiService wompi;
  private final PagoRepository pagos;

  public PagoController(WompiService wompi, PagoRepository pagos) {
    this.wompi = wompi;
    this.pagos = pagos;
  }

  @PostMapping("/iniciar/{reservaId}")
  public Map<String, Object> iniciar(@PathVariable Long reservaId) {
    return wompi.iniciarPago(reservaId);
  }

  @GetMapping("/reserva/{reservaId}")
  public java.util.List<Pago> porReserva(@PathVariable Long reservaId) {
    return pagos.findByReservaId(reservaId);
  }
}
