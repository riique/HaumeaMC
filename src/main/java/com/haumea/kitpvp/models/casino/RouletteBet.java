package com.haumea.kitpvp.models.casino;

/**
 * Representa um tipo de aposta na Roleta.
 * 
 * @author HaumeaMC
 */
public enum RouletteBet {

    // Apostas simples (2x)
    RED("Vermelho", 2.0),
    BLACK("Preto", 2.0),
    ODD("Ímpar", 2.0),
    EVEN("Par", 2.0),
    LOW("1-18", 2.0),
    HIGH("19-36", 2.0),

    // Dúzias (3x)
    FIRST_DOZEN("1ª Dúzia (1-12)", 3.0),
    SECOND_DOZEN("2ª Dúzia (13-24)", 3.0),
    THIRD_DOZEN("3ª Dúzia (25-36)", 3.0),

    // Colunas (3x)
    FIRST_COLUMN("1ª Coluna", 3.0),
    SECOND_COLUMN("2ª Coluna", 3.0),
    THIRD_COLUMN("3ª Coluna", 3.0),

    // Número específico (36x)
    NUMBER("Número", 36.0);

    private final String displayName;
    private final double multiplier;

    RouletteBet(String displayName, double multiplier) {
        this.displayName = displayName;
        this.multiplier = multiplier;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getMultiplier() {
        return multiplier;
    }

    /**
     * Verifica se o número é vencedor para esta aposta.
     * 
     * @param number    Número sorteado (0-36)
     * @param betNumber Número apostado (apenas para aposta NUMBER)
     */
    public boolean isWinner(int number, int betNumber) {
        // 0 (zero verde) perde em todas as apostas exceto NUMBER
        if (number == 0 && this != NUMBER) {
            return false;
        }

        switch (this) {
            case RED:
                return isRed(number);
            case BLACK:
                return !isRed(number) && number != 0;
            case ODD:
                return number % 2 == 1;
            case EVEN:
                return number % 2 == 0 && number != 0;
            case LOW:
                return number >= 1 && number <= 18;
            case HIGH:
                return number >= 19 && number <= 36;
            case FIRST_DOZEN:
                return number >= 1 && number <= 12;
            case SECOND_DOZEN:
                return number >= 13 && number <= 24;
            case THIRD_DOZEN:
                return number >= 25 && number <= 36;
            case FIRST_COLUMN:
                return number % 3 == 1;
            case SECOND_COLUMN:
                return number % 3 == 2;
            case THIRD_COLUMN:
                return number % 3 == 0 && number != 0;
            case NUMBER:
                return number == betNumber;
            default:
                return false;
        }
    }

    /**
     * Verifica se um número é vermelho na roleta.
     */
    public static boolean isRed(int number) {
        // Números vermelhos na roleta europeia
        int[] redNumbers = { 1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36 };
        for (int red : redNumbers) {
            if (red == number)
                return true;
        }
        return false;
    }

    /**
     * Verifica se um número é verde (zero).
     */
    public static boolean isGreen(int number) {
        return number == 0;
    }

    /**
     * Obtém a cor do número para display.
     */
    public static String getColor(int number) {
        if (number == 0)
            return "&a"; // Verde
        if (isRed(number))
            return "&c"; // Vermelho
        return "&8"; // Preto
    }

    /**
     * Obtém o display colorido do número.
     */
    public static String getColoredNumber(int number) {
        return getColor(number) + "&l" + number;
    }
}
