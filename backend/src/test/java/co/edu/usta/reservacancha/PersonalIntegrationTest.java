package co.edu.usta.reservacancha;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import co.edu.usta.reservacancha.pago.EstadoPago;
import co.edu.usta.reservacancha.pago.Pago;
import co.edu.usta.reservacancha.pago.PagoRepository;
import co.edu.usta.reservacancha.reserva.ReservaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Panel del personal: el registro público solo crea jugadores, el empleado ve la agenda y cancela,
 * y solo el administrador ve las métricas y crea o desactiva empleados.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PersonalIntegrationTest {

  /** Días propios, lejos de los que usan las demás pruebas (60+, 400+ y 700+). */
  private static final AtomicInteger DIA = new AtomicInteger(3000);

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired ReservaRepository reservas;
  @Autowired PagoRepository pagos;

  private ResultActions pedir(MockHttpServletRequestBuilder req, String token, Object cuerpo)
      throws Exception {
    if (token != null) req = req.header("Authorization", "Bearer " + token);
    if (cuerpo != null) {
      req = req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
    }
    return mvc.perform(req);
  }

  private JsonNode leer(ResultActions r) throws Exception {
    return json.readTree(r.andReturn().getResponse().getContentAsString());
  }

  private String login(String email, String clave) throws Exception {
    return leer(pedir(post("/api/auth/login"), null, Map.of("email", email, "password", clave))
            .andExpect(status().isOk()))
        .get("token")
        .asText();
  }

  /** El administrador crea un empleado y devuelve {id, nombre, email, rol, activo}. */
  private JsonNode crearEmpleado(String admin, String correo) throws Exception {
    return leer(
        pedir(
                post("/api/admin/empleados"),
                admin,
                Map.of("nombre", "Laura Cancha", "email", correo, "password", TestSupport.CLAVE))
            .andExpect(status().isCreated()));
  }

  private String empleado() throws Exception {
    String correo = TestSupport.correoUnico();
    crearEmpleado(TestSupport.loginAdmin(mvc, json), correo);
    return login(correo, TestSupport.CLAVE);
  }

  private static String celularUnico() {
    return "31" + ThreadLocalRandom.current().nextLong(10_000_000L, 99_999_999L);
  }

  /** Reserva sin cuenta, como lo hace el jugador o el personal por teléfono. Devuelve su id. */
  private long reservar(long cancha, LocalDate dia, String inicio, String fin) throws Exception {
    return leer(pedir(
                post("/api/reservas/rapida"),
                null,
                Map.of(
                    "nombre",
                    "Pedro Prueba",
                    "celular",
                    celularUnico(),
                    "canchaId",
                    cancha,
                    "fecha",
                    dia.toString(),
                    "horaInicio",
                    inicio,
                    "horaFin",
                    fin))
            .andExpect(status().isOk()))
        .get("reserva")
        .get("id")
        .asLong();
  }

  @Test
  void elRegistroPublicoSoloCreaJugadores() throws Exception {
    for (String rol : List.of("PROPIETARIO", "EMPLEADO", "ADMIN")) {
      String correo = TestSupport.correoUnico();
      pedir(
              post("/api/auth/register"),
              null,
              Map.of(
                  "nombre", "Intruso", "email", correo, "password", TestSupport.CLAVE, "rol", rol))
          .andExpect(status().isForbidden());
      // La cuenta no quedó creada
      pedir(post("/api/auth/login"), null, Map.of("email", correo, "password", TestSupport.CLAVE))
          .andExpect(status().isUnauthorized());
    }
  }

  @Test
  void elAdministradorCreaUnEmpleadoQueIniciaSesion() throws Exception {
    String admin = TestSupport.loginAdmin(mvc, json);
    String correo = TestSupport.correoUnico();
    JsonNode e = crearEmpleado(admin, correo);
    assertThat(e.get("rol").asText()).isEqualTo("EMPLEADO");
    assertThat(e.has("password")).isFalse();

    JsonNode sesion =
        leer(
            pedir(
                    post("/api/auth/login"),
                    null,
                    Map.of("email", correo, "password", TestSupport.CLAVE))
                .andExpect(status().isOk()));
    assertThat(sesion.get("rol").asText()).isEqualTo("EMPLEADO");

    // Correo repetido: 409; la lista del personal lo incluye y no trae jugadores
    pedir(
            post("/api/admin/empleados"),
            admin,
            Map.of("nombre", "Otra", "email", correo, "password", TestSupport.CLAVE))
        .andExpect(status().isConflict());
    String lista =
        pedir(get("/api/admin/empleados"), admin, null)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(lista).contains(correo).contains("admin@reservacancha.co").doesNotContain("JUGADOR");
  }

  @Test
  void elEmpleadoVeLaAgendaPeroNoAdministra() throws Exception {
    String empleado = empleado();
    long id = reservar(2, LocalDate.now().plusDays(DIA.incrementAndGet()), "15:00", "16:00");

    pedir(get("/api/admin/reservas"), empleado, null).andExpect(status().isOk());
    // En la ocupación de la cancha ve quién reservó y su celular, como el administrador
    String agenda =
        pedir(get("/api/reservas/cancha/2"), empleado, null)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertThat(agenda)
        .contains("\"id\":" + id)
        .contains("Pedro Prueba")
        .contains("\"celular\":\"31");

    pedir(get("/api/admin/metricas"), empleado, null).andExpect(status().isForbidden());
    pedir(get("/api/admin/pagos"), empleado, null).andExpect(status().isForbidden());
    pedir(get("/api/admin/empleados"), empleado, null).andExpect(status().isForbidden());
    pedir(
            post("/api/admin/empleados"),
            empleado,
            Map.of("nombre", "X", "email", TestSupport.correoUnico(), "password", "Clave-123456"))
        .andExpect(status().isForbidden());
    pedir(
            post("/api/canchas"),
            empleado,
            Map.of("nombre", "Nueva", "tipo", "Fútbol 5", "precioHora", 50000))
        .andExpect(status().isForbidden());
  }

  @Test
  void elJugadorNoVeLaAgenda() throws Exception {
    String jugador = TestSupport.registrarJugador(mvc, json);
    pedir(get("/api/admin/reservas"), jugador, null).andExpect(status().isForbidden());
  }

  @Test
  void elEmpleadoCancelaLaReservaDeUnJugador() throws Exception {
    long id = reservar(2, LocalDate.now().plusDays(DIA.incrementAndGet()), "17:00", "18:00");
    JsonNode r =
        leer(
            pedir(post("/api/reservas/" + id + "/cancelar"), empleado(), null)
                .andExpect(status().isOk()));
    assertThat(r.get("estado").asText()).isEqualTo("CANCELADA");
  }

  @Test
  void unEmpleadoDesactivadoNoEntraNiUsaSuToken() throws Exception {
    String admin = TestSupport.loginAdmin(mvc, json);
    String correo = TestSupport.correoUnico();
    long id = crearEmpleado(admin, correo).get("id").asLong();
    String token = login(correo, TestSupport.CLAVE);
    pedir(get("/api/admin/reservas"), token, null).andExpect(status().isOk());

    JsonNode e =
        leer(
            pedir(patch("/api/admin/empleados/" + id), admin, Map.of("activo", false))
                .andExpect(status().isOk()));
    assertThat(e.get("activo").asBoolean()).isFalse();

    pedir(post("/api/auth/login"), null, Map.of("email", correo, "password", TestSupport.CLAVE))
        .andExpect(status().isUnauthorized());
    assertThat(pedir(get("/api/admin/reservas"), token, null).andReturn().getResponse().getStatus())
        .isIn(401, 403);

    // Se puede volver a activar; una cuenta de administrador no se desactiva desde aquí
    pedir(patch("/api/admin/empleados/" + id), admin, Map.of("activo", true))
        .andExpect(status().isOk());
    login(correo, TestSupport.CLAVE);
    long idAdmin = 0;
    for (JsonNode u : leer(pedir(get("/api/admin/empleados"), admin, null))) {
      if ("admin@reservacancha.co".equals(u.get("email").asText())) idAdmin = u.get("id").asLong();
    }
    pedir(patch("/api/admin/empleados/" + idAdmin), admin, Map.of("activo", false))
        .andExpect(status().isBadRequest());
  }

  @Test
  void lasMetricasJuntanReservasPagosYHorarios() throws Exception {
    String admin = TestSupport.loginAdmin(mvc, json);
    // Cancha propia ($90.000 la hora) abierta de 2:00 a 10:00 p. m. todos los días
    long cancha =
        leer(pedir(
                    post("/api/canchas"),
                    admin,
                    Map.of(
                        "nombre",
                        "Cancha Métricas",
                        "tipo",
                        "Fútbol 7",
                        "precioHora",
                        90000,
                        "porcentajeAbono",
                        50))
                .andExpect(status().isOk()))
            .get("id")
            .asLong();
    for (DayOfWeek d : DayOfWeek.values()) {
      pedir(
              post("/api/horarios"),
              admin,
              Map.of(
                  "canchaId",
                  cancha,
                  "diaSemana",
                  d.name(),
                  "horaApertura",
                  "14:00",
                  "horaCierre",
                  "22:00"))
          .andExpect(status().isOk());
    }
    LocalDate dia = LocalDate.now().plusDays(DIA.incrementAndGet());
    // 1 h de 3 a 4 (abono aprobado) y 2 h de 6 a 8 (abono rechazado); una cancelada no cuenta
    long r1 = reservar(cancha, dia, "15:00", "16:00");
    long r2 = reservar(cancha, dia, "18:00", "20:00");
    long r3 = reservar(cancha, dia, "20:00", "21:00");
    pedir(post("/api/reservas/" + r3 + "/cancelar"), admin, null).andExpect(status().isOk());
    pago(r1, "45000", EstadoPago.APROBADO);
    pago(r2, "90000", EstadoPago.RECHAZADO);

    JsonNode m =
        leer(
            pedir(get("/api/admin/metricas?desde=" + dia + "&hasta=" + dia), admin, null)
                .andExpect(status().isOk()));
    assertThat(m.get("reservas").asLong()).isEqualTo(2);
    assertThat(m.get("canceladas").asLong()).isEqualTo(1);
    assertThat(m.get("jugadores").asLong()).isEqualTo(2);
    assertThat(m.get("ingresos").decimalValue()).isEqualByComparingTo("270000");
    assertThat(m.get("abonosCobrados").decimalValue()).isEqualByComparingTo("45000");
    // 3 horas reservadas de 8 que abre: 37,5 %
    JsonNode c = null;
    for (JsonNode x : m.get("porCancha")) {
      if (x.get("canchaId").asLong() == cancha) c = x;
    }
    assertThat(c).isNotNull();
    assertThat(c.get("horas").asDouble()).isEqualTo(3.0);
    assertThat(c.get("ocupacion").asDouble()).isEqualTo(37.5);
    assertThat(m.get("porDia").get(0).get("reservas").asLong()).isEqualTo(2);
    assertThat(m.get("porHora").toString())
        .contains("{\"hora\":15,\"reservas\":1}")
        .contains("{\"hora\":18,\"reservas\":1}")
        .contains("{\"hora\":19,\"reservas\":1}")
        .doesNotContain("\"hora\":20");

    pedir(get("/api/admin/metricas?desde=" + dia + "&hasta=" + dia.minusDays(1)), admin, null)
        .andExpect(status().isBadRequest());
    pedir(get("/api/admin/metricas?desde=" + dia + "&hasta=" + dia.plusDays(92)), admin, null)
        .andExpect(status().isBadRequest());
  }

  private void pago(long reservaId, String valor, EstadoPago estado) {
    Pago p = new Pago();
    p.setReserva(reservas.findById(reservaId).orElseThrow());
    p.setValor(new BigDecimal(valor));
    p.setReferencia("RC-" + reservaId + "-prueba");
    p.setEstado(estado);
    pagos.save(p);
  }
}
