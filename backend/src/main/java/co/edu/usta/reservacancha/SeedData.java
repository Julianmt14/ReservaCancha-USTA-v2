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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SeedData {

  @Bean
  CommandLineRunner seed(
      UsuarioRepository usuarios,
      CanchaRepository canchas,
      HorarioRepository horarios,
      PasswordEncoder enc,
      @Value("${app.admin.password}") String claveAdmin) {
    return args -> {
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
}
