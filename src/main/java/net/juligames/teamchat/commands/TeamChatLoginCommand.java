package net.juligames.teamchat.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.VelocityBrigadierMessage;
import com.velocitypowered.api.proxy.ProxyServer;
import net.juligames.core.adventure.api.AudienceMessageRecipient;
import net.juligames.core.api.API;
import net.juligames.teamchat.TeamChatPlugin;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * @author Ture Bentzin
 * 04.02.2023
 */
public class TeamChatLoginCommand {

    public static @NotNull BrigadierCommand createBrigadierCommand(final TeamChatPlugin teamChatPlugin) {
        ProxyServer proxy = teamChatPlugin.getServer();
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder.<CommandSource>literal("tclogin")
                .requires(source -> source.hasPermission("teamchat.use"))
                .executes(context -> {
                    CommandSource source = context.getSource();
                    @Nullable
                    UUID identity = source.get(Identity.UUID).orElse(null);
                    if(identity != null) {
                        //we have a identifiable audience here... so far so good
                        if(teamChatPlugin.teamChatters().contains(identity)){
                            API.get().getMessageApi().sendMessage("teamchat.already", AudienceMessageRecipient.getByPointer(source));
                        }else {
                            API.get().getMessageApi().sendMessage("teamchat.login", AudienceMessageRecipient.getByPointer(source));
                        }
                    }else{
                        return 0;
                    }

                    // Returning BrigadierCommand.FORWARD will send the command to the server
                    return Command.SINGLE_SUCCESS;
                })
                // Using the "then" method, you can add subarguments to the command.
                // For example, this subcommand will be executed when using the command "/test <some argument>"
                // A RequiredArgumentBuilder is a type of argument in which you can enter some undefined data
                // of some kind. For example, this example uses a StringArgumentType.word() that requires
                // a single word to be entered, but you can also use different ArgumentTypes provided by Brigadier
                // that return data of type Boolean, Integer, Float, other String types, etc
                .then(RequiredArgumentBuilder.<CommandSource, String>argument("argument", StringArgumentType.word())
                        // Here you can define the hints to be provided in case the ArgumentType does not provide them.
                        // In this example, the names of all connected players are provided
                        .suggests((ctx, builder) -> {
                                    // Here we provide the names of the players along with a tooltip,
                                    // which can be used as an explanation of a specific argument or as a simple decoration
                                    proxy.getAllPlayers().forEach(player -> builder.suggest(
                                            player.getUsername(),
                                            VelocityBrigadierMessage.tooltip(
                                                    MiniMessage.miniMessage().deserialize("<rainbow>" + player.getUsername())
                                            )
                                    ));
                                    return builder.buildFuture();
                                }
                        )
                        // Here the logic of the command "/test <some argument>" is executed
                        .executes(context -> {
                            // Here you get the argument that the CommandSource has entered.
                            // You must enter exactly the name as you have named the argument
                            // and you must provide the class of the argument you expect, in this case... a String
                            String argumentProvided = context.getArgument("argument", String.class);
                            // This method will check if the given string corresponds to a
                            // player's name and if it does, it will send a message to that player
                            proxy.getPlayer(argumentProvided).ifPresent(player ->
                                    player.sendMessage(Component.text("Hello!"))
                            );
                            // Returning Command.SINGLE_SUCCESS means that the execution was successful
                            // Returning BrigadierCommand.FORWARD will send the command to the server
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .build();

        // BrigadierCommand implements Command
        return new BrigadierCommand(node);
    }
}
