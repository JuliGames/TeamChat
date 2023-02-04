package net.juligames.teamchat.commands;

import com.google.protobuf.Internal;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import net.juligames.core.adventure.api.AudienceMessageRecipient;
import net.juligames.core.api.API;
import net.juligames.core.api.message.MessageApi;
import net.juligames.teamchat.TeamChatPlugin;
import net.kyori.adventure.identity.Identity;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static net.juligames.teamchat.TeamChatPlugin.DEVELOPER;
import static net.juligames.teamchat.TeamChatPlugin.NAME;

/**
 * @author Ture Bentzin
 * 04.02.2023
 */
public class TeamChatCommand {
    private TeamChatCommand() {
    }

    public static @NotNull BrigadierCommand createAlias(TeamChatPlugin teamChatPlugin, String literal, @NotNull BrigadierCommand brigadierCommand) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal(literal)
                .redirect(brigadierCommand.getNode()).build();
        return new BrigadierCommand(node);
    }

    public static @NotNull BrigadierCommand createBrigadierCommand(TeamChatPlugin teamChatPlugin) {
        final LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("tc")
                .requires(commandSource -> commandSource.hasPermission("teamchat.use"))
                .requires(commandSource -> commandSource.pointers().supports(Identity.UUID))

                .executes(context -> {
                    //list and info
                    AudienceMessageRecipient recipient = AudienceMessageRecipient.getByPointer(context.getSource());
                    MessageApi messageApi = API.get().getMessageApi();
                    messageApi.sendMessage("teamchat.info.divider", recipient, array(NAME));
                    messageApi.sendMessage("teamchat.info.header.1", recipient, array(NAME));
                    messageApi.sendMessage("teamchat.info.header.2", recipient, array("by " + DEVELOPER));
                    messageApi.sendMessage("teamchat.null", recipient);
                    messageApi.sendMessage("teamchat.info.body.online", recipient);

                    final Collection<Player> players = new ArrayList<>();
                    teamChatPlugin.teamChatters().forEach(uuid -> {
                        Optional<Player> optionalPlayer = teamChatPlugin.getServer().getPlayer(uuid);
                        optionalPlayer.ifPresent(players::add);
                    });

                    players.forEach(player -> {
                        ServerConnection connection = player.getCurrentServer().orElse(null);
                        if(connection != null)
                            messageApi.sendMessage("teamchat.info.body.entry", recipient, array(player.getUsername(),
                                connection.getServer().getServerInfo().getName()));
                        else
                                messageApi.sendMessage("teamchat.info.body.entry", recipient, array(player.getUsername(), "<red>/</red>"));
                    });
                    if(players.isEmpty()) {
                        messageApi.sendMessage("teamchat.info.body.offline", recipient);
                    }
                    messageApi.sendMessage("teamchat.info.divider", recipient, array(NAME));

                    return Command.SINGLE_SUCCESS;
                })
                .then((RequiredArgumentBuilder.<CommandSource, String>argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            CommandSource source = context.getSource();
                            UUID uuid = source.get(Identity.UUID).orElseThrow();
                            if (teamChatPlugin.teamChatters().contains(uuid)) {
                                String message = context.getArgument("message", String.class);
                                teamChatPlugin.sendTCMessage(source.get(Identity.NAME).orElseThrow(), message);
                            } else {
                                API.get().getMessageApi().sendMessage("teamchat.failure", AudienceMessageRecipient.getByPointer(source));
                            }
                            return Command.SINGLE_SUCCESS;
                        })))
                .build();

        return new BrigadierCommand(node);

    }

    public static String[] array(String... vaargs) {
        return vaargs;
    }
}
