package com.haumea.kitpvp.models.chatevents;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Classe base abstrata para implementações de ChatEvent.
 * Fornece funcionalidades comuns a todos os tipos de eventos.
 * 
 * @author HaumeaMC
 */
public abstract class AbstractChatEvent implements ChatEvent {

    protected static final Random RANDOM = new Random();

    protected String question;
    protected String answer;
    protected String hint;
    protected List<String> acceptedAnswers;
    protected int difficulty;
    protected boolean customMode;

    public AbstractChatEvent() {
        this.acceptedAnswers = new ArrayList<>();
        this.difficulty = 1;
        this.customMode = false;
    }

    @Override
    public String getQuestion() {
        return question;
    }

    @Override
    public String getHint() {
        return hint;
    }

    @Override
    public String getAnswer() {
        return answer;
    }

    @Override
    public List<String> getAcceptedAnswers() {
        return acceptedAnswers;
    }

    @Override
    public boolean isCorrect(String playerAnswer) {
        if (playerAnswer == null || playerAnswer.trim().isEmpty()) {
            return false;
        }

        String normalized = normalizeAnswer(playerAnswer);

        // Verificar resposta principal
        if (normalized.equalsIgnoreCase(normalizeAnswer(answer))) {
            return true;
        }

        // Verificar respostas aceitas alternativas
        for (String accepted : acceptedAnswers) {
            if (normalized.equalsIgnoreCase(normalizeAnswer(accepted))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int getDifficulty() {
        return difficulty;
    }

    /**
     * Normaliza a resposta removendo acentos e caracteres especiais
     */
    protected String normalizeAnswer(String answer) {
        if (answer == null)
            return "";

        return answer.trim()
                .toLowerCase()
                .replace("á", "a")
                .replace("à", "a")
                .replace("ã", "a")
                .replace("â", "a")
                .replace("é", "e")
                .replace("ê", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ô", "o")
                .replace("õ", "o")
                .replace("ú", "u")
                .replace("ç", "c")
                .replaceAll("[^a-z0-9 ]", "");
    }

    /**
     * Adiciona uma resposta aceita alternativa
     */
    protected void addAcceptedAnswer(String answer) {
        if (answer != null && !answer.trim().isEmpty()) {
            acceptedAnswers.add(answer);
        }
    }

    /**
     * Define a dificuldade do evento
     */
    protected void setDifficulty(int difficulty) {
        this.difficulty = Math.max(1, Math.min(3, difficulty));
    }

    /**
     * Obtém um elemento aleatório de um array
     */
    protected <T> T randomElement(T[] array) {
        if (array == null || array.length == 0)
            return null;
        return array[RANDOM.nextInt(array.length)];
    }

    /**
     * Obtém um elemento aleatório de uma lista
     */
    protected <T> T randomElement(List<T> list) {
        if (list == null || list.isEmpty())
            return null;
        return list.get(RANDOM.nextInt(list.size()));
    }

    @Override
    public void setCustomData(String customQuestion, String customAnswer) {
        this.question = customQuestion;
        this.answer = customAnswer;
        this.customMode = true;
        this.acceptedAnswers.clear();
        this.difficulty = 2; // Dificuldade média para customizados
        this.hint = null;
    }

    @Override
    public boolean isCustom() {
        return customMode;
    }
}
