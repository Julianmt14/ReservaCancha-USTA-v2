package co.edu.usta.reservacancha.pago;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {
  List<Pago> findByReservaId(Long reservaId);
  Optional<Pago> findByReferencia(String referencia);
}
