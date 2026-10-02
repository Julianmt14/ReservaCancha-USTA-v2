package co.edu.usta.reservacancha.pago;

import co.edu.usta.reservacancha.reserva.EstadoReserva;
import co.edu.usta.reservacancha.reserva.Reserva;
import co.edu.usta.reservacancha.reserva.ReservaRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WompiService {

  private final PagoRepository pagos;
  private final ReservaRepository reservas;
  private final String integritySecret;
  private final String currency;

  public WompiService(PagoRepository pagos, ReservaRepository reservas,
      @Value("${app.wompi.integrity-secret}") String integritySecret,
      @Value("${app.wompi.currency}") String currency) {
    this.pagos = pagos;
    this.reservas = reservas;
    this.integritySecret = integritySecret;
    this.currency = currency;
  }

  /** Crea el pago PENDIENTE y devuelve los datos para el Widget/Checkout de Wompi (sandbox). */
  public Map<String, Object> iniciarPago(Long reservaId) {
    Reserva r = reservas.findById(reservaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reserva no existe"));
    if (r.getEstado() == EstadoReserva.CANCELADA) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reserva cancelada");
    }
    String referencia = "RC-" + r.getId() + "-" + System.currentTimeMillis();
    long valorCentavos = r.getValorTotal().multiply(java.math.BigDecimal.valueOf(100)).longValue();
    String firma = firma(referencia, valorCentavos, currency);

    Pago p = new Pago();
    p.setReserva(r);
    p.setValor(r.getValorTotal());
    p.setMoneda(currency);
    p.setReferencia(referencia);
    p.setEstado(EstadoPago.PENDIENTE);
    pagos.save(p);

    return Map.of(
        "referencia", referencia,
        "valorCentavos", valorCentavos,
        "moneda", currency,
        "firmaIntegridad", firma,
        "reservaId", r.getId());
  }

  /** Webhook de Wompi: actualiza pago y marca reserva PAGADA/APROBADA. Validar x-signature en prod. */
  public Pago webhook(String referencia, String transactionId, String status) {
    Pago p = pagos.findByReferencia(referencia)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pago no existe"));
    if ("APPROVED".equalsIgnoreCase(status)) {
      p.setEstado(EstadoPago.APROBADO);
      p.getReserva().setEstado(EstadoReserva.PAGADA);
    } else if ("DECLINED".equalsIgnoreCase(status)) {
      p.setEstado(EstadoPago.RECHAZADO);
    } else {
      p.setEstado(EstadoPago.FALLIDO);
    }
    p.setWompiTransactionId(transactionId);
    reservas.save(p.getReserva());
    return pagos.save(p);
  }

  private String firma(String referencia, long valorCentavos, String moneda) {
    try {
      String raw = referencia + valorCentavos + moneda + integritySecret;
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo firmar");
    }
  }
}
