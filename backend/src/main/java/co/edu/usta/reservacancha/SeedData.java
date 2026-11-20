package co.edu.usta.reservacancha;

import co.edu.usta.reservacancha.cancha.Cancha;
import co.edu.usta.reservacancha.cancha.CanchaRepository;
import co.edu.usta.reservacancha.horario.Horario;
import co.edu.usta.reservacancha.horario.HorarioRepository;
import co.edu.usta.reservacancha.user.Role;
import co.edu.usta.reservacancha.user.Usuario;
import co.edu.usta.reservacancha.user.UsuarioRepository;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SeedData {

  private static final Logger log = LoggerFactory.getLogger(SeedData.class);

  @Bean
  CommandLineRunner seed(
      UsuarioRepository usuarios,
      CanchaRepository canchas,
      HorarioRepository horarios,
      PasswordEncoder enc,
      JdbcTemplate jdbc,
      @Value("${app.admin.password}") String claveAdmin) {
    return args -> {
      permitirRolesNuevos(jdbc);
      // No se siembra ningún empleado: la base pública y el repositorio son abiertos, y una clave
      // de
      // demostración sería una puerta trasera. Los empleados los crea el administrador en el panel.
      if (!usuarios.existsByEmail("admin@reservacancha.co")) {
        usuarios.save(
            new Usuario(
                "Administrador", "admin@reservacancha.co", enc.encode(claveAdmin), Role.ADMIN));
      }
      // Las 3 canchas de Canchas Mónaco (Villavicencio). Vóley playa no pide abono (se paga en la
      // cancha); las de fútbol piden abono del 50 %. Todas abren los 7 días de 2:00 a 10:00 p. m.
      if (canchas.count() == 0) {
        cancha(canchas, horarios, "Cancha 1 — Vóley playa", "Vóley playa", "Arena", "60000", 0);
        cancha(canchas, horarios, "Cancha 2 — Fútbol 7", "Fútbol 7", "Sintética", "90000", 50);
        cancha(canchas, horarios, "Cancha 3 — Fútbol 9", "Fútbol 9", "Sintética", "120000", 50);
      }
    };
  }

  private static void cancha(
      CanchaRepository canchas,
      HorarioRepository horarios,
      String nombre,
      String tipo,
      String superficie,
      String precioHora,
      int porcentajeAbono) {
    Cancha c = new Cancha();
    c.setNombre(nombre);
    c.setTipo(tipo);
    c.setSuperficie(superficie);
    c.setUbicacion("Villavicencio");
    c.setPrecioHora(new BigDecimal(precioHora));
    c.setPorcentajeAbono(porcentajeAbono);
    canchas.save(c);
    for (DayOfWeek dia : DayOfWeek.values()) {
      Horario h = new Horario();
      h.setCancha(c);
      h.setDiaSemana(dia);
      h.setHoraApertura(LocalTime.of(14, 0));
      h.setHoraCierre(LocalTime.of(22, 0));
      horarios.save(h);
    }
  }

  /**
   * Una base creada antes de existir el rol EMPLEADO guarda «rol» con un CHECK (Postgres) o como
   * ENUM (H2) que no lo acepta, y ddl-auto=update no lo cambia. Se deja como texto con un CHECK que
   * incluye todos los roles; en una base nueva deja lo mismo.
   */
  private static void permitirRolesNuevos(JdbcTemplate jdbc) {
    for (String sql :
        new String[] {
          "ALTER TABLE usuarios ALTER COLUMN rol SET DATA TYPE VARCHAR(20)",
          "ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_rol_check",
          "ALTER TABLE usuarios ADD CONSTRAINT usuarios_rol_check"
              + " CHECK (rol IN ('ADMIN','JUGADOR','PROPIETARIO','EMPLEADO'))"
        }) {
      try {
        jdbc.execute(sql);
      } catch (RuntimeException e) {
        log.debug("Sin cambio en la columna rol: {}", e.getMessage());
      }
    }
  }
}
