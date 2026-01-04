package com.haumea.kitpvp.scoreboard;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper para scoreboard individual de um jogador.
 * Implementa sistema anti-flicker usando Teams do Bukkit.
 * 
 * A técnica usada é:
 * 1. Criar N equipes (uma por linha)
 * 2. Cada equipe tem uma entry única (código de cor invisível)
 * 3. O texto é dividido em prefix/suffix da equipe
 * 4. As pontuações são definidas APENAS UMA VEZ na inicialização
 * 5. Atualizações apenas modificam prefix/suffix (sem resetar scores)
 * 
 * @author HaumeaMC
 */
public class PlayerBoard {

    private static final int MAX_LINES = 15;
    private static final String OBJECTIVE_NAME = "haumeasb";

    private final Player player;
    private final Scoreboard scoreboard;
    private final Objective objective;
    private final Map<Integer, Team> teams;
    private final Map<Integer, String> lineEntries;
    private final Map<Integer, String> currentTexts;

    private int lineCount;
    private String currentTitle;
    private boolean initialized;

    /**
     * Cria uma nova scoreboard para o jogador
     * 
     * @param player Jogador dono da scoreboard
     */
    public PlayerBoard(Player player) {
        this.player = player;
        this.scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        this.teams = new HashMap<>();
        this.lineEntries = new HashMap<>();
        this.currentTexts = new HashMap<>();
        this.currentTitle = "";
        this.lineCount = 0;
        this.initialized = false;

        // Criar objetivo
        this.objective = scoreboard.registerNewObjective(OBJECTIVE_NAME, "dummy");
        this.objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Inicializar equipes para cada linha (anti-flicker)
        initTeams();

        // Aplicar scoreboard ao jogador
        player.setScoreboard(scoreboard);
    }

    /**
     * Inicializa as equipes usadas para cada linha
     * Cada equipe controla uma linha, permitindo atualização sem flicker
     */
    private void initTeams() {
        for (int i = 0; i < MAX_LINES; i++) {
            String teamName = "line" + i;
            Team team = scoreboard.registerNewTeam(teamName);

            // Entry único para cada linha usando códigos de cor invisíveis
            String entry = getUniqueEntry(i);
            team.addEntry(entry);

            teams.put(i, team);
            lineEntries.put(i, entry);
            currentTexts.put(i, "");
        }
    }

    /**
     * Gera uma entrada única para cada linha usando códigos de cor
     * 
     * @param line Número da linha
     * @return String única para a linha
     */
    private String getUniqueEntry(int line) {
        // Usar combinações de códigos de cor para criar entradas únicas
        // Formato: §X§Y onde X e Y são caracteres de cor diferentes
        StringBuilder entry = new StringBuilder();
        entry.append("§").append(Integer.toHexString(line % 16));
        entry.append("§").append(Integer.toHexString((line / 16) % 16));
        entry.append("§r");
        return entry.toString();
    }

    /**
     * Define as linhas da scoreboard
     * Usa sistema anti-flicker - apenas atualiza prefix/suffix
     * 
     * @param lines Linhas a exibir (máximo 15)
     */
    public void setLines(String... lines) {
        int newLineCount = Math.min(lines.length, MAX_LINES);

        // Se o número de linhas mudou, precisamos reconfigurar os scores
        if (!initialized || newLineCount != lineCount) {
            setupScores(newLineCount);
            lineCount = newLineCount;
            initialized = true;
        }

        // Atualizar apenas as linhas que mudaram
        for (int i = 0; i < newLineCount; i++) {
            String newText = lines[i];
            String currentText = currentTexts.get(i);

            // Só atualiza se o texto mudou
            if (!newText.equals(currentText)) {
                updateLineText(i, newText);
                currentTexts.put(i, newText);
            }
        }
    }

    /**
     * Configura as pontuações (scores) para cada linha
     * Isso é feito apenas quando o número de linhas muda
     * 
     * @param count Número de linhas
     */
    private void setupScores(int count) {
        // Primeiro, limpar todas as pontuações antigas
        for (int i = 0; i < MAX_LINES; i++) {
            String entry = lineEntries.get(i);
            scoreboard.resetScores(entry);
        }

        // Definir novas pontuações (só as linhas ativas)
        for (int i = 0; i < count; i++) {
            String entry = lineEntries.get(i);
            // Score decrescente para que linha 0 fique no topo
            objective.getScore(entry).setScore(count - i);
        }
    }

    /**
     * Atualiza apenas o texto de uma linha (prefix/suffix)
     * Isso NÃO causa flicker porque não mexe nos scores
     * 
     * @param lineIndex Índice da linha
     * @param text      Novo texto
     */
    private void updateLineText(int lineIndex, String text) {
        Team team = teams.get(lineIndex);
        if (team == null)
            return;

        // Dividir texto em prefix e suffix (limite de 16 chars cada no 1.8)
        String prefix;
        String suffix = "";

        if (text == null) {
            text = "";
        }

        if (text.length() <= 16) {
            prefix = text;
        } else {
            // Dividir cuidadosamente para não quebrar códigos de cor
            int splitIndex = 16;

            // Verificar se cortou no meio de um código de cor
            if (text.charAt(15) == '§') {
                splitIndex = 15;
            }

            prefix = text.substring(0, splitIndex);
            suffix = text.substring(splitIndex);

            // Adicionar última cor do prefix ao início do suffix
            String lastColors = ChatColor.getLastColors(prefix);
            suffix = lastColors + suffix;

            // Limitar suffix a 16 caracteres
            if (suffix.length() > 16) {
                suffix = suffix.substring(0, 16);
            }
        }

        team.setPrefix(prefix);
        team.setSuffix(suffix);
    }

    /**
     * Define o título da scoreboard
     * 
     * @param title Novo título
     */
    public void setTitle(String title) {
        if (!title.equals(currentTitle)) {
            objective.setDisplayName(title);
            currentTitle = title;
        }
    }

    /**
     * Remove a scoreboard do jogador
     */
    public void remove() {
        // Restaurar scoreboard padrão do servidor
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    /**
     * Obtém o jogador dono desta scoreboard
     * 
     * @return Player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Verifica se o jogador está online
     * 
     * @return true se online
     */
    public boolean isValid() {
        return player != null && player.isOnline();
    }

    /**
     * Obtém a scoreboard interna
     * 
     * @return Scoreboard
     */
    public Scoreboard getScoreboard() {
        return scoreboard;
    }
}
