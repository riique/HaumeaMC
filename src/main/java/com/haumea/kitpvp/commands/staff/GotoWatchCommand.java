package com.haumea.kitpvp.commands.staff;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.profile.PlayerProfile;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para ir até um jogador e ativar modo admin automaticamente.
 * Usado principalmente pelo sistema de reports para facilitar a moderação.
 * 
 * Funcionalidade:
 * - Teleporta o staffer para o jogador alvo
 * - Ativa o modo admin automaticamente (se ainda não estiver)
 * - Requer permissão de /admin
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "gotowatch", aliases = { "gotoreport", "observar",
        "reporttp" }, description = "Teleporta para um jogador e ativa modo admin", playerOnly = true, allowedGroups = {
                "dono", "diretor", "gerente", "admin", "moderador", "ajudante" })
public class GotoWatchCommand extends BaseCommand {

    public GotoWatchCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player staff = getPlayer();

        if (args.length < 1) {
            sendUsageHelp(staff);
            return;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        // Verificar se o alvo existe e está online
        if (target == null || !target.isOnline()) {
            ChatStorage.send(staff, "report.goto.player-offline", "player", targetName);
            return;
        }

        // Verificar se não é o mesmo jogador
        if (target.equals(staff)) {
            ChatStorage.send(staff, "report.goto.self");
            return;
        }

        PlayerProfile profile = plugin.getProfileManager().getProfile(staff);
        if (profile == null) {
            sendRaw("&cErro ao carregar seu perfil!");
            return;
        }

        // Se não está em modo admin, ativar automaticamente
        if (!profile.isVanish()) {
            AdminCommand.enableAdminMode(plugin, staff, profile);
            ChatStorage.send(staff, "staff.admin-enter");
        }

        // Teleportar para o jogador alvo
        staff.teleport(target.getLocation());

        // Enviar mensagem de sucesso
        ChatStorage.send(staff, "report.goto.success",
                "target", target.getName(),
                "x", String.valueOf((int) target.getLocation().getX()),
                "y", String.valueOf((int) target.getLocation().getY()),
                "z", String.valueOf((int) target.getLocation().getZ()));

        // Log para o console
        plugin.getLogger()
                .info("[REPORT] " + staff.getName() + " teleportou para " + target.getName() + " via /gotowatch");
    }

    /**
     * Exibe ajuda de uso do comando
     */
    private void sendUsageHelp(Player staff) {
        sendRaw("");
        sendRaw("&6&l  GOTO WATCH &8- &7Observe jogadores reportados");
        sendRaw("");
        sendRaw("&e  Uso: &f/gotowatch <jogador>");
        sendRaw("");
        sendRaw("&6  Informações:");
        sendRaw("    &7★ &fTeleporta você até o &ejogador");
        sendRaw("    &7★ &fAtiva o &emodo admin &fautomaticamente");
        sendRaw("    &7★ &fVocê fica &einvisível &fpara observar");
        sendRaw("");
    }
}
