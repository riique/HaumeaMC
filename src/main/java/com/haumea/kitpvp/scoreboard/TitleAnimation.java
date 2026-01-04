package com.haumea.kitpvp.scoreboard;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe responsável pela animação premium do título da scoreboard.
 * Cria um efeito visual animado moderno com gradiente de cores
 * seguindo a paleta: Branco, Ouro e Azul/Ciano.
 * 
 * @author HaumeaMC
 */
public class TitleAnimation {

    private final List<String> frames;
    private int currentFrame;

    /**
     * Construtor padrão com animação premium HAUMEAMC
     */
    public TitleAnimation() {
        this.frames = new ArrayList<>();
        this.currentFrame = 0;
        initFrames();
    }

    /**
     * Inicializa os frames da animação premium
     * NOTA: Limite de 32 caracteres para displayName do Objective no Minecraft 1.8
     * Cada frame deve ter no máximo 32 caracteres incluindo códigos de cor
     */
    private void initFrames() {
        // ========== ANIMAÇÃO SIMPLIFICADA (máx 32 chars cada) ==========
        // Formato: "§6§lHAUMEA§e§lMC" = 16 caracteres

        // Fase 1: Cor principal dourada/amarela alternando
        frames.add("§6§lHAUMEA§e§lMC"); // 16 chars ✓
        frames.add("§e§lHAUMEA§6§lMC"); // 16 chars ✓

        // Fase 2: Efeito de brilho branco passando
        frames.add("§f§lH§6§lAUMEA§e§lMC"); // 20 chars ✓
        frames.add("§6§lHA§f§lU§6§lMEA§e§lMC"); // 24 chars ✓
        frames.add("§6§lHAUM§f§lE§6§lA§e§lMC"); // 24 chars ✓
        frames.add("§6§lHAUMEA§f§lM§e§lC"); // 20 chars ✓

        // Fase 3: Pulsação dourada
        frames.add("§6§lHAUMEAMC"); // 12 chars ✓
        frames.add("§e§lHAUMEAMC"); // 12 chars ✓
        frames.add("§6§lHAUMEAMC"); // 12 chars ✓

        // Fase 4: Branco completo (destaque)
        frames.add("§f§lHAUMEAMC"); // 12 chars ✓

        // Fase 5: Gradiente suave
        frames.add("§6§lHAU§e§lMEA§6§lMC"); // 20 chars ✓
        frames.add("§e§lHAU§6§lMEA§e§lMC"); // 20 chars ✓

        // Fase 6: Pausa no principal (repetido para duração)
        frames.add("§6§lHAUMEA§e§lMC"); // 16 chars ✓
        frames.add("§6§lHAUMEA§e§lMC"); // 16 chars ✓
        frames.add("§6§lHAUMEA§e§lMC"); // 16 chars ✓
    }

    /**
     * Avança para o próximo frame e retorna o título atual
     * 
     * @return Título do frame atual
     */
    public String next() {
        String frame = frames.get(currentFrame);
        currentFrame = (currentFrame + 1) % frames.size();
        return frame;
    }

    /**
     * Obtém o título do frame atual sem avançar
     * 
     * @return Título do frame atual
     */
    public String current() {
        return frames.get(currentFrame);
    }

    /**
     * Reseta a animação para o início
     */
    public void reset() {
        currentFrame = 0;
    }

    /**
     * Obtém o total de frames na animação
     * 
     * @return Número de frames
     */
    public int getTotalFrames() {
        return frames.size();
    }
}
