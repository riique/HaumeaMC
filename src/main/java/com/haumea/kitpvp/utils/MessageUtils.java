package com.haumea.kitpvp.utils;

/**
 * Utilitários para mensagens do plugin
 * 
 * @author HaumeaMC
 */
public class MessageUtils {

    /**
     * Converte códigos de cor (&) para códigos do Minecraft (§)
     * 
     * @param message Mensagem com códigos &
     * @return Mensagem com códigos § aplicados
     */
    public static String colorize(String message) {
        if (message == null)
            return "";
        return message.replace("&", "§");
    }

    /**
     * Converte um array de strings para uma única string colorida
     * 
     * @param messages Array de mensagens
     * @return String única com linhas separadas por \n
     */
    public static String colorize(String... messages) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < messages.length; i++) {
            builder.append(colorize(messages[i]));
            if (i < messages.length - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    /**
     * Remove códigos de cor de uma mensagem
     * 
     * @param message Mensagem com códigos de cor
     * @return Mensagem sem cores
     */
    public static String stripColors(String message) {
        if (message == null)
            return "";
        return message.replaceAll("[&§][0-9a-fA-Fk-oK-OrR]", "");
    }
}
