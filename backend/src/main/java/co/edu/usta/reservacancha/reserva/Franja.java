package co.edu.usta.reservacancha.reserva;

import java.time.LocalTime;

/** Hora de inicio y de fin de una reserva, sin cargar el resto de la entidad. */
public record Franja(LocalTime inicio, LocalTime fin) {

  /** Mismo criterio que existeTraslape: (inicio < otroFin) y (fin > otroInicio). */
  public boolean seCruzaCon(LocalTime otroInicio, LocalTime otroFin) {
    return inicio.isBefore(otroFin) && fin.isAfter(otroInicio);
  }
}
