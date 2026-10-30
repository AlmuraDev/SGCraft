//------------------------------------------------------------------------------------------------
//
//   SG Craft - General/Shared code used by the rest of the SGCraft worldgen system
//
//------------------------------------------------------------------------------------------------

package gcewing.sg.command;

import gcewing.sg.SGCraft;
import gcewing.sg.features.configurator.network.ConfiguratorNetworkHandler;
import gcewing.sg.tileentity.DHDTE;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.util.GateType;
import gcewing.sg.util.GateUtil;
import gcewing.sg.util.IrisState;
import gcewing.sg.util.SGAddressing;
import net.minecraft.client.Minecraft;
import net.minecraft.command.*;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import org.checkerframework.checker.nullness.qual.NonNull;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class StargateCommands extends CommandBase {
    private static final String PREFIX = "message.sgcraft.command.gate.info:";
    private static final Map<String, Field> CONFIG_MAP;
    static {
        final Field[] fields = SGBaseTE.class.getFields();
        CONFIG_MAP = new HashMap<>(fields.length);
        for (Field f: fields) {
            if ((f.getModifiers() & (Modifier.STATIC | Modifier.FINAL))==0) {
                CONFIG_MAP.put(f.getName(), f);
            }
        }
    }

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
        subcommands.add("address");
        subcommands.add("reset");
        subcommands.add("config");
        subcommands.add("state");
        subcommands.add("dial");
        subcommands.add("disconnect");
        subcommands.add("type");

        if (SGCraft.isModLoaded("malisiscore")) {
            subcommands.add("configurator");
        }
    }

    @Override
    @NonNull
    public List<String> getTabCompletions(@NonNull MinecraftServer server, @NonNull ICommandSender sender, String @NonNull [] args, @Nullable BlockPos targetPos) {
        List<String> result = Collections.emptyList();
        int offset=0;
        if (args.length==0) {
            return subcommands;
        }
        try {
            //TODO @Tharow getTabCompletions to auto complete block coords when selected and available
            parseBlockPos(sender, args, 0, true);
            offset+=2;
        } catch (Exception ignored) {}

        switch (args[offset]) {
            case "type": {
                result = new ArrayList<>(GateType.values().length);
                for (GateType e: GateType.values()) {
                    result.add(e.name().toLowerCase());
                } break; }
            case "info": result = Collections.singletonList("remote"); break;
            case "config": result = configTabCompletions(targetPos, offset+1, args); break;
        }
        return result;
    }

    public void execute(@NonNull MinecraftServer server, @NonNull ICommandSender sender, String @NonNull [] args) throws CommandException {
        int offset = 0;
        BlockPos pos = null;
        try {
            pos = parseBlockPos(sender, args, offset, true);
            offset += 3;
        } catch (NumberInvalidException ignored) {}
        SGBaseTE te = getStargate(sender, pos);
        if (args.length==offset) {sendMessage(sender,"message.sgcraft.command.gate:Usage", String.join(", ", subcommands)); return;}
        switch (args[offset].toLowerCase()) {
            case "configurator": if (SGCraft.isModLoaded("malisiscore")) { openConfiguratorScreen(sender, te); }
                           else { throw new CommandNotFoundException("Malisiscore not loaded, unable to open menu"); }
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
                if (args.length!=offset+2) throw new CommandException("message.sgcraft:malformedAddress");
                String destination = SGAddressing.normalizeAddress(args[offset+1]);
                try {SGAddressing.validateAddress(destination); } catch (SGAddressing.AddressingError e) {
                    throw new CommandException(e.getMessage()); }
                try { te.connect(destination, getCommandSenderAsPlayer(sender), false, false); }
                catch (PlayerNotFoundException ex) { te.connect(destination, null, false, false); }
            } break;
            case "type": if (args.length==offset+1) {
                    sendMessage(sender, "message.sgcraft.command.gate.info:GateType", te.getGateType().name());
                } else try {te.setGateType(GateType.valueOf(args[1].toUpperCase()));} catch (IllegalArgumentException e) {
                        throw new CommandException("message.sgcraft.command.gate:InvalidGateType", args[offset+1]);} break;
            case "info": {
                if (args.length==offset+2) {
                    info(sender, te.getConnectedStargateTE());
                } else info(sender, te); break;
            }
            case "config": config(sender, te, offset+1, args); break;
            case "access": access(sender, te, offset+1, args); break;
            default: sendMessage(sender,"message.sgcraft.command.gate:Usage", String.join(", ", subcommands));
        }
    }

    private void access(@NonNull ICommandSender s, SGBaseTE g, int o, String[] args) {

    }

    private @NonNull List<String> configTabCompletions(BlockPos pos, int offset, String @NonNull ... args) {
        if (args.length==offset) return Collections.emptyList();
        if (args.length==offset+1) return getListOfStringsMatchingLastWord(args, CONFIG_MAP.keySet());
        final Field field = CONFIG_MAP.get(args[offset]);
        switch (field.getType().getSimpleName()) {
            case "boolean": return Arrays.asList("true", "false");
            case "double": return Collections.singletonList("0.0");
            case "long":
            case "int": return Collections.singletonList("0");
            case "BlockPos": return getTabCompletionCoordinate(args, offset+1, pos);
            case "IrisState": return Arrays.asList("Open", "Closing", "Closed", "Opening"); //IrisState names
            default: return Collections.emptyList();
        }
    }

    private void config(@NonNull ICommandSender s, @NonNull SGBaseTE g, int o, String @NonNull ... args) throws CommandException {
        final int l = args.length;
        if (l==o) { sendMessage(s, "message.sgcraft.command.config:Usage"); return; }
        if (Objects.equals(args[o], "all")) {
            for (Field field: CONFIG_MAP.values()) { cSendValue(s, field, g); }
            return;
        }
        final Field f = CONFIG_MAP.get(args[o]);
        if (f==null) { sendMessage(s, "message.sgcraft.command.config:NoField"); return; }
        if (l==o+1) {cSendValue(s, f, g); return;}
        try {
            final String value = args[o+1];
            switch (f.getType().getSimpleName()) {
                case "boolean": f.setBoolean(g, parseBoolean(value)); break;
                case "int": f.setInt(g, parseInt(value)); break;
                case "double": f.setDouble(g, parseDouble(value)); break;
                case "long": f.setLong(g, parseLong(value)); break;
                case "IrisState": f.set(g, IrisState.valueOf(value)); break;
                case "String": f.set(g, removeQuotes(value)); break;
                case "BlockPos": f.set(g, parseBlockPos(s, args, o+1, true)); break;
                default: throw new CommandException("message.sgcraft.command.config:UnsportedFieldType", f.getName(), f.getType().getCanonicalName());
            }
            g.markChanged();
            cSendValue(s, f, g);
        } catch (IllegalAccessException e) { throw  new CommandException("message.sgcraft.command.config:IllegalAccessException", e.getMessage()); }
    }

    private void cSendValue(@NonNull ICommandSender sender, @NonNull Field field, @NonNull SGBaseTE te) throws CommandException {
        try {sendMessage(sender, "message.sgcraft.command.config:GetValue", field.getName(), field.getType().getSimpleName(), String.valueOf(field.get(te)));}
        catch (IllegalAccessException e) {throw new CommandException("message.sgcraft.command.config:IllegalAccessException", e.getMessage());}
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

    public static @NonNull SGBaseTE getStargate(@NonNull ICommandSender sender, BlockPos pos) throws CommandException {
        SGBaseTE gate = null;
        if (pos!=null && sender.getEntityWorld().getTileEntity(pos) instanceof SGBaseTE) {
            gate = (SGBaseTE) sender.getEntityWorld().getTileEntity(pos);
        }
        BlockPos looking = Minecraft.getMinecraft().objectMouseOver.getBlockPos();
        if (sender.getEntityWorld().getTileEntity(looking) instanceof SGBaseTE) {
            gate = (SGBaseTE) sender.getEntityWorld().getTileEntity(looking);
        }
        if (gate==null){gate = (SGBaseTE) GateUtil.locateLocalGate(sender.getEntityWorld(), sender.getPosition(), 16, false);}
        if (gate!=null) {return gate;}
        throw new CommandException("message.sgcraft.command:GateNotFound", String.valueOf(pos));
    }

    private static void sendMessage(@NonNull ICommandSender sender, String key, Object ... args) {sender.sendMessage(new TextComponentTranslation(key, args));}
    private static void sendMessage(@NonNull ICommandSender sender, String key) {sender.sendMessage(new TextComponentTranslation(key));}
    private static String getHomeAddress(@NonNull SGBaseTE te) {
        try { return te.getHomeAddress(); }
        catch (SGAddressing.AddressingError e) { return e.getMessage(); }
    }
    private void openConfiguratorScreen(ICommandSender sender, SGBaseTE localGate) throws CommandException {
        try {
            EntityPlayerMP player = getCommandSenderAsPlayer(sender);
            ConfiguratorNetworkHandler.sendAccessListsToClient(player, localGate);
            ConfiguratorNetworkHandler.openGuiAtClient(localGate, player, 1, true,
                    true, true, true, true,
                    true, true, true,
                    true, true, true, true,
                    true, true, true,
                    true, true, true,
                    true, true, true, true,
                    true, true, true, true,
                    true, true, true, true);
        } catch (PlayerNotFoundException e) { throw new CommandException(e.getMessage()); }
    }

    private @NonNull String removeQuotes(@NonNull String str) {
        final int l = str.length();
        if (l==1) {return str;} // Makes sure the a single " won't get detected.
        if (str.indexOf('\"')==0 && str.lastIndexOf('\"')==l) {
            return str.substring(l-1).substring(0,1);
        }
        return str;
    }
}
