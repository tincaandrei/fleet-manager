$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    .\mvnw -Popenapi clean compile spring-boot:start
    try {
        .\mvnw -Popenapi org.springdoc:springdoc-openapi-maven-plugin:generate
    }
    finally {
        .\mvnw -Popenapi spring-boot:stop
    }
}
finally {
    Pop-Location
}
