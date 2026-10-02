package co.edu.usta.reservacancha.horario;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/horarios")
public class HorarioController {

  private final HorarioRepository repo;

  public HorarioController(HorarioRepository repo) { this.repo = repo; }

  @GetMapping("/cancha/{canchaId}")
  public List<Horario> porCancha(@PathVariable Long canchaId) {
    return repo.findByCanchaId(canchaId);
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public Horario crear(@Valid @RequestBody Horario h) { return repo.save(h); }
}
