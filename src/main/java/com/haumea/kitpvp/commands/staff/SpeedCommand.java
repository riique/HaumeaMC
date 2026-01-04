package com.haumea.kitpvp.commands.staff;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.utils.ChatStorage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para alterar a velocidade do jogador.
 * Funciona tanto para andar quanto para voar.
 * 
 * Escala: 1-10 (aceita decimais com 1 casa, ex: 1.5)
 * - 1 = Velocidade normal
 * - 10 = Velocidade máxima
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "speed", aliases = {
        "velocidade" }, description = "Altera a velocidade de movimento e voo", usage = "/speed <1-10>", playerOnly = true, allowedGroups = {
                "dono", "diretor", "gerente", "admin", "mod", "helper" })
public class SpeedCommand extends BaseCommand {

    // Velocidade padrão do Minecraft
    private static final float DEFAULT_WALK_SPEED = 0.2f;
    private static final float DEFAULT_FLY_SPEED = 0.1f;

    // Multiplicadores máximos (velocidade 10 = 10x mais rápido)
    private static final float MAX_WALK_SPEED = 1.0f; // Limite do Bukkit
    private static final float MAX_FLY_SPEED = 1.0f; // Limite do Bukkit

    public SpeedCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();

        // Verificar argumentos
        if (args.length == 0) {
            sendUsage();
            showCurrentSpeed(player);
            return;
        }

        // Parsear velocidade
        float speedValue;
        try {
            speedValue = Float.parseFloat(args[0]);
        } catch (NumberFormatException e) {
            ChatStorage.send(player, "error.invalid-number", "value", args[0]);
            return;
        }

        // Validar range (1-10)
        if (speedValue < 1 || speedValue > 10) {
            ChatStorage.send(player, "staff.speed-invalid-range");
            return;
        }

        // Calcular velocidade real (converter 1-10 para 0.0-1.0)
        // 1 = velocidade normal, 10 = velocidade máxima
        float normalizedSpeed = (speedValue - 1) / 9.0f; // 0.0 a 1.0

        // Calcular walk speed: de DEFAULT_WALK_SPEED até MAX_WALK_SPEED
        float walkSpeed = DEFAULT_WALK_SPEED + (normalizedSpeed * (MAX_WALK_SPEED - DEFAULT_WALK_SPEED));

        // Calcular fly speed: de DEFAULT_FLY_SPEED até MAX_FLY_SPEED
        float flySpeed = DEFAULT_FLY_SPEED + (normalizedSpeed * (MAX_FLY_SPEED - DEFAULT_FLY_SPEED));

        // Aplicar velocidades
        player.setWalkSpeed(walkSpeed);
        player.setFlySpeed(flySpeed);

        // Formatar velocidade para exibição (1 casa decimal se necessário)
        String speedDisplay;
        if (speedValue == Math.floor(speedValue)) {
            speedDisplay = String.valueOf((int) speedValue);
        } else {
            speedDisplay = String.format("%.1f", speedValue);
        }

        ChatStorage.send(player, "staff.speed-changed", "speed", speedDisplay);
    }

    /**
     * Mostra a velocidade atual do jogador
     */
    private void showCurrentSpeed(Player player) {
        // Converter velocidade atual para escala 1-10
        float currentWalkSpeed = player.getWalkSpeed();
        float normalizedSpeed = (currentWalkSpeed - DEFAULT_WALK_SPEED) / (MAX_WALK_SPEED - DEFAULT_WALK_SPEED);
        float speedValue = 1 + (normalizedSpeed * 9);

        // Formatar para exibição
        String speedDisplay;
        if (speedValue == Math.floor(speedValue)) {
            speedDisplay = String.valueOf((int) speedValue);
        } else {
            speedDisplay = String.format("%.1f", speedValue);
        }

        ChatStorage.send(player, "staff.speed-current", "speed", speedDisplay);
    }
}
