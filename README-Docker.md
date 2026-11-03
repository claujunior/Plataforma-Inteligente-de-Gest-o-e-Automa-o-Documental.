# Somente Redis no Docker

O Compose executa apenas Redis. Config Server, os tres Eurekas,
Service Alpha e API Gateway devem ser executados em Java, pela IDE
ou pelo script anterior iniciar-tudo.cmd.

## Iniciar

1. Abra o Docker Desktop e aguarde ele iniciar.
2. Na pasta distribuidos3, execute iniciar-docker.cmd.
3. Depois, inicie as aplicacoes Java pela IDE ou pelo iniciar-tudo.cmd.

O comando equivalente, dentro da pasta do projeto, e:

```powershell
docker compose up -d --remove-orphans --wait --wait-timeout 60 redis
```

`--remove-orphans` encerra e remove os antigos conteineres Java deste
projeto Compose, agora ausentes do arquivo. O Redis e seu volume sao mantidos.

O Gateway executado no Windows conecta ao Redis usando:

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

Os arquivos de configuracao Java continuam usando os enderecos localhost.

## Conferir

```powershell
docker compose ps
docker compose exec redis redis-cli ping
```

O segundo comando deve responder PONG. O Redis fica disponivel
somente no computador local, na porta 6379.

## Parar

Execute parar-docker.cmd ou:

```powershell
docker compose stop redis
```

Isso para somente o Redis e preserva seu conteiner e volume.
Pare as aplicacoes Java separadamente, pela IDE ou pelo parar-tudo.cmd.
