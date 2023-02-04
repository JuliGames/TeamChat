package net.juligames.teamchat;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import net.juligames.core.api.API;
import net.juligames.core.api.jdbi.ReplacementDAO;
import net.juligames.core.api.jdbi.mapper.bean.ReplacementBean;
import net.juligames.core.api.message.MessageApi;
import net.juligames.core.api.message.MessageRecipient;
import net.juligames.core.velocity.VelocityPlayerMessageRecipient;
import net.juligames.teamchat.test.TestBrigadierCommand;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * @author Ture Bentzin
 * 04.02.2023
 */

@Plugin(id = "teamchat", name = "TeamChat",version = "1.0-SNAPSHOT", description = "TeamChat Plugin for JuliGamesCore",
        authors = {"Ture Bentzin"}, dependencies = @Dependency(id = "velocitycore"))
public class TeamChatPlugin {

    private final ProxyServer server;
    private final Logger logger;

    @Inject
    public TeamChatPlugin(ProxyServer server, @NotNull Logger logger) {
        this.server = server;
        this.logger = logger;

        logger.info("TeamChat is booting up");
        registerReplacer();
        registerMessages();
    }

    private void registerReplacer() {
        ReplacementBean bean = new ReplacementBean();
        bean.setTag("teamchat_prefix");
        bean.setValue("<gray>[<green>TeamChat</green>]: ");
        bean.setReplacementType("TEXT_CLOSING");
        API.get().getSQLManager().getJdbi().withExtension(ReplacementDAO.class,extension -> {
            extension.insert(bean);
            return null;
        });
    }

    private void registerMessages() {
        MessageApi messageApi = API.get().getMessageApi();
        messageApi.registerMessage("teamchat.login","<teamchat_prefix><green>You are now logged in!");
        messageApi.registerMessage("teamchat.logout","<teamchat_prefix><red>You are now logged out!");
        messageApi.registerMessage("teamchat.failure","<teamchat_prefix><red>You are not logged in!");
        messageApi.registerMessage("teamchat.already","<teamchat_prefix><red>You are logged in already!");
        messageApi.registerMessage("teamchat.message","<teamchat_prefix><gold>@{0}: <gray>{1}");
        messageApi.registerMessage("teamchat.join","<teamchat_prefix><green> + <gold>{0}}");
        messageApi.registerMessage("teamchat.leave","<teamchat_prefix><red> - <gold>{0}}");
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // Do some operation demanding access to the Velocity API here.
        // For instance, we could register an event:
        server.getCommandManager().register(TestBrigadierCommand.createBrigadierCommand(server));
    }

    public List<UUID> teamChatters() {
        //noinspection SpellCheckingInspection
        return API.get().getHazelDataApi().getList("teamchat_login");
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getServer() {
        return server;
    }

    public Collection<MessageRecipient> collectRecipients() {
        return getServer().getAllPlayers().stream().filter(player -> teamChatters().contains(player.getUniqueId()))
                .map(VelocityPlayerMessageRecipient::new)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void sendTCMessage(String senderName, String message) {

            API.get().getMessageApi().sendMessage("teamchat.message");
    }
}
