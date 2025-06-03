package br.com.hyteck.school_control.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenAPI (Swagger) documentation.
 * Sets up the basic information for the API documentation and defines the security scheme
 * for JWT Bearer authentication.
 */
@Configuration
@SecurityScheme(
    name = "bearerAuth", // Can be used as security requirement in @Operation
    type = SecuritySchemeType.HTTP,
    bearerFormat = "JWT", // Specifies the format of the bearer token
    scheme = "bearer" // The security scheme to be used (bearer token)
)
public class OpenApiConfig {

    /**
     * Configures the OpenAPI bean with general information about the School Control API.
     * This includes the title, description, version, contact information, and license.
     *
     * @return An {@link OpenAPI} instance customized for the School Control application.
     */
    @Bean
    public OpenAPI schoolControlOpenAPI() {
        return new OpenAPI()
                .info(new Info() // Sets the main information for the API
                        .title("School Control API")
                        .description("API para gerenciamento de escolas e sistemas relacionados")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe Hyteck")
                                .email("suporte@hyteck.com.br"))
                        .license(new License()
                                .name("Apache 2.0") // License name
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html"))); // Link to the license
    }
}
