package com.haumea.kitpvp.models;

/**
 * Representa o rank completo de um jogador, incluindo liga e divisão.
 * 
 * Esta classe agrupa a EloLeague com a divisão atual do jogador,
 * facilitando a passagem de informações completas de rank.
 * 
 * @author HaumeaMC
 */
public class PlayerRank {

    private final EloLeague league;
    private final int division;
    private final int elo;

    /**
     * Construtor do PlayerRank
     * 
     * @param league   Liga atual
     * @param division Divisão atual (1-5)
     * @param elo      Elo atual do jogador
     */
    public PlayerRank(EloLeague league, int division, int elo) {
        this.league = league;
        this.division = division;
        this.elo = elo;
    }

    // ==================== GETTERS ====================

    public EloLeague getLeague() {
        return league;
    }

    public int getDivision() {
        return division;
    }

    public int getElo() {
        return elo;
    }

    // ==================== MÉTODOS DE CONVENIÊNCIA ====================

    /**
     * Obtém o nome completo do rank
     * Ex: "Gold III"
     */
    public String getFullName() {
        return league.getFullName(division);
    }

    /**
     * Obtém o nome colorizado completo
     * Ex: "§6✹ Gold III"
     */
    public String getFormattedName() {
        return league.getFormattedName(division);
    }

    /**
     * Obtém o símbolo colorizado
     * Ex: "§6✹"
     */
    public String getSymbol() {
        return league.getFormattedSymbol();
    }

    /**
     * Obtém a cor da liga
     */
    public String getColor() {
        return league.getFormattedColorCode();
    }

    /**
     * Obtém a ordem hierárquica do rank (liga * 10 + divisão)
     * Usado para comparações
     */
    public int getTotalOrder() {
        return league.getOrder() * 10 + division;
    }

    /**
     * Obtém as coins de recompensa desta divisão
     */
    public int getCoinsReward() {
        return league.getCoinsReward();
    }

    /**
     * Obtém o Elo necessário para este rank
     */
    public int getRequiredElo() {
        return league.getEloForDivision(division);
    }

    /**
     * Verifica se é a liga máxima (Legendary)
     */
    public boolean isMaxRank() {
        return !league.hasDivisions() && league.getId().equals("legendary");
    }

    /**
     * Compara este rank com outro
     * 
     * @param other Outro rank
     * @return Positivo se este for maior, negativo se menor, 0 se igual
     */
    public int compareTo(PlayerRank other) {
        return Integer.compare(this.getTotalOrder(), other.getTotalOrder());
    }

    @Override
    public String toString() {
        return "PlayerRank{" +
                "league=" + league.getId() +
                ", division=" + division +
                ", elo=" + elo +
                ", formattedName='" + getFormattedName() + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof PlayerRank))
            return false;
        PlayerRank other = (PlayerRank) obj;
        return this.league.getId().equals(other.league.getId()) && this.division == other.division;
    }

    @Override
    public int hashCode() {
        return league.getId().hashCode() * 31 + division;
    }
}
