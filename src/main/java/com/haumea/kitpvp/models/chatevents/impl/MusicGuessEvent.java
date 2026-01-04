package com.haumea.kitpvp.models.chatevents.impl;

import com.haumea.kitpvp.models.chatevents.AbstractChatEvent;
import com.haumea.kitpvp.models.chatevents.ChatEventType;

/**
 * Evento Adivinhe a Música - Trecho de letra para descobrir a música.
 * Músicas brasileiras populares.
 * 
 * @author HaumeaMC
 */
public class MusicGuessEvent extends AbstractChatEvent {

    // Formato: {trecho, nome_musica, respostas_alternativas...}
    private static final String[][] MUSICS = {
            // Pop/Funk BR
            { "'Eu só quero é ser feliz, andar tranquilamente na favela onde eu nasci'", "rap da felicidade",
                    "felicidade" },
            { "'Ai se eu te pego, ai ai se eu te pego'", "ai se eu te pego", "michel telo" },
            { "'Jenifer, eu tentei te ligar, mas não consegui te encontrar'", "jenifer", "gabriel diniz" },
            { "'Tudo que você quiser, eu vou realizar todos os seus desejos'", "realce", "mc pedrinho", "pedrinho" },
            { "'Deixa eu te amar, só por hoje, só por agora'", "deixa eu te amar", "deixa" },
            { "'Tá tranquilo, tá favorável'", "ta tranquilo ta favoravel", "mc bin laden", "tranquilo favoravel" },

            // Sertanejo
            { "'Eu juro que não falo mais o seu nome, juro!'", "maus bocados", "cristiano araujo" },
            { "'Cem anos pra viver, e você gastou só vinte e três'", "cem anos", "henrique e juliano" },
            { "'Tchau, tchau amor, eu vou embora, tchau'", "tchau amor", "larissa manoela", "tchau", "amor" },
            { "'Tô te esperando lá na rua, debaixo da chuva'", "debaixo da chuva" },

            // Rock BR
            { "'Será que é o tempo perdido que eu sinto?'", "tempo perdido", "legiao urbana" },
            { "'Só sei que era primavera, e aquele amor me deixava cega'", "primavera", "primavera cassiano" },
            { "'É preciso amar as pessoas como se não houvesse amanhã'", "pais e filhos", "legiao urbana" },
            { "'Somos quem podemos ser, sonhos que podemos ter'", "somos quem podemos ser", "engenheiros do hawaii" },
            { "'Ana Julia, eu nunca mais vou te esquecer'", "ana julia", "los hermanos" },

            // Pagode
            { "'Ei, você aí, me dá um dinheiro aí'", "me da um dinheiro ai", "antonio marcos" },
            { "'Deixa eu dizer que te amo, amei demais'", "deixa eu dizer", "pagode" },
            { "'Sai da frente que o Bonde passou'", "bonde do tigrao", "cerol na mao" },

            // Internacionais conhecidas
            { "'Is this the real life? Is this just fantasy?'", "bohemian rhapsody", "queen" },
            { "'Never gonna give you up, never gonna let you down'", "never gonna give you up", "rick astley",
                    "rickroll" },
            { "'We will, we will rock you'", "we will rock you", "queen" },
            { "'Hello, it's me, I was wondering'", "hello", "adele" },
            { "'Baby shark, doo doo doo doo doo doo'", "baby shark", "pinkfong" },
            { "'Despacito, quiero respirar tu cuello despacito'", "despacito", "luis fonsi" },
            { "'Cause baby you're a firework'", "firework", "katy perry" }
    };

    private String lyricSnippet;

    @Override
    public ChatEventType getType() {
        return ChatEventType.MUSIC_GUESS;
    }

    @Override
    public void generate() {
        acceptedAnswers.clear();

        // Músicas têm dificuldade variada baseada em popularidade
        setDifficulty(RANDOM.nextInt(3) + 1);

        String[] selected = randomElement(MUSICS);
        lyricSnippet = selected[0];
        answer = selected[1];

        // Adicionar respostas alternativas
        for (int i = 2; i < selected.length; i++) {
            addAcceptedAnswer(selected[i]);
        }

        question = "§d" + lyricSnippet;
        hint = "§7Dica: O nome da música começa com §d\""
                + answer.substring(0, Math.min(3, answer.length())).toUpperCase() + "...\"";
    }
}
