# Referências — fontes técnicas usadas no desenvolvimento

Lista de apoio para a seção de referências da monografia. Formatar conforme o Guia de Normalização da UNIP (ABNT)
e registrar a data de acesso de cada fonte. Os artigos acadêmicos (mínimo de dois) e as fontes teóricas
(estações meteorológicas, sensores, órgãos nacionais) são adicionados pela equipe da monografia.

## API meteorológica

- OPEN-METEO. **Weather Forecast API**: documentação. Disponível em: <https://open-meteo.com/en/docs>.
  Usada para: parâmetros `current`, unidades, resolução de 15 minutos e tabela de códigos WMO (`weather_code`).
- OPEN-METEO. **Terms of use**. Disponível em: <https://open-meteo.com/en/terms>.
  Usada para: limites de uso da API gratuita e atribuição exigida pela licença.

## Tecnologias

- ORACLE. **Java Platform, Standard Edition 21 — API Specification**. Disponível em:
  <https://docs.oracle.com/en/java/javase/21/docs/api/>. Usada para: `java.net.http.HttpClient`, `java.time`.
- HIBERNATE. **Hibernate ORM — User Guide**. Disponível em: <https://hibernate.org/orm/documentation/>.
  Usada para: `Configuration`, `SessionFactory`, `Session`, `Transaction`, HQL e `hibernate.cfg.xml`.
- SPRING. **Spring Boot — Reference Documentation**. Disponível em: <https://docs.spring.io/spring-boot/>.
  Usada para: inicialização das aplicações, configuração externa (`application.properties`), agendamento (`@Scheduled`),
  servidor web embutido e registro de Servlet.
- ECLIPSE FOUNDATION. **Jakarta Servlet Specification**. Disponível em: <https://jakarta.ee/specifications/servlet/>.
  Usada para: `HttpServlet`.
- ORACLE. **MySQL 8.4 Reference Manual**. Disponível em: <https://dev.mysql.com/doc/refman/8.4/en/>.
  Usada para: tipos de dados e criação do schema.
- FASTERXML. **Jackson Databind**. Disponível em: <https://github.com/FasterXML/jackson-databind>.
  Usada para: leitura e geração de JSON.

## Padrões de projeto

- ORACLE. **Core J2EE Patterns — Data Access Object**. Disponível em:
  <https://www.oracle.com/java/technologies/dataaccessobject.html>. Usada para: padrão DAO.
