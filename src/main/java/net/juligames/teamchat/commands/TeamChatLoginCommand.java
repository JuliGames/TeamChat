package net.juligames.teamchat.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.ProxyServer;
import net.juligames.core.adventure.api.AudienceMessageRecipient;
import net.juligames.core.api.API;
import net.juligames.teamchat.TeamChatPlugin;
import net.kyori.adventure.identity.Identity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * @author Ture Bentzin
 * 04.02.2023
 */
public class TeamChatLoginCommand {

    private TeamChatLoginCommand() {
    }

    public static @NotNull BrigadierCommand createBrigadierCommand(final @NotNull TeamChatPlugin teamChatPlugin) {
        ProxyServer proxy = teamChatPlugin.getServer();
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("tclogin")
                .requires(source -> source.hasPermission("teamchat.use"))
                .requires(commandSource -> commandSource.pointers().supports(Identity.UUID))
                .executes(context -> {
                    CommandSource source = context.getSource();
                    @Nullable
                    UUID identity = source.get(Identity.UUID).orElse(null);
                    if (identity != null) {
                        //we have a identifiable audience here... so far so good
                        if (teamChatPlugin.teamChatters().contains(identity)) {
                            API.get().getMessageApi().sendMessage("teamchat.already", AudienceMessageRecipient.getByPointer(source));
                        } else {
                            teamChatPlugin.teamChatters().add(identity);
                            API.get().getMessageApi().sendMessage("teamchat.login", AudienceMessageRecipient.getByPointer(source));
                            teamChatPlugin.sendJoinMessage(source.get(Identity.NAME).orElseThrow());
                        }
                    } else {
                        return 0;
                    }

                    // Returning BrigadierCommand.FORWARD will send the command to the server
                    return Command.SINGLE_SUCCESS;
                }).build();

        // BrigadierCommand implements Command
        return new BrigadierCommand(node);
    }
}
