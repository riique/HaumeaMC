package com.haumea.kitpvp.utils;

import java.util.regex.Pattern;

/**
 * Utilitários para validação de input em comandos.
 * 
 * Centraliza validações comuns para evitar repetição de código
 * e garantir consistência.
 * 
 * @author HaumeaMC
 */
public class InputValidator {

    // Padrões de validação
    private static final Pattern PLAYER_NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
    private static final Pattern POSITIVE_INTEGER = Pattern.compile("^[1-9]\\d*$");
    private static final Pattern POSITIVE_NUMBER = Pattern.compile("^\\d+(\\.\\d+)?$");
    private static final Pattern TIME_DURATION = Pattern.compile("^\\d+[smhd]$", Pattern.CASE_INSENSITIVE);

    /**
     * Valida se uma string é um nome de jogador válido.
     * 
     * @param name Nome a validar
     * @return true se válido (3-16 caracteres, a-z, 0-9, _)
     */
    public static boolean isValidPlayerName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return PLAYER_NAME_PATTERN.matcher(name).matches();
    }

    /**
     * Valida e parseia um número inteiro positivo.
     * 
     * @param input String a validar
     * @return Valor ou -1 se inválido
     */
    public static int parsePositiveInt(String input) {
        if (input == null || input.isEmpty()) {
            return -1;
        }

        try {
            int value = Integer.parseInt(input);
            return value > 0 ? value : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Valida e parseia um número inteiro (pode ser 0 ou positivo).
     * 
     * @param input String a validar
     * @return Valor ou -1 se inválido
     */
    public static int parseNonNegativeInt(String input) {
        if (input == null || input.isEmpty()) {
            return -1;
        }

        try {
            int value = Integer.parseInt(input);
            return value >= 0 ? value : -1;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Valida e parseia um número long positivo.
     * 
     * @param input String a validar
     * @return Valor ou -1 se inválido
     */
    public static long parsePositiveLong(String input) {
        if (input == null || input.isEmpty()) {
            return -1L;
        }

        try {
            long value = Long.parseLong(input);
            return value > 0 ? value : -1L;
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    /**
     * Parseia uma duração de tempo no formato Nx (onde x = s/m/h/d).
     * Ex: "30s" = 30 segundos, "5m" = 5 minutos, "2h" = 2 horas, "7d" = 7 dias
     * 
     * @param input String de duração
     * @return Duração em milissegundos ou -1 se inválido
     */
    public static long parseTimeDuration(String input) {
        if (input == null || input.isEmpty()) {
            return -1L;
        }

        input = input.toLowerCase().trim();

        // Verificar formato
        if (!TIME_DURATION.matcher(input).matches()) {
            return -1L;
        }

        char unit = input.charAt(input.length() - 1);
        String numberPart = input.substring(0, input.length() - 1);

        try {
            long value = Long.parseLong(numberPart);
            if (value <= 0)
                return -1L;

            switch (unit) {
                case 's':
                    return value * 1000L;
                case 'm':
                    return value * 60L * 1000L;
                case 'h':
                    return value * 60L * 60L * 1000L;
                case 'd':
                    return value * 24L * 60L * 60L * 1000L;
                default:
                    return -1L;
            }
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    /**
     * Formata uma duração em milissegundos para string legível.
     * Ex: 3600000 → "1 hora"
     * 
     * @param durationMs Duração em milissegundos
     * @return String formatada
     */
    public static String formatDuration(long durationMs) {
        if (durationMs < 0) {
            return "Permanente";
        }

        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + (days == 1 ? " dia" : " dias");
        } else if (hours > 0) {
            return hours + (hours == 1 ? " hora" : " horas");
        } else if (minutes > 0) {
            return minutes + (minutes == 1 ? " minuto" : " minutos");
        } else {
            return seconds + (seconds == 1 ? " segundo" : " segundos");
        }
    }

    /**
     * Valida se uma string não está vazia após trim.
     * 
     * @param input String a validar
     * @return true se não vazia
     */
    public static boolean isNotEmpty(String input) {
        return input != null && !input.trim().isEmpty();
    }

    /**
     * Valida se uma string tem comprimento dentro do range.
     * 
     * @param input String a validar
     * @param min   Comprimento mínimo
     * @param max   Comprimento máximo
     * @return true se está dentro do range
     */
    public static boolean isLengthInRange(String input, int min, int max) {
        if (input == null) {
            return false;
        }
        int length = input.length();
        return length >= min && length <= max;
    }

    /**
     * Sanitiza uma string removendo caracteres perigosos.
     * 
     * @param input String a sanitizar
     * @return String sanitizada
     */
    public static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        // Remove caracteres de controle e limita tamanho
        return input.replaceAll("[\\p{Cntrl}]", "")
                .substring(0, Math.min(input.length(), 256));
    }

    /**
     * Valida um motivo de punição.
     * 
     * @param reason Motivo
     * @return true se válido (1-200 caracteres, sem caracteres de controle)
     */
    public static boolean isValidReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return false;
        }
        return isLengthInRange(reason.trim(), 1, 200);
    }
}
