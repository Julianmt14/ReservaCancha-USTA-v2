package co.edu.usta.reservacancha.user;

public enum Role {
  ADMIN, // Administrador: canchas, horarios, métricas y cuentas del personal
  JUGADOR, // Jugador o equipo: consulta disponibilidad y reserva
  PROPIETARIO, // Propietario: mismo panel que el administrador (seguimiento e ingresos)
  EMPLEADO; // Trabajador de la cancha: agenda, reservas por teléfono y cancelaciones

  /** Administrador o propietario: todo el panel, incluidas métricas, canchas y empleados. */
  public boolean administra() {
    return this == ADMIN || this == PROPIETARIO;
  }

  /** Cualquiera del personal de la cancha (administra o es empleado). */
  public boolean esPersonal() {
    return administra() || this == EMPLEADO;
  }
}
