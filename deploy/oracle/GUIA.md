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
   - OCPU: 2; memória: 12 GB;
   - rede: criar uma VCN nova;
   - SSH key: cole uma chave pública sua e guarde a privada fora do Git.
5. Crie a instância e anote o IPv4 público.

Se o shape A1 estiver sem capacidade, tente outra região/availability domain ou
use uma VM AMD Always Free menor. Não habilite recursos que apareçam como pagos.

## 2. Liberar o acesso HTTP

Na VCN, abra a subnet da instância, depois **Security List > Ingress Rules** e
adicione TCP `8080` de `0.0.0.0/0`. No servidor, depois do primeiro acesso SSH:

```bash
sudo ufw allow OpenSSH
sudo ufw allow 8080/tcp
sudo ufw enable
```

O primeiro teste será `http://IP_PUBLICO:8080`. Depois podemos colocar Nginx e
HTTPS na porta 443.

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
cp deploy/oracle/.env.oracle.exemplo deploy/oracle/.env.oracle
nano deploy/oracle/.env.oracle
```

Troque as duas senhas do exemplo. Nunca publique esse arquivo.

## 4. Subir os três serviços

```bash
docker compose --env-file deploy/oracle/.env.oracle -f deploy/oracle/docker-compose.yml up -d --build
docker compose --env-file deploy/oracle/.env.oracle -f deploy/oracle/docker-compose.yml ps
docker compose --env-file deploy/oracle/.env.oracle -f deploy/oracle/docker-compose.yml logs -f webapp
```

Abra `http://IP_PUBLICO:8080`. O schema é aplicado automaticamente somente na
primeira criação do volume MySQL. O volume `mysql-data` mantém o banco durante
reinícios e novos deploys.

## 5. Atualizar depois de um push

```bash
cd aps-2026-02-estacao-meteorologica
git pull --ff-only origin feature/probabilidade-chuva-horaria
docker compose --env-file deploy/oracle/.env.oracle -f deploy/oracle/docker-compose.yml up -d --build
```

## 6. Comandos úteis

```bash
docker compose --env-file deploy/oracle/.env.oracle -f deploy/oracle/docker-compose.yml logs -f collector
docker compose --env-file deploy/oracle/.env.oracle -f deploy/oracle/docker-compose.yml restart collector
docker compose --env-file deploy/oracle/.env.oracle -f deploy/oracle/docker-compose.yml down
```

Não use `down -v`: isso removeria o volume do MySQL e os dados.
