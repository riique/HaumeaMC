package com.haumea.kitpvp.models;

/**
 * Modelo de Medalha para o sistema de Medalhas do HaumeaMC.
 * 
 * Cada medalha possui um símbolo unicode e uma cor específica
 * que são exibidos no chat e na tablist do jogador.
 * 
 * @author HaumeaMC
 */
public class Medal {

    private final String name;
    private final String symbol;
    private final String colorCode;
    private final String permission;

    /**
     * Construtor de uma medalha
     * 
     * @param name      Nome único da medalha (ex: "PEACE_LOVE")
     * @param symbol    Símbolo unicode da medalha (ex: "✌")
     * @param colorCode Código de cor sem o § (ex: "a" para verde)
     */
    public Medal(String name, String symbol, String colorCode) {
        this.name = name.toUpperCase();
        this.symbol = symbol;
        this.colorCode = colorCode;
        this.permission = "medal." + name.toLowerCase();
    }

    /**
     * Obtém o nome da medalha
     * 
     * @return Nome em uppercase (ex: "PEACE_LOVE")
     */
    public String getName() {
        return name;
    }

    /**
     * Obtém o símbolo unicode da medalha
     * 
     * @return Símbolo unicode (ex: "✌")
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Obtém o código de cor sem o §
     * 
     * @return Código de cor (ex: "a" para verde)
     */
    public String getColorCode() {
        return colorCode;
    }

    /**
     * Obtém a permissão necessária para usar esta medalha
     * 
     * @return Permissão (ex: "medal.peace_love")
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Obtém o display formatado da medalha (cor + símbolo)
     * Para uso no chat e tablist
     * 
     * @return Display formatado (ex: "§a✌")
     */
    public String getDisplay() {
        return "§" + colorCode + symbol;
    }

    /**
     * Obtém o display com espaço para uso em prefixos
     * 
     * @return Display com espaço (ex: "§a✌ ")
     */
    public String getDisplayWithSpace() {
        return getDisplay() + " ";
    }

    /**
     * Obtém o display para exibição na lista de seleção
     * Inclui o nome da medalha
     * 
     * @return Display formatado para lista (ex: "§a✌ §7(PEACE_LOVE)")
     */
    public String getListDisplay() {
        return getDisplay() + " §7(" + name + ")";
    }

    @Override
    public String toString() {
        return "Medal{" +
                "name='" + name + '\'' +
                ", symbol='" + symbol + '\'' +
                ", color='" + colorCode + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Medal) {
            return name.equalsIgnoreCase(((Medal) obj).name);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return name.toLowerCase().hashCode();
    }
}
