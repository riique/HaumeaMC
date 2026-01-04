package com.haumea.kitpvp.models.chatevents.impl;

import com.haumea.kitpvp.models.chatevents.AbstractChatEvent;
import com.haumea.kitpvp.models.chatevents.ChatEventType;

/**
 * Evento Sequência - Descubra o próximo número da sequência.
 * 
 * @author HaumeaMC
 */
public class SequenceEvent extends AbstractChatEvent {

    private int[] sequence;
    private int nextNumber;

    @Override
    public ChatEventType getType() {
        return ChatEventType.SEQUENCE;
    }

    @Override
    public void generate() {
        acceptedAnswers.clear();

        int diffLevel = RANDOM.nextInt(3) + 1;
        setDifficulty(diffLevel);

        switch (diffLevel) {
            case 1:
                generateEasySequence();
                break;
            case 2:
                generateMediumSequence();
                break;
            case 3:
                generateHardSequence();
                break;
            default:
                generateEasySequence();
        }

        answer = String.valueOf(nextNumber);

        StringBuilder sb = new StringBuilder();
        sb.append("§e§l");
        for (int i = 0; i < sequence.length; i++) {
            sb.append(sequence[i]);
            if (i < sequence.length - 1) {
                sb.append(", ");
            }
        }
        sb.append(", §c§l?");
        question = sb.toString();

        hint = "§7Dica: Observe o padrão entre os números";
    }

    /**
     * Sequências fáceis: soma constante
     */
    private void generateEasySequence() {
        int start = RANDOM.nextInt(10) + 1; // 1-10
        int increment = RANDOM.nextInt(5) + 1; // 1-5

        sequence = new int[4];
        for (int i = 0; i < 4; i++) {
            sequence[i] = start + (i * increment);
        }
        nextNumber = start + (4 * increment);
    }

    /**
     * Sequências médias: multiplicação ou soma variável
     */
    private void generateMediumSequence() {
        int type = RANDOM.nextInt(3);

        switch (type) {
            case 0: // Multiplicação por 2
                int start = RANDOM.nextInt(3) + 1; // 1-3
                sequence = new int[4];
                for (int i = 0; i < 4; i++) {
                    sequence[i] = start * (int) Math.pow(2, i);
                }
                nextNumber = start * (int) Math.pow(2, 4);
                break;

            case 1: // Fibonacci-like
                int a = RANDOM.nextInt(3) + 1; // 1-3
                int b = RANDOM.nextInt(3) + 1; // 1-3
                sequence = new int[5];
                sequence[0] = a;
                sequence[1] = b;
                for (int i = 2; i < 5; i++) {
                    sequence[i] = sequence[i - 1] + sequence[i - 2];
                }
                nextNumber = sequence[4] + sequence[3];
                // Ajustar para mostrar 4 números
                int[] temp = new int[4];
                System.arraycopy(sequence, 0, temp, 0, 4);
                sequence = temp;
                nextNumber = sequence[3] + sequence[2];
                break;

            case 2: // Soma crescente (+1, +2, +3, +4...)
                start = RANDOM.nextInt(5) + 1; // 1-5
                sequence = new int[4];
                sequence[0] = start;
                for (int i = 1; i < 4; i++) {
                    sequence[i] = sequence[i - 1] + i;
                }
                nextNumber = sequence[3] + 4;
                break;
        }
    }

    /**
     * Sequências difíceis: padrões mais complexos
     */
    private void generateHardSequence() {
        int type = RANDOM.nextInt(3);

        switch (type) {
            case 0: // Quadrados
                int start = RANDOM.nextInt(3) + 1; // 1-3
                sequence = new int[4];
                for (int i = 0; i < 4; i++) {
                    sequence[i] = (start + i) * (start + i);
                }
                nextNumber = (start + 4) * (start + 4);
                break;

            case 1: // Potências de 3
                sequence = new int[4];
                for (int i = 0; i < 4; i++) {
                    sequence[i] = (int) Math.pow(3, i);
                }
                nextNumber = (int) Math.pow(3, 4);
                break;

            case 2: // Alternância soma/subtração
                start = 20 + RANDOM.nextInt(20); // 20-39
                int op = RANDOM.nextInt(5) + 2; // 2-6
                sequence = new int[5];
                sequence[0] = start;
                for (int i = 1; i < 5; i++) {
                    if (i % 2 == 1) {
                        sequence[i] = sequence[i - 1] + op;
                    } else {
                        sequence[i] = sequence[i - 1] - (op / 2);
                    }
                }
                if (5 % 2 == 1) {
                    nextNumber = sequence[4] + op;
                } else {
                    nextNumber = sequence[4] - (op / 2);
                }
                // Ajustar para mostrar 4 números
                int[] temp = new int[4];
                System.arraycopy(sequence, 0, temp, 0, 4);
                sequence = temp;
                nextNumber = sequence[3] - (op / 2); // Próximo é subtração
                break;
        }
    }
}
