package com.haumea.kitpvp.models.chatevents.impl;

import com.haumea.kitpvp.models.chatevents.AbstractChatEvent;
import com.haumea.kitpvp.models.chatevents.ChatEventType;

/**
 * Evento de Matemática - Resolva a conta.
 * Gera operações matemáticas de diferentes dificuldades.
 * 
 * @author HaumeaMC
 */
public class MathEvent extends AbstractChatEvent {

    private int num1;
    private int num2;
    private int num3;
    private String operation;
    private int result;

    @Override
    public ChatEventType getType() {
        return ChatEventType.MATH;
    }

    @Override
    public void generate() {
        acceptedAnswers.clear();

        int diffLevel = RANDOM.nextInt(3) + 1; // 1-3
        setDifficulty(diffLevel);

        switch (diffLevel) {
            case 1: // Fácil: soma/subtração simples
                generateEasy();
                break;
            case 2: // Médio: multiplicação ou operações maiores
                generateMedium();
                break;
            case 3: // Difícil: múltiplas operações
                generateHard();
                break;
            default:
                generateEasy();
        }

        answer = String.valueOf(result);
        hint = "§7Dica: O resultado é um número " + (result >= 0 ? "positivo" : "negativo");
    }

    private void generateEasy() {
        int op = RANDOM.nextInt(2); // 0 = soma, 1 = subtração
        num1 = RANDOM.nextInt(100) + 1; // 1-100
        num2 = RANDOM.nextInt(100) + 1; // 1-100

        if (op == 0) {
            operation = "+";
            result = num1 + num2;
            question = "§e§l" + num1 + " + " + num2 + " = ?";
        } else {
            // Garantir resultado positivo
            if (num2 > num1) {
                int temp = num1;
                num1 = num2;
                num2 = temp;
            }
            operation = "-";
            result = num1 - num2;
            question = "§e§l" + num1 + " - " + num2 + " = ?";
        }
    }

    private void generateMedium() {
        int op = RANDOM.nextInt(3); // 0 = soma grande, 1 = multiplicação, 2 = subtração grande

        switch (op) {
            case 0: // Soma grande
                num1 = RANDOM.nextInt(500) + 100; // 100-599
                num2 = RANDOM.nextInt(500) + 100; // 100-599
                operation = "+";
                result = num1 + num2;
                question = "§e§l" + num1 + " + " + num2 + " = ?";
                break;
            case 1: // Multiplicação
                num1 = RANDOM.nextInt(12) + 2; // 2-13
                num2 = RANDOM.nextInt(12) + 2; // 2-13
                operation = "×";
                result = num1 * num2;
                question = "§e§l" + num1 + " × " + num2 + " = ?";
                break;
            case 2: // Subtração grande
                num1 = RANDOM.nextInt(500) + 200; // 200-699
                num2 = RANDOM.nextInt(200) + 50; // 50-249
                operation = "-";
                result = num1 - num2;
                question = "§e§l" + num1 + " - " + num2 + " = ?";
                break;
        }
    }

    private void generateHard() {
        int op = RANDOM.nextInt(4);

        switch (op) {
            case 0: // Duas operações
                num1 = RANDOM.nextInt(50) + 10; // 10-59
                num2 = RANDOM.nextInt(50) + 10; // 10-59
                num3 = RANDOM.nextInt(50) + 10; // 10-59
                result = num1 + num2 + num3;
                question = "§e§l" + num1 + " + " + num2 + " + " + num3 + " = ?";
                break;
            case 1: // Multiplicação maior
                num1 = RANDOM.nextInt(20) + 10; // 10-29
                num2 = RANDOM.nextInt(20) + 10; // 10-29
                operation = "×";
                result = num1 * num2;
                question = "§e§l" + num1 + " × " + num2 + " = ?";
                break;
            case 2: // Soma e multiplicação
                num1 = RANDOM.nextInt(10) + 2; // 2-11
                num2 = RANDOM.nextInt(10) + 2; // 2-11
                num3 = RANDOM.nextInt(50) + 10; // 10-59
                result = (num1 * num2) + num3;
                question = "§e§l(" + num1 + " × " + num2 + ") + " + num3 + " = ?";
                break;
            case 3: // Divisão (resultado exato)
                num2 = RANDOM.nextInt(10) + 2; // 2-11
                result = RANDOM.nextInt(20) + 5; // 5-24
                num1 = num2 * result; // Garante divisão exata
                question = "§e§l" + num1 + " ÷ " + num2 + " = ?";
                break;
        }
    }
}
