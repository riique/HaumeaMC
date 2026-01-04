package com.haumea.kitpvp.models.chatevents.impl;

import com.haumea.kitpvp.models.chatevents.AbstractChatEvent;
import com.haumea.kitpvp.models.chatevents.ChatEventType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Evento Descramble - Descubra a palavra embaralhada.
 * 
 * @author HaumeaMC
 */
public class DescrambleEvent extends AbstractChatEvent {

    private static final String[] EASY_WORDS = {
            "minecraft", "bloco", "espada", "arco", "flecha", "creeper", "zombie",
            "diamante", "ouro", "ferro", "pedra", "madeira", "agua", "fogo", "terra",
            "jogador", "portal", "nether", "mob", "drops", "combate", "pvp"
    };

    private static final String[] MEDIUM_WORDS = {
            "encantamento", "experiencia", "enderdragon", "esqueleto", "enderman",
            "blaze", "pigman", "ghast", "wither", "villager", "golem", "potion",
            "armadura", "capacete", "calca", "botas", "escudo", "machado"
    };

    private static final String[] HARD_WORDS = {
            "redstone", "comparador", "observador", "pistao", "dispensador",
            "encantamento", "fornalha", "crafting", "agricultura", "mineracao",
            "construcao", "exploracao", "sobrevivencia", "speedrun", "hardcore"
    };

    private String scrambled;

    @Override
    public ChatEventType getType() {
        return ChatEventType.DESCRAMBLE;
    }

    @Override
    public void generate() {
        acceptedAnswers.clear();

        int diffLevel = RANDOM.nextInt(3) + 1;
        setDifficulty(diffLevel);

        String[] wordList;
        switch (diffLevel) {
            case 1:
                wordList = EASY_WORDS;
                break;
            case 2:
                wordList = MEDIUM_WORDS;
                break;
            case 3:
                wordList = HARD_WORDS;
                break;
            default:
                wordList = EASY_WORDS;
        }

        answer = randomElement(wordList);
        scrambled = scrambleWord(answer);

        // Garantir que a palavra embaralhada é diferente da original
        int attempts = 0;
        while (scrambled.equalsIgnoreCase(answer) && attempts < 10) {
            scrambled = scrambleWord(answer);
            attempts++;
        }

        question = "§e§l" + scrambled.toUpperCase();
        hint = "§7Dica: A palavra tem §e" + answer.length() + " §7letras e começa com §e"
                + answer.substring(0, 1).toUpperCase();
    }

    /**
     * Embaralha as letras de uma palavra
     */
    private String scrambleWord(String word) {
        List<Character> chars = new ArrayList<>();
        for (char c : word.toCharArray()) {
            chars.add(c);
        }
        Collections.shuffle(chars);

        StringBuilder sb = new StringBuilder();
        for (char c : chars) {
            sb.append(c);
        }
        return sb.toString();
    }
}
