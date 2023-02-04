package com.ranull.proxychatbridge.velocity.provider;

import com.ranull.proxychatbridge.velocity.ProxyChatBridge;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;

public final class PlayerDataProvider {

    private final GroupProvider groupProvider;
    private final MetaProvider metaProvider;

    public PlayerDataProvider(ProxyChatBridge plugin) {

        if (plugin.getProxy().getPluginManager().isLoaded("LuckPerms")) {
            LuckPerms luckPerms = LuckPermsProvider.get();
            groupProvider = new LuckPermsGroupProvider(luckPerms);
            metaProvider = new LuckPermsMetaProvider(luckPerms);
        } else {
            groupProvider = new EmptyGroupProvider();
            metaProvider = new EmptyMetaProvider();
        }
    }

    public GroupProvider getGroupProvider() {
        return groupProvider;
    }

    public MetaProvider getMetaProvider() {
        return metaProvider;
    }

}
