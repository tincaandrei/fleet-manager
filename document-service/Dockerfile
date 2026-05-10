FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-venv \
    && rm -rf /var/lib/apt/lists/*

COPY parser ./parser
RUN python3 -m venv /opt/document-parser-venv \
    && /opt/document-parser-venv/bin/pip install --no-cache-dir -r /app/parser/requirements.txt

COPY --from=build /app/target/*.jar app.jar
ENV DOCUMENT_PARSER_PYTHON=/opt/document-parser-venv/bin/python
ENV DOCUMENT_PARSER_SCRIPT_PATH=/app/parser/parse_inspection_pdf.py
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
