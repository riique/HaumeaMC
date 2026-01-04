package com.haumea.kitpvp.models.chatevents.impl;

import com.haumea.kitpvp.models.chatevents.AbstractChatEvent;
import com.haumea.kitpvp.models.chatevents.ChatEventType;

/**
 * Evento Complete a Palavra - Palavra com letras faltando.
 * 
 * @author HaumeaMC
 */
public class CompleteWordEvent extends AbstractChatEvent {

    private static final String[] EASY_WORDS = {
            "minecraft", "creeper", "diamante", "espada", "armadura", "picareta",
            "zombie", "portal", "nether", "dragon", "caverna", "floresta"
    };

    private static final String[] MEDIUM_WORDS = {
            "encantamento", "experiencia", "craftando", "construcao", "exploracao",
            "sobrevivencia", "multiplayer", "aventura", "mineracao", "agricultura"
    };

    private static final String[] HARD_WORDS = {
            "achievement", "redstone", "comparador", "dispensador", "observador",
            "spawner", "bioma", "estrutura", "dimensao", "coordenadas"
    };

    private String maskedWord;

    @Override
    public ChatEventType getType() {
        return ChatEventType.COMPLETE_WORD;
    }

    @Override
    public void generate() {
        acceptedAnswers.clear();

        int diffLevel = RANDOM.nextInt(3) + 1;
        setDifficulty(diffLevel);

        String[] wordList;
        float hideRatio;

        switch (diffLevel) {
            case 1:
                wordList = EASY_WORDS;
                hideRatio = 0.3f; // Esconder 30% das letras
                break;
            case 2:
                wordList = MEDIUM_WORDS;
                hideRatio = 0.4f; // Esconder 40% das letras
                break;
            case 3:
                wordList = HARD_WORDS;
                hideRatio = 0.5f; // Esconder 50% das letras
                break;
            default:
                wordList = EASY_WORDS;
                hideRatio = 0.3f;
        }

        answer = randomElement(wordList);
        maskedWord = maskWord(answer, hideRatio);

        question = "§3§l" + maskedWord.toUpperCase();
        hint = "§7Dica: A palavra completa tem §3" + answer.length() + " §7letras";
    }

    /**
     * Mascara uma palavra escondendo algumas letras com underscore
     */
    private String maskWord(String word, float hideRatio) {
        char[] chars = word.toCharArray();
        int lettersToHide = Math.max(1, (int) (word.length() * hideRatio));

        // Criar lista de índices para esconder
        boolean[] hidden = new boolean[word.length()];
        int hiddenCount = 0;

        // Garantir que pelo menos 2 letras permaneçam visíveis
        int maxHidden = Math.max(1, word.length() - 2);
        lettersToHide = Math.min(lettersToHide, maxHidden);

        while (hiddenCount < lettersToHide) {
            int index = RANDOM.nextInt(word.length());
            if (!hidden[index]) {
                hidden[index] = true;
                hiddenCount++;
            }
        }

        // Construir palavra mascarada
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            if (hidden[i]) {
                sb.append("_");
            } else {
                sb.append(chars[i]);
            }
        }

        return sb.toString();
    }
}
