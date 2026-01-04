package com.haumea.kitpvp.models.chatevents.impl;

import com.haumea.kitpvp.models.chatevents.AbstractChatEvent;
import com.haumea.kitpvp.models.chatevents.ChatEventType;

/**
 * Evento Adivinhe o Mob - Descrição do mob para descobrir qual é.
 * 
 * @author HaumeaMC
 */
public class MobGuessEvent extends AbstractChatEvent {

    // Formato: {descricao, resposta, respostas_alternativas...}
    private static final String[][] MOBS = {
            // Fáceis
            { "Este mob verde explode quando chega perto do jogador e faz um som de 'sssss'", "creeper" },
            { "Este mob morto-vivo ataca à noite e queima ao sol", "zombie", "zumbi" },
            { "Este mob carrega um arco e flecha e atira no jogador", "esqueleto", "skeleton" },
            { "Este mob é uma aranha gigante que ataca à noite", "aranha", "spider" },
            { "Este mob rosa pode ser usado para obter carne de porco", "porco", "pig" },
            { "Este mob peludo pode ser tosquiado para obter lã", "ovelha", "sheep" },
            { "Este mob é o principal vilão do jogo e vive no End", "ender dragon", "dragon", "dragao" },
            { "Este mob faz 'muuu' e dá leite quando ordenhado", "vaca", "cow" },
            { "Este mob põe ovos e pode ser usado para obter penas", "galinha", "chicken" },

            // Médios
            { "Este mob alto e preto se teleporta e é hostil quando olhado nos olhos", "enderman" },
            { "Este mob morto-vivo aparece em pântanos e atira poções", "bruxa", "witch" },
            { "Este mob pequeno aparece em cavernas e pula no jogador", "silverfish", "peixe-prateado", "traça" },
            { "Este mob esqueleto cavalga aranhas às vezes", "esqueleto aranha", "spider jockey", "aranha jockey" },
            { "Este mob de ferro protege aldeões e é muito forte", "iron golem", "golem de ferro", "golem" },
            { "Este mob fantasma aparece quando você não dorme por dias", "phantom", "fantasma" },
            { "Este mob nada na água e ataca com seu tridente", "drowned", "afogado" },
            { "Este mob vive no Nether e atira bolas de fogo", "ghast" },
            { "Este mob dourado vive no Nether e troca itens", "piglin" },

            // Difíceis
            { "Este mob é uma versão mais forte do esqueleto e aparece no Nether", "wither skeleton",
                    "esqueleto wither" },
            { "Este boss pode ser invocado com 4 blocos de soul sand e 3 cabeças de wither skeleton", "wither" },
            { "Este mob aquático tem presas e pode ser domesticado com bacalhau", "axolotl", "axolote" },
            { "Este mob guardião protege monumentos oceânicos e causa fadiga de mineração", "elder guardian",
                    "guardiao anciao" },
            { "Este mob aparece em ravagers durante raids de aldeões", "ravager" },
            { "Este mob cego vive nas profundezas e detecta vibrações", "warden", "guardiao das profundezas" },
            { "Este mob voador aparece em End Cities e pode dropar Shulker Box", "shulker" },
            { "Este mob é uma versão bebê do zombie e corre muito rápido", "baby zombie", "zombie bebe", "zumbi bebe" }
    };

    @Override
    public ChatEventType getType() {
        return ChatEventType.MOB_GUESS;
    }

    @Override
    public void generate() {
        acceptedAnswers.clear();

        // Dificuldade baseada na posição do mob na lista
        int index = RANDOM.nextInt(MOBS.length);
        if (index < 9) {
            setDifficulty(1);
        } else if (index < 18) {
            setDifficulty(2);
        } else {
            setDifficulty(3);
        }

        String[] selected = MOBS[index];
        question = "§a" + selected[0];
        answer = selected[1];

        // Adicionar respostas alternativas
        for (int i = 2; i < selected.length; i++) {
            addAcceptedAnswer(selected[i]);
        }

        hint = "§7Dica: O nome do mob começa com §a\"" + answer.substring(0, Math.min(2, answer.length())).toUpperCase()
                + "...\"";
    }
}
