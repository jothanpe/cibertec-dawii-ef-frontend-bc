package pe.edu.cibertec.patitas_frontend_wc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import pe.edu.cibertec.patitas_frontend_wc.dto.LoginRequestDTO;
import pe.edu.cibertec.patitas_frontend_wc.dto.LoginResponseDTO;
import pe.edu.cibertec.patitas_frontend_wc.dto.LogoutRequestDTO;
import pe.edu.cibertec.patitas_frontend_wc.dto.LogoutResponseDTO;
import pe.edu.cibertec.patitas_frontend_wc.viewmodel.LoginModel;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/login")
public class LoginController {

    @Autowired
    WebClient webClientAutenticacion;

    @GetMapping("/inicio")
    public String inicio(Model model) {
        LoginModel loginModel = new LoginModel("00", "", "");
        model.addAttribute("loginModel", loginModel);
        return "inicio";
    }

    @PostMapping("/autenticar")
    public String autenticar(@RequestParam("tipoDocumento") String tipoDocumento,
                             @RequestParam("numeroDocumento") String numeroDocumento,
                             @RequestParam("password") String password,
                             Model model) {

        // Validar campos de entrada
        if (tipoDocumento == null || tipoDocumento.trim().length() == 0 ||
                numeroDocumento == null || numeroDocumento.trim().length() == 0 ||
                password == null || password.trim().length() == 0){

            LoginModel loginModel = new LoginModel("01", "Error: Debe completar correctamente sus credenciales", "");
            model.addAttribute("loginModel", loginModel);
            return "inicio";
        }

        try {

            // Invocar API de validación de usuario
            LoginRequestDTO loginRequestDTO = new LoginRequestDTO(tipoDocumento, numeroDocumento, password);

            Mono<LoginResponseDTO> monoLoginResponseDTO = webClientAutenticacion.post()
                    .uri("/login")
                    .body(Mono.just(loginRequestDTO), LoginRequestDTO.class)
                    .retrieve()
                    .bodyToMono(LoginResponseDTO.class);

            // recuperar resultado del mono (Sincrono o bloqueante)
            LoginResponseDTO loginResponseDTO = monoLoginResponseDTO.block();

            // Validar respuesta
            if (loginResponseDTO.codigo().equals("00")) {

                LoginModel loginModel = new LoginModel("00", "", loginResponseDTO.nombreUsuario());
                model.addAttribute("loginModel", loginModel);
                return "principal";

            } else {

                LoginModel loginModel = new LoginModel("02", "Error: Autenticación fallida", "");
                model.addAttribute("loginModel", loginModel);
                return "inicio";

            }

        } catch(Exception e) {

            LoginModel loginModel = new LoginModel("99", "Error: Ocurrió un problema en la autenticación", "");
            model.addAttribute("loginModel", loginModel);
            System.out.println(e.getMessage());
            return "inicio";

        }

    }

    @PostMapping("/logout-async")
    public Mono<LogoutResponseDTO> logout(@RequestBody LogoutRequestDTO logoutRequestDTO) {
        if(logoutRequestDTO.tipoDocumento() == null || logoutRequestDTO.tipoDocumento().trim().length() == 0
                || logoutRequestDTO.numeroDocumento() == null || logoutRequestDTO.numeroDocumento().trim().length()==0) {
            LoginModel loginModel = new LoginModel("91","Error: Debe completar las credenciales","");

            return Mono.just(new LogoutResponseDTO(false,null,"Error: Debe completar las credenciales"
            ));
        }
        try {
            return webClientAutenticacion.post()
                    .uri("/logout")
                    .body(Mono.just(logoutRequestDTO), LogoutResponseDTO.class)
                    .retrieve()
                    .bodyToMono(LogoutResponseDTO.class)
                    .flatMap(response ->{
                        if(response.resultado().equals(true)){
                            return Mono.just(new LogoutResponseDTO(true,response.fecha(),response.mensajeError()));
                        } else {
                            return Mono.just(new LogoutResponseDTO(false,null,"Error al cerrar sesión"));
                        }
                    });
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return Mono.just(new LogoutResponseDTO(false,null,"Error: Error en el logout"));
        }
    }
}
