package com.haumea.kitpvp.commands.base;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation para definir metadados de um comando.
 * Usado pelo sistema de registro automático.
 * 
 * Exemplo de uso:
 * @CommandInfo(
 * name = "tag",
 * aliases = {"tags"},
 * description = "Gerencia suas tags",
 * usage = "/tag [nome]",
 * permission = "haumea.tag",
 * playerOnly = true
 * )
 * 
 * @author HaumeaMC
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandInfo {

    /**
     * Nome principal do comando (sem a barra)
     */
    String name();

    /**
     * Aliases/apelidos do comando
     */
    String[] aliases() default {};

    /**
     * Descrição do comando
     */
    String description() default "";

    /**
     * Uso do comando
     */
    String usage() default "";

    /**
     * Permissão necessária (vazio = nenhuma)
     */
    String permission() default "";

    /**
     * Se true, apenas jogadores podem usar o comando
     */
    boolean playerOnly() default false;

    /**
     * Se true, apenas o console pode usar o comando
     */
    boolean consoleOnly() default false;

    /**
     * Grupos que podem usar o comando (verificação por grupo)
     * Se vazio, não verifica grupo
     */
    String[] allowedGroups() default {};
}
