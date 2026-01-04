package com.haumea.kitpvp.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.managers.FakeNickManager;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Listener de pacotes para o sistema de Fake Nick.
 * 
 * Usa ProtocolLib para interceptar pacotes de PlayerInfo e NamedEntitySpawn
 * e modificar o nome do jogador antes de enviar para outros jogadores.
 * 
 * Isso permite que o nome fake apareça corretamente na:
 * - TabList
 * - Nametag (acima da cabeça)
 * - Chat (já funciona via DisplayManager)
 * 
 * @author HaumeaMC
 */
public class FakeNickPacketListener {

    private final HaumeaMC plugin;
    private final ProtocolManager protocolManager;

    public FakeNickPacketListener(HaumeaMC plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();

        registerListeners();
        plugin.getLogger().info("[FakeNickPacketListener] Listener de pacotes para Fake Nick registrado!");
    }

    /**
     * Registra os listeners de pacotes
     */
    private void registerListeners() {
        // Interceptar PacketPlayOutPlayerInfo (ADD_PLAYER, UPDATE_DISPLAY_NAME)
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Server.PLAYER_INFO) {
            @Override
            public void onPacketSending(PacketEvent event) {
                handlePlayerInfoPacket(event);
            }
        });

        // Interceptar PacketPlayOutNamedEntitySpawn (quando jogador aparece para outro)
        protocolManager.addPacketListener(new PacketAdapter(plugin, ListenerPriority.NORMAL,
                PacketType.Play.Server.NAMED_ENTITY_SPAWN) {
            @Override
            public void onPacketSending(PacketEvent event) {
                handleNamedEntitySpawnPacket(event);
            }
        });
    }

    /**
     * Manipula o pacote PlayerInfo para modificar o nome do jogador
     */
    private void handlePlayerInfoPacket(PacketEvent event) {
        if (plugin.getFakeNickManager() == null)
            return;

        PacketContainer packet = event.getPacket();

        // Obter a ação do pacote
        EnumWrappers.PlayerInfoAction action = packet.getPlayerInfoAction().read(0);

        // Só processar ADD_PLAYER e UPDATE_DISPLAY_NAME
        if (action != EnumWrappers.PlayerInfoAction.ADD_PLAYER &&
                action != EnumWrappers.PlayerInfoAction.UPDATE_DISPLAY_NAME) {
            return;
        }

        // Obter lista de PlayerInfoData
        List<PlayerInfoData> dataList = packet.getPlayerInfoDataLists().read(0);
        if (dataList == null || dataList.isEmpty())
            return;

        List<PlayerInfoData> modifiedList = new ArrayList<>();
        boolean modified = false;

        for (PlayerInfoData data : dataList) {
            WrappedGameProfile profile = data.getProfile();
            if (profile == null)
                continue;

            UUID playerUUID = profile.getUUID();
            FakeNickManager fakeNickManager = plugin.getFakeNickManager();

            // Verificar se o jogador tem fake nick
            if (fakeNickManager.hasFakeNick(playerUUID)) {
                String fakeNick = fakeNickManager.getFakeNick(playerUUID);

                // Obter prefixo e sufixo do DisplayManager
                // PREFIXO = TAG (esquerda do nome)
                // SUFIXO = Símbolo da Liga (direita do nome)
                String displayPrefix = "";
                String displaySuffix = "";
                Player targetPlayer = plugin.getServer().getPlayer(playerUUID);
                if (targetPlayer != null && plugin.getDisplayManager() != null) {
                    com.haumea.kitpvp.managers.DisplayManager.DisplayData displayData = plugin.getDisplayManager()
                            .getDisplayData(targetPlayer);
                    if (displayData != null) {
                        displayPrefix = displayData.getPrefix(); // TAG no lado esquerdo
                        displaySuffix = " " + displayData.getLeagueSymbol().trim(); // Liga no lado direito
                    }
                }

                // Criar novo GameProfile com o nome fake
                WrappedGameProfile newProfile = new WrappedGameProfile(playerUUID, fakeNick);

                // Copiar propriedades (skin) do profile original
                newProfile.getProperties().putAll(profile.getProperties());

                // Criar novo PlayerInfoData com o profile modificado
                // Formato: [PREFIXO/TAG] [NOME] [SUFIXO/LIGA]
                String tablistName = displayPrefix + fakeNick + displaySuffix;

                PlayerInfoData newData = new PlayerInfoData(
                        newProfile,
                        data.getLatency(),
                        data.getGameMode(),
                        WrappedChatComponent.fromText(tablistName));

                modifiedList.add(newData);
                modified = true;
            } else {
                modifiedList.add(data);
            }
        }

        // Se algo foi modificado, atualizar o pacote
        if (modified) {
            packet.getPlayerInfoDataLists().write(0, modifiedList);
        }
    }

    /**
     * Manipula o pacote NamedEntitySpawn
     * Este pacote usa o GameProfile que já foi modificado pelo PlayerInfo,
     * então na teoria já deve estar correto. Mas vamos garantir.
     */
    private void handleNamedEntitySpawnPacket(PacketEvent event) {
        // O NamedEntitySpawn usa o UUID para referenciar o GameProfile
        // que já foi enviado via PlayerInfo, então não precisa modificar aqui
        // O cliente usa o cache do PlayerInfo para pegar o nome
    }

    /**
     * Força a atualização do PlayerInfo para todos os jogadores verem o novo nome
     * 
     * IMPORTANTE: Não destruímos/respawnamos a entidade porque isso quebra a
     * associação com os Teams do Scoreboard (que mostram prefixo da tag/liga).
     * Apenas atualizamos o PlayerInfo e usamos hide/show do Bukkit.
     */
    public void refreshPlayerInfo(Player player) {
        if (plugin.getFakeNickManager() == null)
            return;

        FakeNickManager fakeNickManager = plugin.getFakeNickManager();
        String displayName = fakeNickManager.hasFakeNick(player)
                ? fakeNickManager.getFakeNick(player)
                : player.getName();

        // Obter prefixo e sufixo do DisplayManager
        // PREFIXO = TAG (esquerda do nome)
        // SUFIXO = Símbolo da Liga (direita do nome)
        String displayPrefix = "";
        String displaySuffix = "";
        if (plugin.getDisplayManager() != null) {
            com.haumea.kitpvp.managers.DisplayManager.DisplayData displayData = plugin.getDisplayManager()
                    .getDisplayData(player);
            if (displayData != null) {
                displayPrefix = displayData.getPrefix(); // TAG no lado esquerdo
                displaySuffix = " " + displayData.getLeagueSymbol().trim(); // Liga no lado direito
            }
        }
        // Formato: [PREFIXO/TAG] [NOME] [SUFIXO/LIGA]
        String tablistName = displayPrefix + displayName + displaySuffix;

        // Criar pacote de remoção
        PacketContainer removePacket = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
        removePacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);

        WrappedGameProfile profile = WrappedGameProfile.fromPlayer(player);
        PlayerInfoData removeData = new PlayerInfoData(profile, 0, EnumWrappers.NativeGameMode.SURVIVAL, null);

        List<PlayerInfoData> removeList = new ArrayList<>();
        removeList.add(removeData);
        removePacket.getPlayerInfoDataLists().write(0, removeList);

        // Criar pacote de adição com nome fake e prefixo
        PacketContainer addPacket = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
        addPacket.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);

        WrappedGameProfile newProfile = new WrappedGameProfile(player.getUniqueId(), displayName);
        // Copiar skin
        newProfile.getProperties().putAll(WrappedGameProfile.fromPlayer(player).getProperties());

        PlayerInfoData addData = new PlayerInfoData(
                newProfile,
                1, // Ping
                EnumWrappers.NativeGameMode.fromBukkit(player.getGameMode()),
                WrappedChatComponent.fromText(tablistName));

        List<PlayerInfoData> addList = new ArrayList<>();
        addList.add(addData);
        addPacket.getPlayerInfoDataLists().write(0, addList);

        // Enviar para todos os jogadores online
        for (Player online : plugin.getServer().getOnlinePlayers()) {
            if (online.equals(player))
                continue;

            try {
                // Atualizar PlayerInfo (TabList)
                protocolManager.sendServerPacket(online, removePacket);
                protocolManager.sendServerPacket(online, addPacket);

                // Usar hide/show do Bukkit para forçar atualização da nametag
                // Isso preserva a associação com os Teams do Scoreboard
                online.hidePlayer(player);
            } catch (Exception e) {
                plugin.getLogger().warning("[FakeNickPacketListener] Erro ao enviar pacote: " + e.getMessage());
            }
        }

        // Mostrar jogador novamente após 2 ticks (para o hide processar)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline())
                return;

            for (Player online : plugin.getServer().getOnlinePlayers()) {
                if (online.equals(player))
                    continue;
                if (online.isOnline()) {
                    online.showPlayer(player);
                }
            }
        }, 2L);
    }

    /**
     * Remove os listeners ao desabilitar o plugin
     */
    public void unregister() {
        protocolManager.removePacketListeners(plugin);
    }
}
