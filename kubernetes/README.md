# Projeto no Kubernetes local

Use o cluster kubeadm do Docker Desktop. As quatro imagens Java usam as tags referenciadas nos manifestos. O script reconstruir-tudo gera uma tag nova por execucao.

## Iniciar

Na pasta distribuidos3:

```powershell
.\kubernetes\iniciar-kubernetes.cmd
```

O script aplica e espera cada etapa: Config Server, tres Eureka juntos, Redis, PostgreSQL, Alpha, Gateway e Prometheus. Se uma etapa falhar ou exceder 300 segundos, ele para sem iniciar as seguintes. Nao exclui recursos nem volumes existentes. Executar novamente aplica as mesmas definicoes.

O script ordena a implantacao inicial. Se ja houver recursos em execucao, eles continuam funcionando; o script nao os desliga para recomecar a ordem. Init containers fazem cada novo Pod Java aguardar as portas dos servicos de que depende. Os Services Java so encaminham conexoes aos Pods aprovados pelas verificacoes de readiness. Essas esperas nao garantem disponibilidade permanente; os clientes ainda precisam tratar falhas depois de iniciar.

## Acessar

O Service api-gateway usa NodePort 30080, acessivel no Docker Desktop kubeadm pelo computador sem executar port-forward:

```powershell
curl.exe http://127.0.0.1:30080/service-alpha/teste
curl.exe http://127.0.0.1:30080/service-alpha/funcionarios
```

O caminho e computador:30080 -> Service:8080 -> Gateway:8080. A porta e mantida pelo Kubernetes enquanto o cluster estiver ativo. O Eureka 1 tambem e publicado na porta 30761. Config Server, Alpha, Redis, PostgreSQL e os Eureka 2 e 3 permanecem internos. Conforme o firewall e a rede do Docker Desktop, a porta publicada tambem pode ser acessivel por outros computadores da rede.

O primeiro acesso pode depender do tempo de propagacao do registro do Alpha ao cache Eureka do Gateway. Aguarde alguns segundos e repita se a rota ainda nao estiver disponivel.

Painel do Eureka 1, sem port-forward: http://127.0.0.1:30761 . O caminho e computador:30761 -> Service:8761 -> Eureka:8761. Esse NodePort tambem pode ser acessivel na rede do computador, conforme o firewall. O laboratorio ainda nao configura autenticacao do Eureka.

Para acessar somente o Config Server, use outra porta, sem conflito com o Windows:

```powershell
kubectl --context docker-desktop -n distribuidos3-lab port-forward --address 127.0.0.1 service/config-server 18888:8888
```

Confirme a busca no Git sem imprimir os valores das propriedades:

```powershell
curl.exe -f -o NUL http://localhost:18888/service-alpha/default
```

## Banco e enderecos

- PostgreSQL: imagem oficial postgres:17-alpine, banco distribuidos3, usuario postgres, sem senha (trust), somente para o laboratorio local. O Service nao publica a porta no computador, mas qualquer Pod com acesso de rede pode conectar sem senha.
- StatefulSet postgres: uma instancia. O PVC dados-postgres-0 reserva 5Gi na StorageClass padrao do Docker Desktop. O volume sobrevive a substituicao do Pod; ele nao substitui backups e pode ser perdido se o volume ou cluster for excluido.
- O banco e novo, separado dos dados do Docker Compose. As migracoes Flyway do Alpha criam as tabelas no primeiro inicio.
- Redis: guarda apenas o estado temporario do exercicio e nao tem volume persistente. Substituir seu Pod reinicia os contadores do rate limit.
- Config Server: http://config-server:8888
- Eureka: http://discovery-1:8761/eureka/, http://discovery-2:8762/eureka/, http://discovery-3:8763/eureka/
- Redis: redis:6379
- PostgreSQL: jdbc:postgresql://postgres:5432/distribuidos3
- Alpha: http://service-alpha:8090

O ConfigMap config-client-bootstrap fornece o nome da aplicacao e a importacao do Config Server aos clientes. SPRING_CONFIG_LOCATION aponta para esse arquivo montado no Pod, evitando carregar a importacao localhost embutida na imagem.

SPRING_APPLICATION_JSON nos manifestos substitui apenas os enderecos e valores necessarios para o cluster. Os application.properties/yaml locais e o repositorio Git nao sao alterados. O Config Server continua lendo o Git remoto configurado; editar a pasta configuration local sem publicar no Git nao atualiza esse servidor.

Nao e preciso um Dockerfile para Redis/PostgreSQL: usamos as imagens oficiais prontas. Discovery usa uma unica imagem para os tres perfis server1, server2 e server3.

## Diagnostico

```powershell
kubectl --context docker-desktop -n distribuidos3-lab get pods,services,pvc
kubectl --context docker-desktop -n distribuidos3-lab logs deployment/service-alpha --tail=80
kubectl --context docker-desktop -n distribuidos3-lab logs deployment/api-gateway --tail=80
kubectl --context docker-desktop -n distribuidos3-lab logs deployment/service-alpha -c aguardar-dependencias
kubectl --context docker-desktop -n distribuidos3-lab describe pod postgres-0
```

A autenticacao das APIs, integracao com Vault e restricoes entre Pods ainda nao estao configuradas por estes arquivos.

Referencias:
- https://kubernetes.io/docs/concepts/workloads/pods/init-containers/
- https://kubernetes.io/docs/concepts/workloads/controllers/statefulset/
- https://hub.docker.com/_/postgres

## Metricas e Prometheus

Primeira ativacao ou atualizacao das imagens com Micrometer (na pasta do projeto):

```powershell
.\kubernetes\atualizar-metricas.cmd
```

O comando atualizar-metricas foi mantido como atalho para reconstruir-tudo. Ele agora constroi as quatro aplicacoes Java e somente depois inicia a atualizacao do cluster.

O Actuator ja existia. Foi adicionada a dependencia io.micrometer:micrometer-registry-prometheus com scope runtime, sem versao explicita, aos POMs de ApiGateway, discovery-server e service-alpha. O Config Server nao foi alterado.

Os manifestos desses clientes habilitam health e prometheus. Alpha tambem preserva refresh. Nao foi usado exposure.include=*. O formato das metricas fica em /actuator/prometheus. As tags application identificam o nome da aplicacao.

A configuracao da coleta esta em kubernetes/prometheus/prometheus.yml. O inicializador transforma esse arquivo no ConfigMap prometheus-config, montado em /etc/prometheus/prometheus.yml. O Prometheus consulta diretamente os Services internos a cada 15 segundos: Gateway, Alpha e as tres instancias Eureka. Tambem coleta suas proprias metricas. Config Server, Redis e PostgreSQL nao sao alvos desta configuracao Java; monitorar os dois ultimos exigiria exporters especificos.

Prometheus usa a imagem prom/prometheus:v3.15.1 e um volume de 5Gi, com retencao maxima de 7 dias ou 4GB. O volume sobrevive a substituicao do Pod, mas nao substitui backups nem garante preservacao se o armazenamento/cluster for excluido.

Para alterar os alvos: edite prometheus/prometheus.yml, execute o inicializador para atualizar o ConfigMap e reinicie o Prometheus para reler o arquivo:

```powershell
kubectl --context docker-desktop -n distribuidos3-lab rollout restart deployment/prometheus
kubectl --context docker-desktop -n distribuidos3-lab rollout status deployment/prometheus --timeout=300s
```

No painel Targets, aguarde ate 15 segundos para a primeira coleta. Todos os seis alvos devem aparecer UP (cinco instancias Java e o proprio Prometheus).

Consultas para experimentar:

```promql
up
sum by (application) (jvm_memory_used_bytes)
rate(http_server_requests_seconds_count[1m])
```

A coleta e do tipo pull: o Prometheus busca as metricas; as aplicacoes nao precisam enviar requisicoes para ele. O laboratorio ainda nao possui autenticacao dos endpoints de monitoramento.

Painel Prometheus: http://127.0.0.1:30900 . Alvos: http://127.0.0.1:30900/targets . O NodePort 30900 foi publicado para acesso sem port-forward; conforme o firewall, a rede local tambem pode acessa-lo.

## Reconstruir todas as aplicacoes

Na pasta distribuidos3:

```powershell
.\kubernetes\reconstruir-tudo.cmd
```

O script constroi Config Server, Discovery, Alpha e Gateway com --no-cache. Gera uma tag nova (build-data-hora-identificador), valida os quatro manifestos e so altera suas referencias de imagem se todas as construcoes terminarem com sucesso. A mesma imagem Discovery e usada nos tres Eureka.

Depois chama o inicializador, que atualiza e aguarda cada etapa. A mudanca da tag faz o Kubernetes criar os novos Pods. Se um rollout falhar, as etapas seguintes param; as etapas ja atualizadas nao sao revertidas automaticamente. Consulte os logs e execute novamente depois da correcao.

As imagens oficiais de Redis, PostgreSQL, Prometheus e BusyBox continuam nas versoes dos manifestos. Seus volumes nao sao excluidos, e o script nao executa docker system prune, docker compose down -v nem reset do cluster. Reconstruir nao e backup: nao exclua os PVCs se quiser manter os dados.

As dependencias podem ser reaproveitadas pelo cache Maven do Dockerfile; --no-cache impede o reaproveitamento das etapas de construcao da imagem. A imagem final continua contendo apenas Java e o JAR. O Config Server e reconstruido, mas continua fora da coleta de metricas do Prometheus.