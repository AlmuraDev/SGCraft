//------------------------------------------------------------------------------------------------
//
//   SG Craft - General/Shared code used by the rest of the SGCraft worldgen system
//
//------------------------------------------------------------------------------------------------

package gcewing.sg.command;

import gcewing.sg.BaseOrientation;
import gcewing.sg.SGCraft;
import gcewing.sg.block.SGRingBlock;
import gcewing.sg.features.configurator.network.ConfiguratorNetworkHandler;
import gcewing.sg.tileentity.DHDTE;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.util.GateType;
import gcewing.sg.util.GateUtil;
import gcewing.sg.util.SGAddressing;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nullable;
import java.util.*;

public class StargateCommands extends CommandBase {
    private static final String PREFIX = "message.sgcraft.command.gate.info:";

    @Override
    @NonNull
    public String getName() {
        return "stargate";
    }

    @Override
    @NonNull
    public List<String> getAliases() {
        return Arrays.asList("gate", "stargate");
    }

    @Override
    @NonNull
    public String getUsage(@NonNull ICommandSender sender) {
        return new TextComponentTranslation("message.sgcraft.command.gate:Usage", String.join(", ", subcommands)).getFormattedText();
    }

    private static final List<String> subcommands = new ArrayList<>();

    static {
        subcommands.add("info");
        subcommands.add("place");
        subcommands.add("address");
        subcommands.add("reset");
        subcommands.add("debug");
        subcommands.add("state");
        subcommands.add("dial");
        subcommands.add("disconnect");
        subcommands.add("type");

        if (SGCraft.isModLoaded("malisiscore")) {
            subcommands.add("configurator");
        }
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length<=1) {
            return subcommands;
        }

        switch (args[0]) {
            case "place":
            case "type": {
                ArrayList<String> l = new ArrayList<>(GateType.values().length);
                for (GateType e: GateType.values()) {
                    l.add(e.name().toLowerCase());
                } return l; }
            case "info": return Collections.singletonList("remote");
            default: return super.getTabCompletions(server, sender, args, targetPos);
        }

    }

    public void execute(@NonNull MinecraftServer server, @NonNull ICommandSender sender, String @NonNull [] args) throws CommandException {
        SGBaseTE te = getStargate(sender);
        switch (args[0].toLowerCase()) {
            case "configurator": {
                if (SGCraft.isModLoaded("malisiscore")) {
                    openConfiguratorScreen(sender, te);
                } else {
                    throw new CommandNotFoundException("Malisiscore not loaded, unable to open menu");
                }
            }
            case "address":
            case "add": sendMessage(sender, "message.sgcraft.command.gate.info:HomeAddress", getHomeAddress(te)); break;
            case "dis":
            case "disconnect": sendMessage(sender, "sgcraft.gui.pdd.label.disconnecting"); te.disconnect(); break;
            case "reset": {
                te.clearConnection();
                te.clearIdleConnection();
                te.resetStargate();
            } break;
            case "state": sendMessage(sender, "message.sgcraft.command.gate.info:State", te.sgStateDescription()); break;
            case "connect":
            case "dial": {
                if (args.length!=2) throw new CommandException("message.sgcraft:malformedAddress");
                String destination = SGAddressing.normalizeAddress(args[1]);
                try {SGAddressing.validateAddress(destination); } catch (SGAddressing.AddressingError e) {
                    throw new CommandException(e.getMessage()); }
                try { te.connect(destination, getCommandSenderAsPlayer(sender), false, false); }
                catch (PlayerNotFoundException ex) { te.connect(destination, null, false, false); }
            } break;
            case "type": if (args.length==1) {
                    sendMessage(sender, "message.sgcraft.command.gate.info:GateType", te.getGateType().name());
                } else try {te.setGateType(GateType.valueOf(args[1].toUpperCase()));} catch (IllegalArgumentException e) {
                        throw new CommandException("message.sgcraft.command.gate:InvalidGateType", args[1]);} break;
            case "info": {
                if (args.length==2) {
                    info(sender, te.getConnectedStargateTE());
                } else info(sender, te); break;
            }
            default: sendMessage(sender,"message.sgcraft.command.gate:Usage", String.join(", ", subcommands));
        }

    }

    private void info(ICommandSender s, @NonNull SGBaseTE g) {
        sendMessage(s,PREFIX+"ChunkPos", g.getWorld().provider.getDimension(), g.getX()>>4, g.getZ()>>4);
        sendMessage(s, PREFIX+"BlockPos", g.getX(), g.getY(), g.getZ());
        sendMessage(s,PREFIX+"HomeAddress", getHomeAddress(g));
        sendMessage(s,PREFIX+"State", g.sgStateDescription());
        sendMessage(s, PREFIX+"Type", g.getGateType().name().toLowerCase());
        if (g.hasIrisUpgrade) {
            sendMessage(s,PREFIX+"IrisState", g.irisStateDescription());
        } else {
            sendMessage(s,PREFIX+"Iris");
        }
        sendMessage(s,PREFIX+"NumChevron", g.getNumChevrons());
        if (g.hasChevronUpgrade) sendMessage(s,PREFIX+"ChevronUpgrade");
        if (g.hasIrisUpgrade) sendMessage(s,PREFIX+"IrisUpgrade");
        if (g.isGenerated) sendMessage(s,PREFIX+"Generated");
        if (g.isMerged) sendMessage(s,PREFIX+"Merged");
        if (g.isInitiator) sendMessage(s,PREFIX+"Initiator");
        if (g.isLinkedToController) info(s, g.getLinkedControllerTE());
        if (g.isPending) sendMessage(s, PREFIX+"Pending");
        if (g.closeFromEitherEnd) sendMessage(s, PREFIX+"CloseFromEitherEnd");
        if (g.oneWayTravel) sendMessage(s, PREFIX+"OneWayTravel");
        if (g.allowIncomingConnections) sendMessage(s, PREFIX+"AllowIncomingConnections");
        if (g.allowOutgoingConnections) sendMessage(s, PREFIX+"AllowOutgoingConnections");
        if (g.allowRedstoneInput) sendMessage(s, PREFIX+"AllowRedstoneInput");
        if (g.allowRedstoneOutput) sendMessage(s, PREFIX+"AllowRedstoneOutput");
        sendMessage(s,PREFIX+"DialedAddress", g.dialledAddress);
    }
    private void info(ICommandSender s, DHDTE d) {

    }

    public static @NonNull SGBaseTE getStargate(@NonNull ICommandSender sender) throws CommandException {
        TileEntity gate = null;
        BlockPos pos;
        pos=Minecraft.getMinecraft().objectMouseOver.getBlockPos();
        if (sender.getEntityWorld().getTileEntity(pos) instanceof SGBaseTE) {
            gate= sender.getEntityWorld().getTileEntity(pos);
        }
        if (gate==null){gate= GateUtil.locateLocalGate(sender.getEntityWorld(), sender.getPosition(), 16, true);}
        if (gate!=null) {return (SGBaseTE) gate;}
        throw new CommandException("message.sgcraft.command:GateNotFound");
    }

    private static void sendMessage(@NonNull ICommandSender sender, String key, Object ... args) {sender.sendMessage(new TextComponentTranslation(key, args));}
    private static void sendMessage(@NonNull ICommandSender sender, String key) {sender.sendMessage(new TextComponentTranslation(key));}
    private static String getHomeAddress(@NonNull SGBaseTE te) {
        try {
            return te.getHomeAddress();
        } catch (SGAddressing.AddressingError e) {
            return e.getMessage();
        }
    }
    private void openConfiguratorScreen(ICommandSender sender, SGBaseTE localGate) throws CommandException {
        try {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            ConfiguratorNetworkHandler.sendAccessListsToClient(player, localGate);
            ConfiguratorNetworkHandler.openGuiAtClient(localGate,
                    player,
                    1,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true,
                    true);
        } catch (PlayerNotFoundException e) {
            throw new CommandException(e.getMessage());
        }
    }
}
