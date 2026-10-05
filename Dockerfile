# syntax=docker/dockerfile:1
# Imagem da Pedidos API. A mesma receita gera a imagem em Java 21 ou 25:
#   docker build -t pedidos-api:21 .
#   docker build --build-arg JAVA_VERSION=25 -t pedidos-api:25 .
# Os testes rodam antes, no `mvn package` normal ou na CI; aqui o build os pula.
ARG JAVA_VERSION=21

# 1) Compila o jar. Os POMs vêm antes do código para o cache das dependências sobreviver a mudanças no código.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY application/pom.xml application/pom.xml
COPY application/pedidos/pom.xml application/pedidos/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -f application/pom.xml -q dependency:go-offline
COPY application/pedidos/src application/pedidos/src
RUN --mount=type=cache,target=/root/.m2 mvn -f application/pom.xml -q package -DskipTests

# 2) Separa o jar em camadas: dependências mudam pouco, o código da aplicação muda sempre.
FROM eclipse-temurin:${JAVA_VERSION}-jre AS extract
WORKDIR /x
COPY --from=build /src/application/pedidos/target/pedidos-*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# 3) Imagem final: só o JRE e a aplicação, rodando sem privilégio de root.
FROM eclipse-temurin:${JAVA_VERSION}-jre
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=extract /x/extracted/dependencies/ ./
COPY --from=extract /x/extracted/spring-boot-loader/ ./
COPY --from=extract /x/extracted/snapshot-dependencies/ ./
COPY --from=extract /x/extracted/application/ ./
USER app
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
