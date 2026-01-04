package com.haumea.kitpvp.commands.league;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.menu.RankingMenu;
import org.bukkit.command.CommandSender;

/**
 * Comando /ranking para abrir o menu de ligas.
 * 
 * Aliases: /rank, /elo, /league, /liga
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "ranking", aliases = { "rank", "elo", "league", "liga",
        "ligas" }, description = "Abre o menu de ranking/ligas", usage = "/ranking", playerOnly = true)
public class RankingCommand extends BaseCommand {

    public RankingCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        // Abrir menu de ranking
        RankingMenu menu = new RankingMenu(plugin, getPlayer());
        menu.open();
    }
}
