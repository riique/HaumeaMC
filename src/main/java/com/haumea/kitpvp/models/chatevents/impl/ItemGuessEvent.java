package com.haumea.kitpvp.models.chatevents.impl;

import com.haumea.kitpvp.models.chatevents.AbstractChatEvent;
import com.haumea.kitpvp.models.chatevents.ChatEventType;

/**
 * Evento Adivinhe o Item - Descrição do item para descobrir qual é.
 * 
 * @author HaumeaMC
 */
public class ItemGuessEvent extends AbstractChatEvent {

    // Formato: {descricao, resposta, respostas_alternativas...}
    private static final String[][] ITEMS = {
            // Fáceis
            { "Este item é usado para minerar blocos e é feito na crafting table", "picareta", "pickaxe" },
            { "Este item azul brilhante é o mais valioso para ferramentas", "diamante", "diamond" },
            { "Este item vermelho aumenta sua vida quando consumido", "maca", "apple", "golden apple", "maca dourada" },
            { "Este item é usado para fazer tochas junto com gravetos", "carvao", "coal", "carvão" },
            { "Este item de ferro é usado para cortar árvores", "machado", "axe" },
            { "Este item quadrado amarelo pode ser colocado e armazena itens", "bau", "chest", "baú" },
            { "Este item verde é dropado por Creepers", "polvora", "gunpowder", "pólvora" },
            { "Este item é usado para pescar", "vara de pesca", "fishing rod", "vara" },
            { "Este item preto é usado para fazer portais do Nether", "obsidiana", "obsidian" },

            // Médios
            { "Este item roxo permite teleporte quando arremessado", "ender pearl", "perola do end", "pérola" },
            { "Este item é feito com 9 diamantes e é usado em beacons", "bloco de diamante", "diamond block" },
            { "Este item laranja é obtido derretendo areia", "vidro", "glass" },
            { "Este item é usado para encantamentos e é encontrado em bibliotecas", "livro", "book" },
            { "Este item permite respirar debaixo d'água por mais tempo", "helmet encantado", "respiration",
                    "aqua affinity" },
            { "Este item é craftado com bastões de blaze e ender pearls", "olho do end", "eye of ender",
                    "olho do ender" },
            { "Este item dourado dá regeneração e absorção quando comido", "golden apple", "maca dourada enchanted",
                    "gapple", "notch apple" },
            { "Este item é usado para criar poções na brewing stand", "frasco de vidro", "glass bottle", "frasco" },
            { "Este item é dropado por Blazes e usado em poções", "bastao de blaze", "blaze rod" },

            // Difíceis
            { "Este item raro permite voar e é obtido em End Cities", "elytra", "elytras", "asas" },
            { "Este item especial pode reviver você quando morre", "totem of undying", "totem",
                    "totem da imortalidade" },
            { "Este item é usado para capturar mobs", "lead", "corda", "cordas" },
            { "Este item é usado por Shulkers para se proteger", "shulker shell", "casco de shulker" },
            { "Este item pode carregar seu inventário mesmo após a morte", "shulker box", "caixa de shulker" },
            { "Este item é usado para encantamentos de nível máximo", "livro encantado", "enchanted book" },
            { "Este item permite carregar 27 stacks em um slot", "ender chest", "bau do end", "baú do end" },
            { "Este item é craftado com netherite scrap e lingotes de ouro", "netherite ingot", "lingote de netherite" }
    };

    @Override
    public ChatEventType getType() {
        return ChatEventType.ITEM_GUESS;
    }

    @Override
    public void generate() {
        acceptedAnswers.clear();

        // Dificuldade baseada na posição do item na lista
        int index = RANDOM.nextInt(ITEMS.length);
        if (index < 9) {
            setDifficulty(1);
        } else if (index < 18) {
            setDifficulty(2);
        } else {
            setDifficulty(3);
        }

        String[] selected = ITEMS[index];
        question = "§6" + selected[0];
        answer = selected[1];

        // Adicionar respostas alternativas
        for (int i = 2; i < selected.length; i++) {
            addAcceptedAnswer(selected[i]);
        }

        hint = "§7Dica: O nome do item começa com §6\""
                + answer.substring(0, Math.min(2, answer.length())).toUpperCase() + "...\"";
    }
}
