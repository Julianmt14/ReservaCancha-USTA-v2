package co.edu.usta.reservacancha;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.usta.reservacancha.user.Role;
import co.edu.usta.reservacancha.user.Usuario;
import co.edu.usta.reservacancha.user.UsuarioRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * HU-13 y HU-14: el jugador reserva sin cuenta (nombre y celular), solo 1 o 2 horas en punto dentro
 * del horario de 2:00 a 10:00 p. m., y en línea paga solo el abono de la cancha.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ReservaRapidaTest {

  /** Cada prueba usa una fecha distinta para que no choquen entre si (la base H2 se comparte). */
  private static final AtomicInteger DIA = new AtomicInteger(700);

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired UsuarioRepository usuarios;

  private static LocalDate fechaNueva() {
    return LocalDate.now().plusDays(DIA.incrementAndGet());
  }

  private static String celularNuevo() {
    return "3" + ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L);
  }

  private ResultActions rapida(
      String nombre, String celular, long canchaId, LocalDate fecha, String inicio, String fin)
      throws Exception {
    String body =
        json.writeValueAsString(
            Map.of(
                "nombre", nombre,
                "celular", celular,
                "canchaId", canchaId,
                "fecha", fecha.toString(),
                "horaInicio", inicio,
                "horaFin", fin));
    return mvc.perform(
        post("/api/reservas/rapida").contentType(MediaType.APPLICATION_JSON).content(body));
  }

  private JsonNode cuerpo(ResultActions r) throws Exception {
    return json.readTree(r.andReturn().getResponse().getContentAsString());
  }

  @Test
  void jugadorReservaSinCuentaConNombreYCelular() throws Exception {
    String celular = celularNuevo();
    JsonNode res =
        cuerpo(
            rapida("Ana Prueba", celular, 2, fechaNueva(), "15:00:00", "16:00:00")
                .andExpect(status().isOk()));
    assertThat(res.at("/reserva/estado").asText()).isEqualTo("PENDIENTE");
    assertThat(res.at("/reserva/valorTotal").decimalValue()).isEqualByComparingTo("90000");
    assertThat(res.at("/reserva/usuario/celular").asText()).isEqualTo(celular);
    assertThat(res.at("/reserva/usuario").has("password")).isFalse();
    assertThat(res.at("/sesion/rol").asText()).isEqualTo("JUGADOR");

    // Con la sesión que recibe ve su reserva en «Mis reservas»
    String token = res.at("/sesion/token").asText();
    JsonNode mias =
        cuerpo(
            mvc.perform(get("/api/reservas/mias").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()));
    assertThat(mias).hasSize(1);
    assertThat(mias.get(0).get("id").asLong()).isEqualTo(res.at("/reserva/id").asLong());
  }

  @Test
  void elMismoCelularJuntaSusReservas() throws Exception {
    String celular = celularNuevo();
    LocalDate fecha = fechaNueva();
    JsonNode a = cuerpo(rapida("Beto", celular, 2, fecha, "14:00:00", "15:00:00"));
    JsonNode b = cuerpo(rapida("Beto Ruiz", celular, 3, fecha, "18:00:00", "20:00:00"));
    assertThat(b.at("/sesion/id").asLong()).isEqualTo(a.at("/sesion/id").asLong());
    assertThat(b.at("/sesion/nombre").asText()).isEqualTo("Beto Ruiz");
  }

  @Test
  void celularInvalidoONombreVacioResponden400() throws Exception {
    rapida("Ana", "12345", 2, fechaNueva(), "15:00:00", "16:00:00")
        .andExpect(status().isBadRequest());
    rapida(" ", celularNuevo(), 2, fechaNueva(), "15:00:00", "16:00:00")
        .andExpect(status().isBadRequest());
  }

  @Test
  void soloSeReservaUnaODosHorasEnPuntoDentroDelHorario() throws Exception {
    LocalDate fecha = fechaNueva();
    String celular = celularNuevo();
    rapida("Ana", celular, 2, fecha, "14:30:00", "15:30:00").andExpect(status().isBadRequest());
    rapida("Ana", celular, 2, fecha, "14:00:00", "15:30:00").andExpect(status().isBadRequest());
    rapida("Ana", celular, 2, fecha, "14:00:00", "17:00:00").andExpect(status().isBadRequest());
    rapida("Ana", celular, 2, fecha, "12:00:00", "13:00:00").andExpect(status().isBadRequest());
    rapida("Ana", celular, 2, fecha, "21:00:00", "23:00:00").andExpect(status().isBadRequest());
    rapida("Ana", celular, 2, fecha, "20:00:00", "22:00:00").andExpect(status().isOk());
  }

  @Test
  void laMismaHoraNoSeReservaDosVeces() throws Exception {
    LocalDate fecha = fechaNueva();
    rapida("Ana", celularNuevo(), 2, fecha, "15:00:00", "16:00:00").andExpect(status().isOk());
    rapida("Beto", celularNuevo(), 2, fecha, "15:00:00", "16:00:00")
        .andExpect(status().isConflict());
    rapida("Beto", celularNuevo(), 2, fecha, "14:00:00", "16:00:00")
        .andExpect(status().isConflict());
    rapida("Beto", celularNuevo(), 2, fecha, "16:00:00", "17:00:00").andExpect(status().isOk());
    rapida("Beto", celularNuevo(), 1, fecha, "15:00:00", "16:00:00").andExpect(status().isOk());
  }

  @Test
  void diezPersonasALaVezPorLaMismaHoraSoloUnaLaConsigue() throws Exception {
    LocalDate fecha = fechaNueva();
    int personas = 10;
    ExecutorService hilos = Executors.newFixedThreadPool(personas);
    CountDownLatch salida = new CountDownLatch(1);
    List<Future<Integer>> codigos = new ArrayList<>();
    for (int i = 0; i < personas; i++) {
      String nombre = "Persona " + i;
      String celular = celularNuevo();
      codigos.add(
          hilos.submit(
              () -> {
                salida.await();
                return rapida(nombre, celular, 3, fecha, "20:00:00", "21:00:00")
                    .andReturn()
                    .getResponse()
                    .getStatus();
              }));
    }
    salida.countDown();
    List<Integer> resultado = new ArrayList<>();
    for (Future<Integer> f : codigos) resultado.add(f.get());
    hilos.shutdown();
    assertThat(resultado).filteredOn(c -> c == 200).hasSize(1);
    assertThat(resultado).filteredOn(c -> c == 409).hasSize(personas - 1);
  }

  @Test
  void enLineaSePagaSoloElAbonoDeLaCancha() throws Exception {
    LocalDate fecha = fechaNueva();
    // Fútbol 7: $90.000 la hora, abono del 50 %
    JsonNode futbol = cuerpo(rapida("Ana", celularNuevo(), 2, fecha, "19:00:00", "20:00:00"));
    JsonNode pago =
        cuerpo(
            mvc.perform(
                    post("/api/pagos/iniciar/" + futbol.at("/reserva/id").asLong())
                        .header("Authorization", "Bearer " + futbol.at("/sesion/token").asText()))
                .andExpect(status().isOk()));
    assertThat(pago.get("valor").decimalValue()).isEqualByComparingTo("45000");
    assertThat(pago.get("valorCentavos").asLong()).isEqualTo(4_500_000L);

    // Vóley playa: no pide abono, se paga todo en la cancha
    JsonNode voley = cuerpo(rapida("Beto", celularNuevo(), 1, fecha, "19:00:00", "20:00:00"));
    mvc.perform(
            post("/api/pagos/iniciar/" + voley.at("/reserva/id").asLong())
                .header("Authorization", "Bearer " + voley.at("/sesion/token").asText()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void unCelularDelPersonalNoEntregaSuSesion() throws Exception {
    String celular = celularNuevo();
    usuarios.save(
        new Usuario("Personal", celular + "@celular.reservacancha.co", "x", Role.PROPIETARIO));
    rapida("Intruso", celular, 2, fechaNueva(), "15:00:00", "16:00:00")
        .andExpect(status().isConflict());
  }
}
