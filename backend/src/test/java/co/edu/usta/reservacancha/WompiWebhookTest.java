package co.edu.usta.reservacancha;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Eventos firmados de Wompi sobre /api/wompi/webhook (formato transaction.updated). */
@SpringBootTest
@AutoConfigureMockMvc
class WompiWebhookTest {

  private static final AtomicInteger DIA = new AtomicInteger(400);
  private static final long TIMESTAMP = 1762790400L;

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;

  @Value("${app.wompi.events-secret}")
  String secretoEventos;

  /** Reserva de una hora en la Cancha 1 con su pago iniciado: {token, reservaId, referencia}. */
  private Object[] pagoIniciado() throws Exception {
    String token = TestSupport.registrarJugador(mvc, json);
    String reserva =
        json.writeValueAsString(
            Map.of(
                "canchaId", 1,
                "fecha", LocalDate.now().plusDays(DIA.incrementAndGet()).toString(),
                "horaInicio", "18:00:00",
                "horaFin", "19:00:00"));
    long id =
        json.readTree(
                mvc.perform(
                        post("/api/reservas")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(reserva))
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getContentAsString())
            .get("id")
            .asLong();
    JsonNode pago =
        json.readTree(
            mvc.perform(post("/api/pagos/iniciar/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    return new Object[] {token, id, pago.get("referencia").asText()};
  }

  private String evento(String referencia, String status, long centavos, String secreto)
      throws Exception {
    String txId = "15113-" + TIMESTAMP + "-10000";
    String checksum = sha256(txId + status + centavos + TIMESTAMP + secreto);
    return json.writeValueAsString(
        Map.of(
            "event", "transaction.updated",
            "data",
                Map.of(
                    "transaction",
                    Map.of(
                        "id", txId,
                        "reference", referencia,
                        "status", status,
                        "amount_in_cents", centavos,
                        "currency", "COP")),
            "environment", "test",
            "signature",
                Map.of(
                    "properties",
                    List.of("transaction.id", "transaction.status", "transaction.amount_in_cents"),
                    "checksum",
                    checksum.toUpperCase()),
            "timestamp", TIMESTAMP));
  }

  private static String sha256(String texto) throws Exception {
    return HexFormat.of()
        .formatHex(
            MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
  }

  private String estadoPago(String token, long reservaId) throws Exception {
    return json.readTree(
            mvc.perform(
                    get("/api/pagos/reserva/" + reservaId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString())
        .get(0)
        .get("estado")
        .asText();
  }

  private org.springframework.test.web.servlet.ResultActions enviar(String cuerpo) throws Exception {
    return mvc.perform(
        post("/api/wompi/webhook").contentType(MediaType.APPLICATION_JSON).content(cuerpo));
  }

  @Test
  void eventoAprobadoConFirmaValidaMarcaLaReservaPagada() throws Exception {
    Object[] p = pagoIniciado();
    String res =
        enviar(evento((String) p[2], "APPROVED", 6_000_000, secretoEventos))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode r = json.readTree(res);
    assertThat(r.get("estado").asText()).isEqualTo("APROBADO");
    assertThat(r.get("reservaEstado").asText()).isEqualTo("PAGADA");
  }

  @Test
  void eventoRechazadoDejaLaReservaPendiente() throws Exception {
    Object[] p = pagoIniciado();
    JsonNode r =
        json.readTree(
            enviar(evento((String) p[2], "DECLINED", 6_000_000, secretoEventos))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertThat(r.get("estado").asText()).isEqualTo("RECHAZADO");
    assertThat(r.get("reservaEstado").asText()).isEqualTo("PENDIENTE");
  }

  @Test
  void firmaFalsaResponde401YNoTocaElPago() throws Exception {
    Object[] p = pagoIniciado();
    enviar(evento((String) p[2], "APPROVED", 6_000_000, "otro-secreto"))
        .andExpect(status().isUnauthorized());
    assertThat(estadoPago((String) p[0], (long) p[1])).isEqualTo("PENDIENTE");
  }

  @Test
  void montoDistintoNoApruebaElPago() throws Exception {
    Object[] p = pagoIniciado();
    enviar(evento((String) p[2], "APPROVED", 100, secretoEventos))
        .andExpect(status().isOk());
    assertThat(estadoPago((String) p[0], (long) p[1])).isEqualTo("PENDIENTE");
  }

  @Test
  void referenciaAjenaSeIgnoraCon200() throws Exception {
    String res =
        enviar(evento("OTRA-REFERENCIA", "APPROVED", 6_000_000, secretoEventos))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(json.readTree(res).get("ignorado").asBoolean()).isTrue();
  }

  @Test
  void formatoSimplificadoSinSecretoSigueRespondiendo401() throws Exception {
    enviar("{\"referencia\":\"RC-1-0\",\"transactionId\":\"x\",\"status\":\"APPROVED\"}")
        .andExpect(status().isUnauthorized());
  }
}
