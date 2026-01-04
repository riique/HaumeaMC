package com.haumea.kitpvp.permissions;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.GroupManager;
import com.haumea.kitpvp.models.Group;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementação customizada de Permissible que ESTENDE PermissibleBase.
 * 
 * Esta classe é o "filtro de segurança" entre o jogador e o servidor.
 * 
 * FUNCIONALIDADES:
 * 1. Intercepta TODAS as verificações de permissão
 * 2. SEMPRE retorna false para isOp() - desativa o poder do /op
 * 3. Consulta primeiro o banco de dados interno de cargos
 * 4. DONO recebe permissão * (tudo)
 * 5. Fallback: se permissão UNDEFINED, verifica OP real como último recurso
 * 
 * @author HaumeaMC
 */
public class HaumeaPermissible extends PermissibleBase {

    private final Player player;
    private final HaumeaMC plugin;
    private final PermissibleBase originalPermissible;

    // Cache de permissões para alta performance
    private final Set<String> cachedPermissions;
    private long cacheLastUpdate;
    private static final long CACHE_DURATION = 5000; // 5 segundos

    // Grupos com autoridade máxima (permissão *)
    private static final Set<String> AUTHORITY_GROUPS = new HashSet<>();
    static {
        AUTHORITY_GROUPS.add("dono");
    }

    /**
     * Construtor do HaumeaPermissible
     * 
     * @param player   O jogador que terá o gerenciador substituído
     * @param original O PermissibleBase original (para copiar attachments)
     */
    public HaumeaPermissible(Player player, PermissibleBase original) {
        // Passar o player como ServerOperator (mas vamos sobrescrever isOp())
        super(player);

        this.player = player;
        this.plugin = HaumeaMC.getInstance();
        this.originalPermissible = original;
        this.cachedPermissions = new HashSet<>();
        this.cacheLastUpdate = 0;

        // Copiar attachments do original para manter compatibilidade
        copyAttachments(original);
    }

    /**
     * Copia os attachments do PermissibleBase original para este.
     * Isso garante que permissões já configuradas não sejam perdidas.
     */
    private void copyAttachments(PermissibleBase original) {
        if (original == null)
            return;

        try {
            // Acessar o campo "attachments" do PermissibleBase original
            Field attachmentsField = PermissibleBase.class.getDeclaredField("attachments");
            attachmentsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<PermissionAttachment> originalAttachments = (List<PermissionAttachment>) attachmentsField
                    .get(original);

            List<PermissionAttachment> myAttachments = (List<PermissionAttachment>) attachmentsField.get(this);

            // Copiar attachments
            if (originalAttachments != null) {
                myAttachments.addAll(originalAttachments);
            }

        } catch (Exception e) {
            // Se falhar, não é crítico - apenas não terá os attachments antigos
            if (plugin != null) {
                plugin.getLogger()
                        .warning("§eHaumeaPermissible: Não foi possível copiar attachments: " + e.getMessage());
            }
        }
    }

    /**
     * PONTO CRÍTICO: Intercepta verificação de OP.
     * 
     * SEMPRE retorna FALSE para que ninguém receba
     * permissão total apenas por ter o nome na lista de OPs.
     * 
     * O status de "Operador" torna-se meramente decorativo.
     */
    @Override
    public boolean isOp() {
        // SEMPRE retorna false - desativa o poder do /op
        // O sistema interno de cargos é quem dita as regras
        return false;
    }

    /**
     * Define o status de OP (não faz nada útil no nosso sistema)
     */
    @Override
    public void setOp(boolean value) {
        // Não faz nada - o OP não tem poder no nosso sistema
        // Mantemos para compatibilidade, mas ignoramos
    }

    /**
     * PONTO CRÍTICO: Verificação principal de permissões.
     * 
     * Esta é a inteligência central do sistema:
     * 1. Primeiro verifica se é DONO (autoridade máxima = *)
     * 2. Verifica no cache de permissões do nosso sistema
     * 3. Verifica wildcards parciais
     * 4. FALLBACK: Se permissão UNDEFINED, verifica OP real como último recurso
     */
    @Override
    public boolean hasPermission(String permission) {
        if (permission == null || permission.isEmpty()) {
            return false;
        }

        // Atualizar cache se necessário
        updateCacheIfNeeded();

        // 1. Verificar autoridade máxima (cargo DONO = *)
        if (hasAuthorityGroup()) {
            return true;
        }

        String permLower = permission.toLowerCase();

        // 2. Verificar permissão NEGADA explicitamente
        if (cachedPermissions.contains("-" + permLower)) {
            return false;
        }

        // 3. Verificar permissão exata
        if (cachedPermissions.contains(permLower)) {
            return true;
        }

        // 4. Verificar wildcards parciais (ex: haumea.tag.*)
        if (checkWildcardPermission(permLower)) {
            return true;
        }

        // 5. Verificar no PermissibleBase pai (attachments)
        if (super.hasPermission(permission)) {
            return true;
        }

        // 6. FALLBACK: Se permissão UNDEFINED e é vanilla, verificar OP real
        if (shouldFallbackToOp(permLower)) {
            return isReallyOp();
        }

        // Nenhuma permissão encontrada
        return false;
    }

    /**
     * Verificação de permissão por objeto Permission
     */
    @Override
    public boolean hasPermission(Permission perm) {
        return hasPermission(perm.getName());
    }

    /**
     * Verifica se o jogador possui um dos grupos de autoridade máxima (DONO)
     * 
     * @return true se tem autoridade máxima
     */
    private boolean hasAuthorityGroup() {
        if (plugin == null)
            return false;

        GroupManager groupManager = plugin.getGroupManager();
        if (groupManager == null) {
            return false;
        }

        // Verificar todos os grupos do jogador
        for (String groupName : groupManager.getPlayerGroupNames(player.getUniqueId())) {
            if (AUTHORITY_GROUPS.contains(groupName.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Verifica wildcards parciais (ex: haumea.tag.*)
     */
    private boolean checkWildcardPermission(String permission) {
        // Verificar permissão global (*)
        if (cachedPermissions.contains("*")) {
            return true;
        }

        String[] parts = permission.split("\\.");
        StringBuilder check = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                check.append(".");
            }
            check.append(parts[i]);

            // Verificar se existe wildcard neste nível
            String wildcardPerm = check.toString() + ".*";
            if (cachedPermissions.contains(wildcardPerm)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determina se deve usar o fallback para OP real.
     * 
     * O fallback é usado para permissões que:
     * 1. NÃO são do nosso plugin (não começam com "haumea.")
     * 2. NÃO foram configuradas em nenhum grupo (UNDEFINED)
     * 3. São provavelmente permissões padrão do Minecraft/Bukkit
     * 
     * @param permission A permissão a verificar
     * @return true se deve usar fallback
     */
    private boolean shouldFallbackToOp(String permission) {
        // Se é uma permissão do nosso plugin, NUNCA usa fallback
        if (permission.startsWith("haumea.")) {
            return false;
        }

        // Se está definida no nosso sistema (configurada em algum grupo), não usar
        // fallback
        if (isPermissionDefinedInSystem(permission)) {
            return false;
        }

        // Permissão vanilla não configurada - usar fallback
        return true;
    }

    /**
     * Verifica se uma permissão foi definida em algum grupo do sistema
     */
    private boolean isPermissionDefinedInSystem(String permission) {
        if (plugin == null)
            return false;

        GroupManager groupManager = plugin.getGroupManager();
        if (groupManager == null) {
            return false;
        }

        // Verificar em todos os grupos se esta permissão existe
        for (Group group : groupManager.getAllGroups()) {
            Set<String> perms = groupManager.getAllPermissions(group);
            String permLower = permission.toLowerCase();
            if (perms.contains(permLower) || perms.contains("-" + permLower)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Verifica se o jogador é REALMENTE um operador no Minecraft original.
     * 
     * Esta verificação é usada apenas como FALLBACK para permissões vanilla
     * não configuradas no nosso sistema.
     * 
     * @return true se é operador real
     */
    private boolean isReallyOp() {
        // Consultar o status de OP real no servidor
        return player.getServer().getOperators().stream()
                .anyMatch(op -> op.getUniqueId().equals(player.getUniqueId()));
    }

    /**
     * Atualiza o cache de permissões se necessário
     */
    private void updateCacheIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - cacheLastUpdate > CACHE_DURATION) {
            refreshCache();
        }
    }

    /**
     * Atualiza completamente o cache de permissões
     * 
     * Este método é chamado:
     * - Quando o cache expira
     * - Quando o cargo do jogador muda
     * - Quando um VIP expira
     */
    public void refreshCache() {
        cachedPermissions.clear();

        if (plugin == null)
            return;

        GroupManager groupManager = plugin.getGroupManager();
        if (groupManager == null) {
            return;
        }

        // Carregar todas as permissões de todos os grupos do jogador
        Set<String> allPerms = groupManager.getAllPermissions(player.getUniqueId());
        for (String perm : allPerms) {
            cachedPermissions.add(perm.toLowerCase());
        }

        cacheLastUpdate = System.currentTimeMillis();
    }

    /**
     * Força atualização imediata do cache
     * Chamado quando há mudança de cargo ou expiração de VIP
     */
    public void forceRefresh() {
        cacheLastUpdate = 0; // Invalidar cache
        refreshCache();
    }

    // ==================== SOBRESCREVER isPermissionSet ====================

    @Override
    public boolean isPermissionSet(String name) {
        updateCacheIfNeeded();
        String nameLower = name.toLowerCase();

        // Verificar no nosso cache
        if (cachedPermissions.contains(nameLower) ||
                cachedPermissions.contains("-" + nameLower)) {
            return true;
        }

        // Verificar no pai
        return super.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(Permission perm) {
        return isPermissionSet(perm.getName());
    }

    // ==================== GETTERS ====================

    /**
     * Obtém o jogador associado a este Permissible
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Obtém o PermissibleBase original (para restauração)
     */
    public PermissibleBase getOriginalPermissible() {
        return originalPermissible;
    }

    /**
     * Obtém as permissões em cache (para debug)
     */
    public Set<String> getCachedPermissions() {
        return new HashSet<>(cachedPermissions);
    }
}
