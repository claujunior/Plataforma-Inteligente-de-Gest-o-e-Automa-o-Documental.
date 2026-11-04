# Redis e PostgreSQL no Docker

O Compose inicia Redis e PostgreSQL. Config Server, Eureka, Service Alpha e
API Gateway rodam em Java pela IDE ou pelo iniciar-tudo.cmd.

## Iniciar

Abra o Docker Desktop e execute iniciar-docker.cmd na pasta do projeto.
O comando equivalente e:

```powershell
docker compose up -d --wait --wait-timeout 120 redis postgres
```

O Redis fica em 127.0.0.1:6379. Para conectar ao PostgreSQL pelo Windows:

```properties
spring.datasource.url=jdbc:postgresql://127.0.0.1:5432/distribuidos3
spring.datasource.username=postgres
spring.datasource.password=
```

O PostgreSQL usa autenticacao trust para desenvolvimento local e publica
a porta somente no endereco de loopback do Windows. O volume postgres-data
guarda o banco entre reinicios.

## Conferir

```powershell
docker compose ps
docker compose exec redis redis-cli ping
docker compose exec postgres psql -h 127.0.0.1 -U postgres -d distribuidos3 -w -c "SELECT current_database();"
```

O Redis deve responder PONG. A consulta deve retornar distribuidos3.

## Parar

Execute parar-docker.cmd ou:

```powershell
docker compose stop redis postgres
```

Os volumes redis-data e postgres-data continuam disponiveis para a proxima
execucao. Pare as aplicacoes Java separadamente.
