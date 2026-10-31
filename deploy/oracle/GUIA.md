# Deploy na Oracle Cloud Always Free

Este diretório executa a aplicação completa em uma VM Ubuntu: MySQL persistente,
`webapp` (API + front-end) e `collector` (coleta contínua).

## 1. Criar a conta e a VM

1. Acesse <https://www.oracle.com/cloud/free/> e selecione **Sign Up**.
2. Escolha o país e conclua e-mail, telefone, cartão, CAPTCHA e MFA. A Oracle
   pode exigir cartão para validar a identidade; o cartão não deve ser cobrado
   enquanto os recursos permanecerem dentro do Always Free.
3. No console, abra **Compute > Instances > Create instance**.
4. Use estes valores:
   - nome: `estacao-meteorologica`;
   - imagem: Ubuntu 24.04 Minimal;
   - shape: `VM.Standard.A1.Flex` (Ampere ARM), dentro do limite Always Free;
   - OCPU: 1; memória: 4 GB;
   - rede: criar uma VCN nova;
   - SSH key: cole uma chave pública sua e guarde a privada fora do Git.
5. Crie a instância e anote o IPv4 público.

Essa configuração é suficiente para a demonstração: o webapp usa no máximo
512 MB, o collector 384 MB e o MySQL foi limitado a 256 MB de buffer. Se o
shape A1 estiver sem capacidade, tente outra região/availability domain. Não
habilite recursos que apareçam como pagos.

## 2. Acesso seguro com HTTPS

O acesso público final não usa a porta `8080`: ela é interna entre o Caddy e o
webapp. Na VCN, abra a subnet da instância, depois **Security List > Ingress
Rules**, e adicione estas regras **stateless: No**:

- TCP `80` de `0.0.0.0/0` (redirecionamento HTTP para HTTPS e emissão do certificado);
- TCP `443` de `0.0.0.0/0` (site HTTPS).

Não habilite UFW neste deploy: portas publicadas pelo Docker podem contornar
suas regras. O bloqueio é feito pela Security List da Oracle e pelo Compose,
que não publica MySQL, o coletor ou o webapp diretamente.

### DNS e certificado

No provedor responsável por `normaflowapp.com.br`, crie um registro DNS:

| Tipo | Nome/host | Valor |
| --- | --- | --- |
| A | `meteora` | IPv4 público atual da VM Oracle |

Espere a propagação e confirme na VM (o resultado deve ser o IPv4 público da
VM):

```bash
getent ahostsv4 meteora.normaflowapp.com.br
```

O Caddy solicitará e renovará o certificado TLS automaticamente. Certificados
confiáveis não podem ser emitidos para um IPv4 puro; o domínio é indispensável.
O proxy consulta o DNS interno do Docker periodicamente; assim um reinício do
`webapp` não deixa o endereço público preso a um IP antigo do contêiner.

## 3. Instalar Docker e baixar o projeto

```bash
sudo apt update
sudo apt install -y git docker.io docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker "$USER"
exit
```

Entre novamente por SSH e execute:

```bash
git clone https://github.com/trabalhos-unip/aps-2026-02-estacao-meteorologica.git
cd aps-2026-02-estacao-meteorologica
cp deploy/oracle/env.oracle.exemplo deploy/oracle/.env.oracle
nano deploy/oracle/.env.oracle
```

Troque as duas senhas do exemplo. Nunca publique esse arquivo.
Também mantenha `CADDY_DOMAIN=meteora.normaflowapp.com.br`.

### VM AMD Micro de 1 GB

Se `free -h` mostrar aproximadamente 1 GB de memória, como na
`VM.Standard.E2.1.Micro`, crie antes uma swap de 2 GB para evitar que uma
compilação ou um pico momentâneo encerre um processo Java:

```bash
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
```

Use também a sobreposição `docker-compose.micro.yml`, que reduz os limites da
JVM e do MySQL e reserva pools de conexão pequenos para o webapp e o coletor.
Ela é adequada para demonstração e baixo tráfego; para uso
contínuo com folga, prefira a A1 com 4 GB.

## 4. Subir os serviços com HTTPS

```bash
docker compose --env-file deploy/oracle/.env.oracle \
  -f deploy/oracle/docker-compose.yml \
  -f deploy/oracle/docker-compose.https.yml up -d --build
docker compose --env-file deploy/oracle/.env.oracle \
  -f deploy/oracle/docker-compose.yml \
  -f deploy/oracle/docker-compose.https.yml ps
```

Em uma VM Micro de 1 GB, substitua o primeiro comando por:

```bash
docker compose --env-file deploy/oracle/.env.oracle \
  -f deploy/oracle/docker-compose.yml \
  -f deploy/oracle/docker-compose.micro.yml \
  -f deploy/oracle/docker-compose.https.yml up -d --build
```

Abra `https://meteora.normaflowapp.com.br`. O schema é aplicado automaticamente
somente na primeira criação do volume MySQL. O volume `mysql-data` mantém o
banco durante reinícios e novos deploys.

Depois de confirmar que o endereço HTTPS está funcionando, remova da Security
List a regra pública TCP `8080`. Para proteger também o SSH, primeiro crie uma
regra TCP `22` apenas para o IP público da sua rede (`SEU_IP/32`), teste uma
nova sessão SSH e só então remova a regra TCP `22` de `0.0.0.0/0`.

## 5. Atualizar depois de um push

```bash
cd aps-2026-02-estacao-meteorologica
git pull --ff-only origin feature/probabilidade-chuva-horaria
docker compose --env-file deploy/oracle/.env.oracle \
  -f deploy/oracle/docker-compose.yml \
  -f deploy/oracle/docker-compose.micro.yml \
  -f deploy/oracle/docker-compose.https.yml up -d --build
```

## 6. Comandos úteis

```bash
docker compose --env-file deploy/oracle/.env.oracle -f deploy/oracle/docker-compose.yml -f deploy/oracle/docker-compose.micro.yml -f deploy/oracle/docker-compose.https.yml logs -f caddy
docker compose --env-file deploy/oracle/.env.oracle -f deploy/oracle/docker-compose.yml -f deploy/oracle/docker-compose.micro.yml -f deploy/oracle/docker-compose.https.yml logs -f collector
docker compose --env-file deploy/oracle/.env.oracle -f deploy/oracle/docker-compose.yml -f deploy/oracle/docker-compose.micro.yml -f deploy/oracle/docker-compose.https.yml down
```

Não use `down -v`: isso removeria o volume do MySQL e os dados.

### Consultar o banco com segurança

O MySQL não possui porta pública. Ele é publicado apenas no `127.0.0.1` da VM,
para acesso administrativo por um túnel SSH. No PowerShell do computador local,
mantenha este comando aberto durante o uso do MySQL Workbench:

```powershell
ssh -N -L 3307:127.0.0.1:3306 -i "$env:USERPROFILE\.ssh\estacao-oracle" ubuntu@IP_PUBLICO_DA_VM
```

No MySQL Workbench crie uma conexão **Standard TCP/IP** com host `127.0.0.1`,
porta `3307`, usuário `aps` e a senha `MYSQL_PASSWORD` do arquivo
`deploy/oracle/.env.oracle`. O schema é `estacao_meteorologica`.

Não crie uma regra Oracle para a porta `3306`: o túnel usa a conexão SSH já
protegida e mantém o banco invisível para a internet.

## 7. Deploy automático após `push` (GitHub Actions)

O workflow `.github/workflows/deploy-oracle.yml` só roda quando a variável do
repositório `ORACLE_DEPLOY_ENABLED` for `true`. Antes de habilitá-la, crie uma
chave SSH exclusiva para a automação — **não reutilize sua chave pessoal**.

No PowerShell do seu computador:

```powershell
ssh-keygen -t ed25519 -f "$env:USERPROFILE\.ssh\meteora-github-actions" -C "github-actions-meteora"
Get-Content "$env:USERPROFILE\.ssh\meteora-github-actions.pub"
```

Na VM, cole a chave pública exibida como uma nova linha em
`~/.ssh/authorized_keys`, precedida por estas restrições (a chave só poderá
executar o script de atualização, sem terminal ou encaminhamento de portas):

```text
command="/home/ubuntu/aps-2026-02-estacao-meteorologica/deploy/oracle/atualizar.sh",no-port-forwarding,no-agent-forwarding,no-X11-forwarding,no-pty COLE_AQUI_A_CHAVE_PUBLICA_COMPLETA
```

Na página do GitHub do repositório, abra **Settings > Secrets and variables >
Actions** e crie estes *Repository secrets*:

| Secret | Conteúdo |
| --- | --- |
| `ORACLE_SSH_PRIVATE_KEY` | conteúdo completo do arquivo privado `meteora-github-actions` |
| `ORACLE_KNOWN_HOSTS` | saída de `ssh-keyscan -H IP_PUBLICO_DA_VM` |
| `ORACLE_HOST` | IPv4 público da VM |
| `ORACLE_USER` | `ubuntu` |

Por fim, em **Variables**, crie `ORACLE_DEPLOY_ENABLED` com o valor `true`.
Após isso, cada push na branch `feature/probabilidade-chuva-horaria` dispara a
atualização. O script detecta os arquivos do push: alteração só no `webapp`
recompila apenas o webapp; alteração só no `collector` recompila somente o
coletor; alteração no módulo `compartilhado` recompila ambos; documentação não
reinicia nada. A compilação do módulo `compartilhado` fica em cache e os testes
não são compilados durante o deploy, reduzindo o tempo de alterações apenas no
front-end. Acompanhe o resultado na aba **Actions** do GitHub. Nunca coloque a
chave privada, senhas MySQL ou o arquivo `.env.oracle` no Git.
