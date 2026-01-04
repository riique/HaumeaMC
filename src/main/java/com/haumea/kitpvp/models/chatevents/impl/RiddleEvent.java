package com.haumea.kitpvp.models.chatevents.impl;

import com.haumea.kitpvp.models.chatevents.AbstractChatEvent;
import com.haumea.kitpvp.models.chatevents.ChatEventType;

/**
 * Evento Enigma - Charadas e enigmas lógicos.
 * 
 * @author HaumeaMC
 */
public class RiddleEvent extends AbstractChatEvent {

    // Formato: {enigma, resposta, respostas_alternativas...}
    private static final String[][] RIDDLES = {
            // Fáceis
            { "O que é, o que é? Tem coroa mas não é rei, tem raiz mas não é planta", "dente" },
            { "O que é, o que é? Quanto mais se tira, maior fica", "buraco" },
            { "O que é, o que é? Passa pela água e não se molha", "sombra" },
            { "O que é, o que é? Tem pernas mas não anda", "mesa", "cadeira" },
            { "O que é, o que é? Quanto mais quente, mais fresco", "pão", "pao" },
            { "O que é, o que é? Está no meio do ovo", "v", "letra v" },
            { "O que é, o que é? Cai em pé e corre deitado", "chuva" },
            { "O que é, o que é? Tem dentes mas não morde", "pente" },
            { "O que é, o que é? Anda com os pés na cabeça", "piolho" },

            // Médios
            { "O que é, o que é? Não é vivo mas cresce, não tem pulmões mas precisa de ar", "fogo" },
            { "O que é, o que é? Sempre vem mas nunca chega", "amanha", "amanhã", "o amanhã" },
            { "O que é, o que é? Quanto mais você tira, mais ele cresce", "buraco" },
            { "O que é, o que é? Uma casa com duas portas, sem janelas nem telhados, com moradores barulhentos",
                    "acordeao", "acordeão", "sanfona" },
            { "O que é, o que é? Tem olho mas não vê", "agulha", "furacao" },
            { "O que é, o que é? Nasce grande e morre pequeno", "vela", "lápis", "lapis" },
            { "O que é, o que é? Anda deitado e dorme em pé", "pe", "pé" },
            { "O que é, o que é? Tem braços mas não abraça", "cadeira", "poltrona" },

            // Difíceis
            { "O que é, o que é? Pode atravessar o vidro sem quebrá-lo", "luz" },
            { "O que é, o que é? Fala todas as línguas mas nunca aprendeu nenhuma", "eco" },
            { "O que é, o que é? Sempre está na sua frente mas nunca pode ser visto", "futuro", "o futuro" },
            { "O que é, o que é? Você pode pegá-lo mas não pode jogá-lo", "resfriado", "gripe" },
            { "O que é, o que é? Tem chaves mas não abre portas", "piano", "teclado" },
            { "O que é, o que é? Está sempre chegando mas nunca chega", "amanhã", "amanha", "o amanhã" },
            { "O que é, o que é? Quanto mais seca, mais molhada fica", "toalha" },
            { "O que é, o que é? Pode ser quebrado sem ser tocado", "promessa", "silêncio", "silencio" },
            { "O que é, o que é? Atravessa cidades e campos sem se mover", "estrada", "rua", "caminho" }
    };

    @Override
    public ChatEventType getType() {
        return ChatEventType.RIDDLE;
    }

    @Override
    public void generate() {
        acceptedAnswers.clear();

        // Dificuldade baseada na posição do enigma na lista
        int index = RANDOM.nextInt(RIDDLES.length);
        if (index < 9) {
            setDifficulty(1);
        } else if (index < 17) {
            setDifficulty(2);
        } else {
            setDifficulty(3);
        }

        String[] selected = RIDDLES[index];
        question = "§9" + selected[0];
        answer = selected[1];

        // Adicionar respostas alternativas
        for (int i = 2; i < selected.length; i++) {
            addAcceptedAnswer(selected[i]);
        }

        hint = "§7Dica: A resposta tem §9" + answer.length() + " §7letras";
    }
}
