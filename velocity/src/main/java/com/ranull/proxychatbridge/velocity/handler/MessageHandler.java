package com.ranull.proxychatbridge.velocity.handler;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.ranull.proxychatbridge.velocity.ProxyChatBridge;
import com.ranull.proxychatbridge.velocity.manager.ConfigManager;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.gauntletmc.adventure.serializer.binary.BinaryComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

import java.io.IOException;
import java.util.UUID;

public class MessageHandler {
    private final ProxyChatBridge plugin;
    private final ConfigManager config;

    public MessageHandler(ProxyChatBridge plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
    }

    /**
     * Handle the message received from a backend server.
     */
    public void handleIncomingMessage(final PluginMessageEvent event) {
        if (!(event.getIdentifier().equals(ProxyChatBridge.PLUGIN_MESSAGE_CHANNEL) && (event.getSource() instanceof ServerConnection source)))
            return;
        if (config.isDisabled(source.getServerInfo()))
            return;

        ByteArrayDataInput in = event.dataAsDataStream();

        String id = in.readUTF();
        if (!id.equals("Global")) return;
        UUID player = UUID.fromString(in.readUTF());
        try {
            int messageLength = in.readInt();
            byte[] componentBytes = new byte[messageLength];
            in.readFully(componentBytes);
            Component message = BinaryComponentSerializer.INSTANCE.deserialize(componentBytes);

            plugin.getMessageHandler().handleOutgoingMessage(player, message, source.getServerInfo());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Forward the message received from a backend server to others.
     */
    public void handleOutgoingMessage(UUID player, Component message, ServerInfo source) {
        String group = config.getGroup(source);
        plugin.getProxy().getAllServers().stream()
            .filter(server -> !server.getPlayersConnected().isEmpty()) // exclude all empty servers
            .filter(server -> !server.getServerInfo().equals(source)) // exclude the source server itself
            .filter(server -> config.getGroup(server.getServerInfo()).equals(group)) // select servers within the same group
            .forEach(server -> {
                @SuppressWarnings("UnstableApiUsage")
                ByteArrayDataOutput out = ByteStreams.newDataOutput();

                out.writeUTF("Global");
                out.writeUTF(player.toString());
                try {
                    String format = config.getFormat(source);
                    TagResolver resolver = TagResolver.builder().resolvers(
                        Placeholder.component("message", message),
                        Placeholder.unparsed("player", plugin.getProxy().getPlayer(player).map(Player::getUsername).orElse("Null"))
                    ).build();
                    Component fullMessage = MiniMessage.miniMessage().deserialize(format, resolver);
                    byte[] componentBytes = BinaryComponentSerializer.INSTANCE.serialize(fullMessage);
                    out.writeInt(componentBytes.length); // store length of the byte array
                    out.write(componentBytes);
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }

                boolean success = server.sendPluginMessage(ProxyChatBridge.PLUGIN_MESSAGE_CHANNEL, out.toByteArray());
                if (!success) plugin.getLogger().warn("Message cannot be sent to: " + server.getServerInfo().getName());
            });
    }

}