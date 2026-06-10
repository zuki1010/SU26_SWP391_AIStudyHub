package swp391.aistudyhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class Su26Swp391AiStudyHubApplication {

    public static void main(String[] args) {

        SpringApplication.run(Su26Swp391AiStudyHubApplication.class, args);

        printStartupInfo();
    }
    private static void printStartupInfo() {
        System.out.println("""
                
                ╔═══════════════════════════════════════════════════════════╗
                ║                                                           ║
                ║   🍎                   WELCOME                      🍎    ║
                ║   🍎   AI STUDYHUB SYSTEM - STARTED SUCCESSFULLY    🍎    ║
                ║                                                           ║
                ╠═══════════════════════════════════════════════════════════╣
                ║                                                           ║
                ║  📌 Server:    http://localhost:8080                      ║
                ║  📚 Swagger:   http://localhost:8080/swagger-ui.html      ║
                ║                                                           ║
                ║  🚀 API Endpoints:                                        ║
                ║     • GET   /api/...                                      ║
                ║     • GET    /api/...                                     ║
                ║     • GET    /api/...                                     ║
                ║     • GET    /api/...                                     ║
                ║     • GET   /api/...                                      ║
                ║     • POST   /api/...                                     ║
                ║                                                           ║
                ╚═══════════════════════════════════════════════════════════╝
                """);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
