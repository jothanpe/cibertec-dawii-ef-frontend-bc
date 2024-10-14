package pe.edu.cibertec.patitas_frontend_wc.config;

import feign.Request;
import org.springframework.context.annotation.Bean;

public class FeignClientConfig {
    @Bean
    public Request.Options requestoptions() {
        return new Request.Options(30000,6000);
    }
}
