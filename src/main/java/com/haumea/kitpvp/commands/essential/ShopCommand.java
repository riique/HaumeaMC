package com.haumea.kitpvp.commands.essential;

import com.haumea.kitpvp.HaumeaMC;
import com.haumea.kitpvp.commands.base.BaseCommand;
import com.haumea.kitpvp.commands.base.CommandInfo;
import com.haumea.kitpvp.menu.shop.ShopMainMenu;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Comando para abrir o menu da Loja.
 * 
 * Uso: /loja ou /shop
 * 
 * Abre o menu central da loja onde o jogador pode:
 * - Comprar/alugar kits
 * - Comprar multiplicadores
 * - Resetar estatísticas
 * 
 * @author HaumeaMC
 */
@CommandInfo(name = "loja", aliases = { "shop", "store", "mercado" }, playerOnly = true)
public class ShopCommand extends BaseCommand {

    public ShopCommand(HaumeaMC plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer();
        ShopMainMenu.open(plugin, player);
    }
}
