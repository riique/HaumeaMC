package com.haumea.kitpvp.models.chatevents.impl;

import com.haumea.kitpvp.models.chatevents.AbstractChatEvent;
import com.haumea.kitpvp.models.chatevents.ChatEventType;

/**
 * Evento Type Race - Digite a frase exata primeiro.
 * 
 * @author HaumeaMC
 */
public class TypeRaceEvent extends AbstractChatEvent {

    private static final String[] EASY_PHRASES = {
            "haumea pvp",
            "minecraft e legal",
            "eu amo pvp",
            "diamante azul",
            "creeper explodir",
            "sopa de cogumelo",
            "espada afiada",
            "arco e flecha",
            "nether portal",
            "ender dragon"
    };

    private static final String[] MEDIUM_PHRASES = {
            "o melhor servidor de pvp",
            "vamos jogar minecraft juntos",
            "encantamento de protecao quatro",
            "picareta de diamante afiada",
            "o dragao do fim foi derrotado",
            "haumea eh o melhor servidor",
            "kit pvp com sopa e gapple",
            "duelo aceito vamos la",
            "ranking subindo rapido demais"
    };

    private static final String[] HARD_PHRASES = {
            "o creeper explodiu minha casa inteira",
            "precisamos de mais diamantes para a armadura",
            "a batalha contra o wither foi muito dificil",
            "encantamento de afiacao cinco na espada de netherite",
            "o servidor haumea tem os melhores eventos de pvp",
            "minha estrategia de combate eh usar sopa rapidamente",
            "consegui a primeira posicao no ranking de kills"
    };

    @Override
    public ChatEventType getType() {
        return ChatEventType.TYPE_RACE;
    }

    @Override
    public void generate() {
        acceptedAnswers.clear();

        int diffLevel = RANDOM.nextInt(3) + 1;
        setDifficulty(diffLevel);

        String[] phraseList;
        switch (diffLevel) {
            case 1:
                phraseList = EASY_PHRASES;
                break;
            case 2:
                phraseList = MEDIUM_PHRASES;
                break;
            case 3:
                phraseList = HARD_PHRASES;
                break;
            default:
                phraseList = EASY_PHRASES;
        }

        answer = randomElement(phraseList);
        question = "§e§l" + answer.toUpperCase();
        hint = "§7Dica: A frase tem §e" + answer.split(" ").length + " §7palavras";
    }

    @Override
    public boolean isCorrect(String playerAnswer) {
        if (playerAnswer == null || playerAnswer.trim().isEmpty()) {
            return false;
        }

        // Para Type Race, precisa ser exatamente igual (normalizado)
        String normalized = normalizeAnswer(playerAnswer);
        String normalizedAnswer = normalizeAnswer(answer);

        return normalized.equals(normalizedAnswer);
    }
}
