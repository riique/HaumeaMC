package com.haumea.kitpvp.models.chatevents.impl;

import com.haumea.kitpvp.models.chatevents.AbstractChatEvent;
import com.haumea.kitpvp.models.chatevents.ChatEventType;

/**
 * Evento Trivia - Perguntas de conhecimento geral e Minecraft.
 * 
 * @author HaumeaMC
 */
public class TriviaEvent extends AbstractChatEvent {

    // Formato: {pergunta, resposta, respostas_alternativas...}
    private static final String[][] EASY_QUESTIONS = {
            { "Qual mob explode quando chega perto do jogador?", "creeper" },
            { "Qual o minério mais raro do Minecraft?", "diamante", "esmeralda" },
            { "Quantos blocos de altura tem o Steve?", "2", "dois" },
            { "Qual a dimensão onde fica o Ender Dragon?", "end", "the end", "fim" },
            { "Quantos itens cabem em um slot de inventário?", "64", "sessenta e quatro" },
            { "Qual ferramenta é usada para minerar diamantes?", "picareta", "picareta de ferro", "pickaxe" },
            { "De qual animal você obtém lã?", "ovelha", "sheep" },
            { "Qual mob dropa pólvora?", "creeper" },
            { "Qual item é usado para fazer tochas?", "carvao", "carvão", "coal" },
            { "Quantos corações o jogador tem?", "10", "dez", "20 meios" }
    };

    private static final String[][] MEDIUM_QUESTIONS = {
            { "Qual a altura máxima do mundo no Minecraft antigo?", "256", "duzentos e cinquenta e seis" },
            { "Quantos blocos de obsidiana são necessários para um portal do Nether?", "10", "dez", "14", "quatorze" },
            { "Qual mob dropa Ender Pearl?", "enderman" },
            { "Qual versão do Minecraft introduziu o Elytra?", "1.9" },
            { "Quantos tipos de madeira existem no Minecraft vanilla?", "6", "seis", "8", "oito" },
            { "Qual a comida que mais sacia fome no Minecraft?", "golden apple", "maca dourada", "bife", "steak" },
            { "Em qual dimensão você encontra Blazes?", "nether", "inferno" },
            { "Qual o encantamento que dá respiração debaixo d'água?", "respiration", "respiracao", "aqua affinity" },
            { "Quantas vezes você pode encantar um item na bigorna?", "6", "seis" },
            { "Qual mob pode ser domesticado com ossos?", "lobo", "wolf", "cachorro" }
    };

    private static final String[][] HARD_QUESTIONS = {
            { "Em qual ano o Minecraft foi lançado oficialmente?", "2011", "dois mil e onze" },
            { "Qual o nome do criador do Minecraft?", "notch", "markus persson", "markus" },
            { "Quantos biomas existem no Overworld do Minecraft Java?", "64", "sessenta e quatro" },
            { "Qual o material mais resistente a explosões?", "obsidiana", "obsidian", "bedrock" },
            { "Quantas páginas um livro encantado pode ter no máximo?", "100", "cem" },
            { "Qual a velocidade máxima do Elytra em blocos por segundo?", "67", "sessenta e sete" },
            { "Qual encantamento é incompatível com Smite?", "sharpness", "afiacao", "bane of arthropods" },
            { "Quantos slots tem o inventário do jogador?", "36", "trinta e seis" },
            { "Qual mob foi adicionado na versão 1.19?", "warden", "guardiao" },
            { "Qual o limite de nível de encantamento na mesa de encantamento?", "30", "trinta" }
    };

    @Override
    public ChatEventType getType() {
        return ChatEventType.TRIVIA;
    }

    @Override
    public void generate() {
        acceptedAnswers.clear();

        int diffLevel = RANDOM.nextInt(3) + 1;
        setDifficulty(diffLevel);

        String[][] questionList;
        switch (diffLevel) {
            case 1:
                questionList = EASY_QUESTIONS;
                break;
            case 2:
                questionList = MEDIUM_QUESTIONS;
                break;
            case 3:
                questionList = HARD_QUESTIONS;
                break;
            default:
                questionList = EASY_QUESTIONS;
        }

        String[] selected = randomElement(questionList);
        question = "§e" + selected[0];
        answer = selected[1];

        // Adicionar respostas alternativas
        for (int i = 2; i < selected.length; i++) {
            addAcceptedAnswer(selected[i]);
        }

        hint = "§7Dica: A resposta começa com §e\"" + answer.substring(0, Math.min(2, answer.length())).toUpperCase()
                + "...\"";
    }
}
