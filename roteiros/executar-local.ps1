param(
    [ValidateSet("teste", "preparar", "coletor", "web")]
    [string]$Acao = "teste"
)

$projectRoot = Split-Path -Parent $PSScriptRoot
$javaHome = @(
    $env:JAVA_HOME,
    [Environment]::GetEnvironmentVariable("JAVA_HOME", "User"),
    (Get-ChildItem "C:\Program Files\Microsoft\jdk-*-hotspot" -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        Select-Object -First 1 -ExpandProperty FullName)
) | Where-Object { $_ -and (Test-Path (Join-Path $_ "bin\java.exe")) } | Select-Object -First 1

$mavenHome = @(
    $env:MAVEN_HOME,
    [Environment]::GetEnvironmentVariable("MAVEN_HOME", "User"),
    (Get-ChildItem "C:\Users\$env:USERNAME\AppData\Local\apache-maven\apache-maven-*" -Directory -ErrorAction SilentlyContinue |
        Sort-Object Name -Descending |
        Select-Object -First 1 -ExpandProperty FullName)
) | Where-Object { $_ -and (Test-Path (Join-Path $_ "bin\mvn.cmd")) } | Select-Object -First 1

if (-not $javaHome) {
    throw "JAVA_HOME não aponta para um JDK válido. Configure o JDK 21 antes de continuar."
}

if (-not $mavenHome) {
    throw "MAVEN_HOME não aponta para uma instalação válida do Maven."
}

$env:JAVA_HOME = $javaHome
$mavenCommand = Join-Path $mavenHome "bin\mvn.cmd"

if ($Acao -in @("coletor", "web") -and -not (Test-Path (Join-Path $projectRoot ".env"))) {
    throw "Crie o arquivo .env a partir de .env.exemplo e informe as credenciais do MySQL antes de iniciar a aplicação."
}

Push-Location $projectRoot
try {
    switch ($Acao) {
        "teste" {
            & $mavenCommand test
        }
        "preparar" {
            & $mavenCommand install -DskipTests
        }
        "coletor" {
            & $mavenCommand -pl coletor spring-boot:run
        }
        "web" {
            & $mavenCommand -pl servidor-web spring-boot:run
        }
    }
} finally {
    Pop-Location
}
