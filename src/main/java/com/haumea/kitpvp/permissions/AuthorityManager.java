package com.haumea.kitpvp.permissions;

import com.haumea.kitpvp.HaumeaMC;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gerenciador Central de Autoridade do HaumeaMC
 * 
 * Esta classe é responsável por "sequestrar" o gerenciador de permissões
 * padrão do Bukkit e substituir pelo nosso sistema customizado.
 * 
 * FUNCIONALIDADES PRINCIPAIS:
 * 1. Injetar HaumeaPermissible em todos os jogadores
 * 2. Desativar o poder do /op (isOp() sempre retorna false)
 * 3. DONO recebe permissão * (tudo)
 * 4. Fallback para OP real em permissões vanilla não configuradas
 * 
 * @author HaumeaMC
 */
public class AuthorityManager {

    private final HaumeaMC plugin;

    // Cache de HaumeaPermissible por UUID
    private final Map<UUID, HaumeaPermissible> permissibleCache;

    // Cache de PermissibleBase original por UUID (para restauração)
    private final Map<UUID, PermissibleBase> originalPermissibles;

    // Campo "perm" da classe CraftHumanEntity (reflexão)
    private Field permField;
    private Field modifiersField;
    private boolean reflectionInitialized;
    private boolean reflectionFailed;

    public AuthorityManager(HaumeaMC plugin) {
        this.plugin = plugin;
        this.permissibleCache = new ConcurrentHashMap<>();
        this.originalPermissibles = new ConcurrentHashMap<>();
        this.reflectionInitialized = false;
        this.reflectionFailed = false;

        initReflection();
    }

    /**
     * Inicializa a reflexão para acessar o campo "perm" do jogador.
     * 
     * Este é o campo que contém o PermissibleBase que precisamos substituir.
     */
    private void initReflection() {
        try {
            // Obter a classe CraftHumanEntity
            String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            String className = "org.bukkit.craftbukkit." + version + ".entity.CraftHumanEntity";
            Class<?> craftHumanEntity = Class.forName(className);

            // Obter o campo "perm"
            permField = craftHumanEntity.getDeclaredField("perm");
            permField.setAccessible(true);

            // Tentar obter o campo modifiers para remover o modificador final
            try {
                modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                // Remover o modificador FINAL do campo perm
                modifiersField.setInt(permField, permField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            } catch (NoSuchFieldException e) {
                // Java 12+ removeu o acesso ao campo modifiers
                // Vamos tentar sem remover o final
                plugin.getLogger()
                        .warning("§eAuthorityManager: Não foi possível remover modificador final (Java 12+?)");
            }

            reflectionInitialized = true;
            plugin.getLogger().info("§aAuthorityManager: Reflexão inicializada com sucesso!");

        } catch (ClassNotFoundException e) {
            plugin.getLogger().warning("§cAuthorityManager: Classe CraftHumanEntity não encontrada!");
            reflectionFailed = true;
        } catch (NoSuchFieldException e) {
            plugin.getLogger().warning("§cAuthorityManager: Campo 'perm' não encontrado!");
            reflectionFailed = true;
        } catch (Exception e) {
            plugin.getLogger().warning("§cAuthorityManager: Erro ao inicializar reflexão: " + e.getMessage());
            e.printStackTrace();
            reflectionFailed = true;
        }
    }

    /**
     * Injeta o HaumeaPermissible em um jogador, substituindo o gerenciador padrão.
     * 
     * @param player O jogador que receberá o novo gerenciador
     * @return true se a injeção foi bem-sucedida
     */
    public boolean injectPermissible(Player player) {
        if (reflectionFailed || !reflectionInitialized) {
            plugin.getLogger()
                    .warning("§cAuthorityManager: Reflexão não inicializada para " + player.getName());
            return false;
        }

        // Verificar se já foi injetado
        if (permissibleCache.containsKey(player.getUniqueId())) {
            return true; // Já está injetado
        }

        try {
            // Obter o PermissibleBase original
            Object originalObj = permField.get(player);

            if (!(originalObj instanceof PermissibleBase)) {
                plugin.getLogger()
                        .warning("§cAuthorityManager: Campo perm não é PermissibleBase para " + player.getName());
                return false;
            }

            PermissibleBase original = (PermissibleBase) originalObj;

            // Verificar se já é nosso HaumeaPermissible (evitar dupla injeção)
            if (original instanceof HaumeaPermissible) {
                permissibleCache.put(player.getUniqueId(), (HaumeaPermissible) original);
                return true;
            }

            // Guardar referência ao original
            originalPermissibles.put(player.getUniqueId(), original);

            // Criar nosso HaumeaPermissible
            HaumeaPermissible haumeaPerm = new HaumeaPermissible(player, original);

            // Injetar no jogador
            permField.set(player, haumeaPerm);

            // Adicionar ao cache
            permissibleCache.put(player.getUniqueId(), haumeaPerm);

            // Atualizar cache de permissões
            haumeaPerm.refreshCache();

            if (plugin.getConfigManager() != null && plugin.getConfigManager().isDebug()) {
                plugin.getLogger().info("§aAuthorityManager: Permissões injetadas em " + player.getName());
            }

            return true;

        } catch (IllegalAccessException e) {
            plugin.getLogger().warning("§cAuthorityManager: Acesso negado ao campo 'perm' para " + player.getName()
                    + " - Campo é final?");
            return false;
        } catch (Exception e) {
            plugin.getLogger().warning(
                    "§cAuthorityManager: Erro ao injetar permissões em " + player.getName() + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Remove o HaumeaPermissible e restaura o gerenciador original.
     * 
     * @param player O jogador que terá o gerenciador restaurado
     */
    public void ejectPermissible(Player player) {
        UUID uuid = player.getUniqueId();

        // Remover do cache
        permissibleCache.remove(uuid);

        // Restaurar PermissibleBase original
        PermissibleBase original = originalPermissibles.remove(uuid);

        if (original != null && reflectionInitialized && !reflectionFailed) {
            try {
                permField.set(player, original);
            } catch (Exception e) {
                // Ignorar - jogador está saindo mesmo
            }
        }
    }

    /**
     * Atualiza as permissões de um jogador específico.
     * 
     * @param player O jogador a ter permissões atualizadas
     */
    public void updatePermissions(Player player) {
        HaumeaPermissible perm = permissibleCache.get(player.getUniqueId());
        if (perm != null) {
            perm.forceRefresh();
        }
    }

    /**
     * Atualiza as permissões de um jogador por UUID
     * 
     * @param uuid UUID do jogador
     */
    public void updatePermissions(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null && player.isOnline()) {
            updatePermissions(player);
        }
    }

    /**
     * Atualiza as permissões de TODOS os jogadores online.
     */
    public void updateAllPermissions() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePermissions(player);
        }
    }

    /**
     * Verifica se um jogador tem nosso gerenciador de permissões injetado.
     * 
     * @param player O jogador a verificar
     * @return true se o HaumeaPermissible está ativo
     */
    public boolean hasInjectedPermissible(Player player) {
        return permissibleCache.containsKey(player.getUniqueId());
    }

    /**
     * Obtém o HaumeaPermissible de um jogador.
     * 
     * @param player O jogador
     * @return O HaumeaPermissible ou null se não injetado
     */
    public HaumeaPermissible getPermissible(Player player) {
        return permissibleCache.get(player.getUniqueId());
    }

    /**
     * Obtém o HaumeaPermissible por UUID.
     * 
     * @param uuid UUID do jogador
     * @return O HaumeaPermissible ou null se não injetado
     */
    public HaumeaPermissible getPermissible(UUID uuid) {
        return permissibleCache.get(uuid);
    }

    /**
     * Verifica se a reflexão está funcionando corretamente.
     * 
     * @return true se podemos injetar permissões
     */
    public boolean isReflectionWorking() {
        return reflectionInitialized && !reflectionFailed;
    }

    /**
     * Limpa todos os caches e restaura permissões originais.
     * 
     * Chamado no onDisable do plugin.
     */
    public void shutdown() {
        // Restaurar todas as permissões originais
        for (Player player : Bukkit.getOnlinePlayers()) {
            ejectPermissible(player);
        }

        // Limpar caches
        permissibleCache.clear();
        originalPermissibles.clear();
    }

    /**
     * Obtém estatísticas do sistema de autoridade.
     * 
     * @return Informações de debug
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("§6=== AuthorityManager Debug ===\n");
        sb.append("§fReflexão ativa: ").append(reflectionInitialized && !reflectionFailed ? "§aSIM" : "§cNÃO")
                .append("\n");
        sb.append("§fJogadores injetados: §e").append(permissibleCache.size()).append("\n");
        sb.append("§fPermissibles originais: §e").append(originalPermissibles.size()).append("\n");

        for (Map.Entry<UUID, HaumeaPermissible> entry : permissibleCache.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                HaumeaPermissible perm = entry.getValue();
                sb.append("§f- ").append(player.getName()).append(": §e").append(perm.getCachedPermissions().size())
                        .append(" permissões em cache\n");
            }
        }

        return sb.toString();
    }
}
