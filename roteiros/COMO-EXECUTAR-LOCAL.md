# Executar o projeto localmente

## Primeira preparação

1. Abra o MySQL Workbench e execute `database/schema.sql`.
2. Na raiz do projeto, crie o arquivo local de configuração:

   ```powershell
   Copy-Item .env.exemplo .env
   notepad .env
   ```

3. No `.env`, informe uma senha para `MYSQL_PASSWORD` e outra para `MYSQL_ROOT_PASSWORD`.
   Os valores devem corresponder ao usuário e à configuração local do MySQL.
4. Para validar que o Java e o Maven estão funcionando, abra o PowerShell na raiz do projeto e execute:

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\roteiros\executar-local.ps1 teste
   ```

5. Prepare os módulos antes da primeira execução:

   ```powershell
   powershell -ExecutionPolicy Bypass -File .\roteiros\executar-local.ps1 preparar
   ```

## Iniciar as aplicações

Abra dois terminais PowerShell na raiz do projeto.

No primeiro, inicie o coletor:

```powershell
powershell -ExecutionPolicy Bypass -File .\roteiros\executar-local.ps1 coletor
```

No segundo, inicie o servidor web:

```powershell
powershell -ExecutionPolicy Bypass -File .\roteiros\executar-local.ps1 web
```

Abra `http://localhost:8080` no navegador. Para interromper qualquer aplicação, use `Ctrl+C` no terminal correspondente.

## Pelo NetBeans

1. Abra a pasta raiz como projeto Maven.
2. Em **Tools > Java Platforms**, selecione o JDK 21.
3. Execute primeiro `ColetorApplication.java` e depois `ServidorWebApplication.java`.
4. O banco e o arquivo `.env` continuam sendo necessários da mesma forma.
