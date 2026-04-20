$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
Push-Location $root
try {
    .\mvnw -Popenapi spring-boot:start
    try {
        .\mvnw -Popenapi springdoc-openapi:generate
    }
    finally {
        .\mvnw -Popenapi spring-boot:stop
    }
}
finally {
    Pop-Location
}
