package co.edu.usta.reservacancha.user;

import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Jugadores que reservan sin crear cuenta: se identifican con su nombre y su celular. Cada reserva
 * necesita un usuario, así que se guarda uno interno con un correo que sale del celular y una
 * contraseña al azar que nadie usa: el jugador entra con el token que recibe al reservar.
 */
@Service
public class Clientes {

  static final String DOMINIO = "@celular.reservacancha.co";

  private final UsuarioRepository usuarios;
  private final PasswordEncoder encoder;

  public Clientes(UsuarioRepository usuarios, PasswordEncoder encoder) {
    this.usuarios = usuarios;
    this.encoder = encoder;
  }

  /**
   * El jugador de ese celular; si es la primera vez, se crea. El nombre queda el último que dio.
   */
  public Usuario porCelular(String nombre, String celular) {
    String email = celular + DOMINIO;
    Usuario u = usuarios.findByEmail(email).orElseGet(() -> nuevo(nombre, email));
    // Ese correo solo puede ser de un jugador: nunca se entrega la sesión de alguien del personal
    if (u.getRol() != Role.JUGADOR || !u.isActivo()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Ese celular no puede reservar aquí");
    }
    u.setNombre(nombre.trim());
    u.setCelular(celular);
    try {
      return usuarios.save(u);
    } catch (DataIntegrityViolationException e) {
      // Dos reservas del mismo celular al mismo tiempo: la otra ya lo creó
      return usuarios.findByEmail(email).orElseThrow(() -> e);
    }
  }

  private Usuario nuevo(String nombre, String email) {
    return new Usuario(
        nombre.trim(), email, encoder.encode(UUID.randomUUID().toString()), Role.JUGADOR);
  }
}
