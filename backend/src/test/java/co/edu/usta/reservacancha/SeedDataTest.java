package co.edu.usta.reservacancha;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// Base aparte: la semilla solo crea al administrador si no existe
@SpringBootTest(
    properties = {
      "app.admin.password=clave-de-prueba",
      "spring.datasource.url=jdbc:h2:mem:semilla;DB_CLOSE_DELAY=-1"
    })
@AutoConfigureMockMvc
class SeedDataTest {

  @Autowired MockMvc mvc;

  private int login(String clave) throws Exception {
    String cuerpo = "{\"email\":\"admin@reservacancha.co\",\"password\":\"" + clave + "\"}";
    return mvc.perform(
            post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(cuerpo))
        .andReturn()
        .getResponse()
        .getStatus();
  }

  @Test
  void adminEntraConLaClaveDeLaVariable() throws Exception {
    assertThat(login("clave-de-prueba")).isEqualTo(200);
  }

  @Test
  void admin123NoEntraSiLaVariableEstaDefinida() throws Exception {
    assertThat(login("admin123")).isEqualTo(401);
  }
}
