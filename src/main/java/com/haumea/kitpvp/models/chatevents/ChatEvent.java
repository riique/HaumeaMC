package com.haumea.kitpvp.models.chatevents;

import java.util.List;

/**
 * Interface base para todos os tipos de eventos de chat.
 * Cada implementação deve fornecer a lógica específica do evento.
 * 
 * @author HaumeaMC
 */
public interface ChatEvent {

    /**
     * Obtém o tipo deste evento
     * 
     * @return Tipo do evento
     */
    ChatEventType getType();

    /**
     * Gera a pergunta/desafio do evento.
     * Este método é chamado quando o evento inicia.
     */
    void generate();

    /**
     * Obtém a pergunta/desafio formatado para exibição
     * 
     * @return Pergunta formatada com cores
     */
    String getQuestion();

    /**
     * Obtém a dica adicional (se houver)
     * 
     * @return Dica ou null se não houver
     */
    String getHint();

    /**
     * Obtém a resposta correta do evento
     * 
     * @return Resposta correta
     */
    String getAnswer();

    /**
     * Obtém todas as respostas aceitas (sinônimos, etc)
     * 
     * @return Lista de respostas aceitas
     */
    List<String> getAcceptedAnswers();

    /**
     * Verifica se uma resposta está correta
     * 
     * @param answer Resposta do jogador
     * @return true se a resposta está correta
     */
    boolean isCorrect(String answer);

    /**
     * Obtém a dificuldade do evento atual (1-3)
     * 1 = Fácil, 2 = Médio, 3 = Difícil
     * 
     * @return Nível de dificuldade
     */
    int getDifficulty();

    /**
     * Define dados customizados para o evento.
     * Permite que staffers configurem perguntas/respostas personalizadas.
     * 
     * @param customQuestion Pergunta/desafio customizado
     * @param customAnswer   Resposta customizada
     */
    void setCustomData(String customQuestion, String customAnswer);

    /**
     * Verifica se o evento está usando dados customizados
     * 
     * @return true se está usando dados do staffer
     */
    boolean isCustom();
}
