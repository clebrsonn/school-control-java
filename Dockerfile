# Etapa 1: build da aplicação com cache inteligente
FROM eclipse-temurin:23-jdk AS builder

WORKDIR /app

# Copia somente os arquivos relacionados às dependências
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Baixa as dependências (isso será cacheado enquanto os arquivos acima não mudarem)
RUN ./gradlew dependencies --no-daemon || true

# Copia o restante do projeto (ex: src)
COPY . .

# Compila a aplicação
RUN ./gradlew bootJar --no-daemon

# Etapa 2: imagem final leve com apenas o JAR
FROM eclipse-temurin:23-jre

WORKDIR /app

# Variáveis de memória para ambientes limitados como Render
ENV JAVA_TOOL_OPTIONS="-Xmx256m -Xss512k -XX:MaxMetaspaceSize=256M -XX:ReservedCodeCacheSize=128M"

# Copia o JAR gerado
COPY --from=builder /app/build/libs/*.jar app.jar

# Executa a aplicação
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
