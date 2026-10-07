package io.github.kltyton.kltytonui.network.packet;

import io.github.kltyton.kltytonui.KltytonUI;
import io.github.kltyton.kltytonui.element.ContainerDeclaration;
import io.github.kltyton.kltytonui.network.api.INetworkContext;
import io.github.kltyton.kltytonui.network.api.INetworkPacket;
import io.github.kltyton.kltytonui.network.api.NetworkPacket;
import io.github.kltyton.kltytonui.network.api.Side;
import io.github.kltyton.kltytonui.spi.KuiServices;

import java.util.List;

@NetworkPacket(modId = KltytonUI.MODID, id = "open_screen", side = Side.SERVER)
public record OpenScreenRequestPacket(String templatePath, List<ContainerDeclaration> containers)
        implements INetworkPacket<OpenScreenRequestPacket> {
    public OpenScreenRequestPacket(String templatePath) { this(templatePath, List.of()); }
    public OpenScreenRequestPacket {
        templatePath = templatePath == null ? "" : templatePath;
        containers = containers == null ? List.of() : List.copyOf(containers);
    }
    @Override
    public void handle(INetworkContext context) {
        if (context.sender() != null) {
            KuiServices.network().openScreen(context.sender(), templatePath, containers);
        }
    }
}
