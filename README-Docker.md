# Projeto completo no Docker Compose (teste local)

O Compose executa Config Server, tres Eureka, Redis, PostgreSQL, Service Alpha, API Gateway e Prometheus. As imagens Java usam os Dockerfiles existentes, com Maven na construcao e Java 21 JRE na execucao.

## Iniciar

Abra o Docker Desktop, aguarde o mecanismo iniciar e execute:

```cmd
cd C:\Users\claud\Downloads\distribuidos3
.\iniciar-docker.cmd
```

O script valida o Compose, constroi quatro imagens e executa up com --wait. As imagens discovery-2 e discovery-3 reutilizam a imagem construida por discovery-1. Nao e preciso executar Kubernetes.

No Linux/VPS, os comandos equivalentes sao:

```sh
docker compose build config-server discovery-1 service-alpha api-gateway
docker compose up -d --no-build --wait --wait-timeout 600
```

Este arquivo ainda esta configurado para desenvolvimento local. Antes de usar na VPS, ajuste autenticacao do banco e das APIs, HTTPS, backups e exposicao de portas. As portas publicadas estao limitadas a 127.0.0.1 e o banco continua sem senha, conforme o ambiente local anterior. Elas nao ficam automaticamente acessiveis pela internet.

## Ordem e saude

```text
Config Server saudavel
        |
Tres Eureka saudaveis (iniciam juntos)
        |
Redis e PostgreSQL saudaveis (podem iniciar juntos)
        |
Service Alpha saudavel
        |
Gateway saudavel (tambem aguarda o Redis)
        |
Prometheus
```

depends_on com condition: service_healthy espera o healthcheck da dependencia. Nao ha dependencia circular entre os tres Eureka. As verificacoes Java consultam o Actuator readiness com curl, que ja existe na base Ubuntu do Eclipse Temurin.

A readiness do Config Server verifica o repositorio Git; a do Alpha inclui banco; a do Gateway inclui Redis. Se a dependencia nao ficar saudavel, os dependentes nao iniciam. O script pode encerrar com erro ou exceder 600 segundos; os containers que ja iniciaram permanecem disponiveis para diagnostico.

Essa ordem vale para a inicializacao feita pelo Compose. O reinicio automatico pelo Docker usa restart: unless-stopped e nao reexecuta todo esse fluxo de dependencia; as aplicacoes precisam continuar tratando falhas de conexao durante a execucao.

## Enderecos

- Gateway: http://127.0.0.1:30080/service-alpha/teste
- CRUD: http://127.0.0.1:30080/service-alpha/funcionarios
- Refresh do Alpha: POST http://127.0.0.1:30080/service-alpha/actuator/refresh
- Eureka 1: http://127.0.0.1:30761
- Config Server: http://127.0.0.1:8888/service-alpha/default
- Prometheus: http://127.0.0.1:30900/targets
- PostgreSQL local: 127.0.0.1:5432, banco distribuidos3, usuario postgres, sem senha.
- Redis local: 127.0.0.1:6379.

Alpha e Eureka 2/3 nao publicam portas no computador. Eles sao acessados pelo nome do servico na rede Docker. O Grafana que voce ja instalou pode continuar usando http://127.0.0.1:30900 como fonte Prometheus. Nao foi criada outra instancia de Grafana.

Se os recursos Kubernetes ainda estiverem ativos, pare-os antes para liberar as portas 30080, 30761 e 30900:

```sh
kubectl --context docker-desktop -n distribuidos3-lab scale deployment,statefulset --all --replicas=0
```

Esse comando nao exclui PVCs. O Compose nao para nem altera o Kubernetes automaticamente.

## Configuracao e metricas

docker/application-container.properties e montado com o recurso configs do Compose em /config/application.properties. SPRING_CONFIG_LOCATION faz os clientes usarem esse arquivo em vez da importacao localhost embutida na imagem. APPLICATION_NAME define o nome de cada cliente. Os application.properties/yaml do codigo nao foram alterados.

SPRING_APPLICATION_JSON no Compose fornece os enderecos internos do Eureka, Redis e PostgreSQL. Config Server continua lendo o Git remoto configurado, portanto o primeiro inicio e a atualizacao das configuracoes podem precisar de internet.

As metricas do Gateway, Alpha e dos tres Eureka ficam em /actuator/prometheus. Alpha preserva refresh. Config Server continua fora da coleta.

Edite docker/prometheus.yml para mudar os alvos de coleta. Ele e montado em /etc/prometheus/prometheus.yml no Prometheus. Depois de alterar esse arquivo:

```sh
docker compose restart prometheus
```

## Reconstruir e atualizar

Execute iniciar-docker.cmd novamente. Docker Compose compara a imagem construida e recria os containers Java cuja imagem mudou, preservando volumes. As tags :compose sao separadas das imagens do Kubernetes.

Para uma reconstrucao sem cache:

```sh
docker compose build --no-cache config-server discovery-1 service-alpha api-gateway
docker compose up -d --no-build --wait --wait-timeout 600
```

## Conferir e parar

```sh
docker compose ps
docker compose logs --tail 80 config-server
docker compose logs --tail 80 discovery-1
docker compose logs --tail 80 service-alpha
docker compose exec redis redis-cli ping
docker compose exec postgres psql -U postgres -d distribuidos3 -c "SELECT current_database();"
docker compose stop
```

parar-docker.cmd executa compose stop para todos os servicos deste Compose. Ele nao fecha o Grafana instalado separadamente no Windows.

## Dados

O nome do projeto continua distribuidos3 e os volumes redis-data e postgres-data foram mantidos. Isso permite reutilizar os volumes do Compose anterior, sem altera-los para recriar o banco.

Prometheus usa o novo volume prometheus-data, com retencao de 7 dias ou 4GB. Volumes do Kubernetes sao separados: esta mudanca nao copia os funcionarios nem o historico de metricas que estavam no cluster. Se houver dados importantes la, e necessario exportar e importar separadamente antes de abandonar esse ambiente.

Nao execute docker compose down -v nem remova volumes para apenas parar o projeto. Os limites de memoria e CPU do Compose foram definidos por container, como no laboratorio Kubernetes.

Referencia sobre a ordem: https://docs.docker.com/compose/how-tos/startup-order/
