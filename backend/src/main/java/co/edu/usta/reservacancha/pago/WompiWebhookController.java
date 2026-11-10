package co.edu.usta.reservacancha.pago;

import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/wompi")
public class WompiWebhookController {

  private final WompiService wompi;
  private final String eventsSecret;

  public WompiWebhookController(
      WompiService wompi, @Value("${app.wompi.events-secret}") String eventsSecret) {
    this.wompi = wompi;
    this.eventsSecret = eventsSecret;
  }

  @PostMapping("/webhook")
  public Map<String, Object> webhook(
      @RequestHeader(value = "X-Webhook-Secret", required = false) String secreto,
      @RequestBody JsonNode body) {
    // El endpoint es publico (lo llama Wompi): sin firma valida no se toca ningun pago (SEC-01)
    if (body.has("signature")) {
      return eventoFirmado(body);
    }
    if (secreto == null || !iguales(secreto, eventsSecret)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Webhook no autorizado");
    }
    // Formato simplificado para pruebas manuales: {referencia, transactionId, status}
    Pago p =
        wompi.webhook(
            body.path("referencia").asText(""),
            body.path("transactionId").asText(""),
            body.path("status").asText("PENDING"));
    return respuesta(p);
  }

  /**
   * Evento de Wompi (transaction.updated). La firma es el SHA-256 de los valores de
   * signature.properties, el timestamp y el secreto de eventos, en ese orden.
   */
  private Map<String, Object> eventoFirmado(JsonNode body) {
    if (!firmaValida(body, eventsSecret)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Firma del evento no valida");
    }
    if (!"transaction.updated".equals(body.path("event").asText())) {
      return Map.of("ignorado", true);
    }
    JsonNode tx = body.path("data").path("transaction");
    // Una transaccion que no es de esta aplicacion se responde con 200 para que Wompi no reintente
    return wompi
        .evento(
            tx.path("reference").asText(""),
            tx.path("id").asText(""),
            tx.path("status").asText("PENDING"),
            tx.path("amount_in_cents").asLong(-1))
        .map(this::respuesta)
        .orElse(Map.of("ignorado", true));
  }

  static boolean firmaValida(JsonNode body, String secreto) {
    JsonNode firma = body.path("signature");
    JsonNode propiedades = firma.path("properties");
    if (!propiedades.isArray() || propiedades.isEmpty() || !body.hasNonNull("timestamp")) {
      return false;
    }
    StringBuilder cadena = new StringBuilder();
    for (JsonNode propiedad : propiedades) {
      JsonNode valor = body.path("data");
      for (String parte : propiedad.asText().split("\\.")) {
        valor = valor.path(parte);
      }
      if (valor.isMissingNode() || valor.isNull() || valor.isContainerNode()) {
        return false;
      }
      cadena.append(valor.asText());
    }
    cadena.append(body.path("timestamp").asText()).append(secreto);
    return iguales(sha256(cadena.toString()), firma.path("checksum").asText("").toLowerCase());
  }

  private static String sha256(String texto) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(texto.getBytes(StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo verificar");
    }
  }

  private static boolean iguales(String a, String b) {
    return MessageDigest.isEqual(
        a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
  }

  private Map<String, Object> respuesta(Pago p) {
    return Map.of(
        "pagoId",
        p.getId(),
        "estado",
        p.getEstado().name(),
        "reserva",
        p.getReserva().getId(),
        "reservaEstado",
        p.getReserva().getEstado().name());
  }
}
