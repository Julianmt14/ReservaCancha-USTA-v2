package co.edu.usta.reservacancha.cancha;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface CanchaRepository extends JpaRepository<Cancha, Long> {
  List<Cancha> findByActivaTrue();

  /**
   * La cancha con bloqueo de escritura hasta que termine la transacción: si varias personas
   * reservan la misma cancha a la vez, las reservas se revisan una por una y solo una gana la hora.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT c FROM Cancha c WHERE c.id = :id")
  Optional<Cancha> bloquearParaReservar(Long id);
}
