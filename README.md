# HaumeaMC

Uma base completa de KitPvP e lobby para servidores Minecraft 1.8.8, com combate, progressão, moderação e persistência reunidos em um único plugin.

O HaumeaMC foi construído para operar em dois perfis. Em `KITPVP`, carrega a experiência de combate completa; em `LOBBY`, mantém os sistemas essenciais e adiciona seleção de servidores e NPCs. Os dados persistentes usam MongoDB.

## Principais sistemas

### Combate e progressão

- kits primários, secundários e habilidades com raridades;
- sopas, controle de dano, killstreaks e recompensas;
- ligas, ranking por Elo, estatísticas e multiplicadores;
- duelos 1v1, arenas e fila;
- desafios diários, conquistas e bounty;
- eventos, Feast e eventos interativos no chat;
- cassino com slots, blackjack, roleta, coinflip e crash;
- loja de kits, VIPs, multiplicadores e reset de estatísticas.

### Comunidade

- tags, grupos e permissões próprias;
- medalhas, cosméticos, skins e fake nick;
- mensagens privadas, ignore, chat de staff e broadcast;
- scoreboards, tablist, nametags e boss bar;
- warps, spawn protegido e seletor de servidores;
- NPCs clicáveis no lobby quando o ProtocolLib está disponível.

### Administração

- banimentos, mutes, avisos e expulsões;
- reports com menu;
- modo administrador, inspeção e logs administrativos;
- comandos de teleporte, inventário, gamemode, velocidade e limpeza;
- anti-flood e bloqueio de comandos;
- gerenciamento de chaves VIP, warps, grupos, eventos e duelos.

## Modos de servidor

Defina o perfil em `plugins/HaumeaPVP/config.yml`:

```yaml
server-type: KITPVP
```

Valores implementados:

- `KITPVP`: carrega os sistemas de combate, kits, duelos, eventos, Feast, cassino, cosméticos e recursos comuns;
- `LOBBY`: carrega recursos comuns, seletor de servidores, proteções e NPCs, sem os listeners de combate.

Os nomes de servidores configurados no lobby precisam corresponder ao proxy da rede.

## Tecnologias e dependências

- Java 8
- Maven
- Spigot API 1.8.8
- MongoDB Java Driver 3.12.14, incluído e realocado no JAR final
- ProtocolLib 4.8.0 como dependência opcional em execução

## Pré-requisitos

- JDK 8;
- Maven 3;
- servidor compatível com Spigot 1.8.8;
- MongoDB acessível pelo servidor;
- ProtocolLib para recursos que dependem de pacotes, como NPCs e integração de identidade premium;
- uma rede BungeeCord/Velocity quando usar seleção e transferência entre servidores.

> Este código mira especificamente a API e o comportamento do Minecraft 1.8.8. Versões modernas não são uma substituição direta.

## Compilação

```bash
git clone https://github.com/riique/HaumeaMC.git
cd HaumeaMC
mvn clean package
```

O Maven Shade gera o JAR em `target/` e incorpora o driver MongoDB com os pacotes realocados.

## Instalação

1. Pare o servidor.
2. Copie o JAR gerado para `plugins/`.
3. Instale o ProtocolLib se utilizar os recursos correspondentes.
4. Inicie o servidor uma vez para gerar os arquivos.
5. Pare o servidor e revise `config.yml`, `groups.yml`, `kits.yml` e `messages.yml`.
6. Configure o MongoDB.
7. Reinicie e verifique o console.

Exemplo local sem autenticação:

```yaml
mongodb:
  enabled: true
  host: "localhost"
  port: 27017
  database: "haumeamc"
  username: ""
  password: ""
  auth-database: "admin"
```

Em produção, use autenticação, restrinja a rede e não publique credenciais.

## Arquivos de configuração

| Arquivo | Conteúdo |
| --- | --- |
| `config.yml` | modo do servidor, lobby, MongoDB, spawn, tags, cooldowns, anti-flood e cassino |
| `groups.yml` | grupos, hierarquia e permissões |
| `kits.yml` | kits, itens e parâmetros de habilidades |
| `messages.yml` | textos exibidos aos jogadores |
| `plugin.yml` | comandos, aliases e permissões registradas |

## Comandos

O `plugin.yml` é a referência completa. Entre os grupos de comandos implementados estão:

- jogador: `/kit`, `/conta`, `/ranking`, `/tag`, `/medalha`, `/skin`, `/warp`, `/spawn`;
- social: `/tell`, `/ignore`, `/pay`, `/trade`;
- combate: `/duel`, `/evento`, `/feast`, `/bounty`, `/multiplicador`;
- moderação: `/ban`, `/mute`, `/warn`, `/kick`, `/report`, `/reports`;
- administração: `/admin`, `/build`, `/fly`, `/gamemode`, `/god`, `/tp`, `/tpall`;
- configuração: `/haumeawarp`, `/haumeaspawn`, `/haumeagroups`, `/dueladmin`, `/multadmin`, `/haumeastats`, `/haumeavip`, `/npc`.

Permissões e aliases variam por comando. Revise `src/main/resources/plugin.yml` antes de liberar acesso.

## Estrutura

```text
src/main/java/com/haumea/kitpvp/
├── abilities/     habilidades dos kits
├── commands/      comandos de jogador, staff e administração
├── database/      conexão e repositórios MongoDB
├── listeners/     eventos Bukkit e módulos por perfil de servidor
├── managers/      regras e ciclo de vida dos sistemas
├── menu/          inventários e interfaces interativas
├── models/        contas, partidas, punições e demais entidades
├── permissions/   autoridade e permissões dinâmicas
├── scoreboard/    placar lateral
└── tablist/       lista de jogadores
```

## Estado e limitações

- O repositório não contém testes automatizados; valide o JAR em um servidor de homologação.
- Vários sistemas dependem da configuração coerente de mundos, warps, arenas e permissões.
- Recursos de pacotes podem variar conforme a versão do ProtocolLib e o software do servidor.
- A integração entre servidores exige que os nomes e canais do proxy estejam corretos.
- Não use `/reload` como fluxo normal de atualização; prefira reiniciar o servidor após substituir o JAR.
- Faça backup do MongoDB e dos YAML antes de atualizar.

## Contribuição

Envie mudanças pequenas e descreva o perfil testado (`LOBBY` ou `KITPVP`), a versão do servidor e as dependências instaladas.

Antes de abrir o pull request:

```bash
mvn clean package
```

Não inclua mundos, bancos de dados, dados de jogadores ou credenciais.

## Licença

Distribuído sob a licença MIT. Consulte [LICENSE](LICENSE).
