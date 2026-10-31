//------------------------------------------------------------------------------------------------
//
//   SG Craft - General/Shared code used by the rest of the SGCraft worldgen system
//
//------------------------------------------------------------------------------------------------

package gcewing.sg.command;

import gcewing.sg.SGCraft;
import gcewing.sg.features.configurator.network.ConfiguratorNetworkHandler;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.tileentity.data.GateAccessData;
import gcewing.sg.tileentity.data.PlayerAccessData;
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
    private static final String PREFIX = "message.sgcraft.command.gate";
    private static final Map<String, Field> CONFIG_MAP;
    private static final Map<String, Field> STATIC_CONFIG_MAP;
    private static final List<String> BOOLEAN_TAB = Arrays.asList("true", "false");
    static {
        final Field[] fields = SGBaseTE.class.getFields();
        CONFIG_MAP = new HashMap<>();
        STATIC_CONFIG_MAP = new HashMap<>();
        for (Field f: fields) {
            if (!Modifier.isFinal(f.getModifiers())) {
                if (Modifier.isStatic(f.getModifiers())) {
                    CONFIG_MAP.put(f.getName(), f);
                } else {
                    STATIC_CONFIG_MAP.put(f.getName(), f);
                }
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
        subcommands.add("default");
        subcommands.add("state");
        subcommands.add("dial");
        subcommands.add("disconnect");
        subcommands.add("type");
        subcommands.add("access");
        if (SGCraft.isModLoaded("malisiscore")) {
            subcommands.add("configurator");
        }
    }

    @Override
    @NonNull
    public List<String> getTabCompletions(@NonNull MinecraftServer server, @NonNull ICommandSender sender, String @NonNull [] args, @Nullable BlockPos targetPos) {
        List<String> result = Collections.emptyList();
        int offset=0;
        try {
            //TODO @Tharow getTabCompletions to auto complete block coords when selected and available
            parseBlockPos(sender, args, 0, true);
            offset+=3;
        } catch (ArrayIndexOutOfBoundsException | NumberInvalidException ignored) {}
        if (args.length==offset) {
            return subcommands;
        }
        switch (args[offset]) {
            case "type": {
                result = new ArrayList<>(GateType.values().length);
                for (GateType e: GateType.values()) {
                    result.add(e.name().toLowerCase());
                } break; }
//            case "info": result = Collections.singletonList("remote"); break; TODO @Tharow add /stargate info command.
            case "config": result = configTabCompletions(sender, targetPos, CONFIG_MAP, offset+1, args); break;
            case "default": result = configTabCompletions(sender, targetPos, STATIC_CONFIG_MAP, offset+1, args); break;
            case "access": result = accessTabCompletions(server, sender, targetPos, offset+1, args); break;
        }
        return getListOfStringsMatchingLastWord(args, result);
    }

    public void execute(@NonNull MinecraftServer server, @NonNull ICommandSender sender, String @NonNull [] args) throws CommandException {
        int offset = 0;
        BlockPos pos = null;
        try {
            pos = parseBlockPos(sender, args, offset, true);
            offset += 3;
        } catch (ArrayIndexOutOfBoundsException | NumberInvalidException ignored) {}
        SGBaseTE te = getStargate(sender, pos);
        if (args.length==offset) {sendMessage(sender,":Usage", true, String.join(", ", subcommands)); return;}
        switch (args[offset].toLowerCase()) {
            case "configurator": if (SGCraft.isModLoaded("malisiscore")) { openConfiguratorScreen(sender, te); }
                           else { throw new CommandException("fml.messages.mod.missing.dependencies.requires", "malisiscore"); }
            case "address":
            case "add": sendMessage(sender, PREFIX+":HomeAddress", true, getHomeAddress(te)); break;
            case "dis":
            case "disconnect": sendMessage(sender, "sgcraft.gui.pdd.label.disconnecting", false); te.disconnect(); break;
            case "reset": {
                te.clearConnection();
                te.clearIdleConnection();
                te.resetStargate();
                sendMessage(sender, ":Reset", true);
            } break;
            case "state": sendMessage(sender, ":State", true, te.sgStateDescription()); break;
            case "connect":
            case "dial": {
                if (args.length!=offset+2) throw new CommandException("message.sgcraft:malformedAddress");
                String destination = SGAddressing.normalizeAddress(args[offset+1]);
                try {SGAddressing.validateAddress(destination); } catch (SGAddressing.AddressingError e) {
                    throw new CommandException(e.getMessage()); }
                try { te.connect(destination, getCommandSenderAsPlayer(sender), false, false); }
                catch (PlayerNotFoundException ex) { te.connect(destination, null, false, false); }
                sendMessage(sender, "sgcraft.gui.pdd.label.connecting", false);
            } break;
            case "type": if (args.length==offset+1) {
                    sendMessage(sender, ":GateType", true, te.getGateType().name());
                } else try {te.setGateType(GateType.valueOf(args[1].toUpperCase()));} catch (IllegalArgumentException e) {
                        throw new CommandException(PREFIX+":InvalidGateType", args[offset+1]);} break;
            case "config": config(sender, te, CONFIG_MAP, offset+1, args); break;
            case "default": config(sender, te, STATIC_CONFIG_MAP, offset+1, args); break;
            case "access": access(server, sender, te, offset+1, args); break;
            default: sendMessage(sender,":Usage", true, String.join(", ", subcommands));
        }
    }
    private List<String> accessTabCompletions(@NonNull MinecraftServer server, @NonNull ICommandSender sender, BlockPos pos, int offset, @NonNull String @NonNull ... args) {
        final int l = args.length;
        if (l==offset) return Arrays.asList("add", "remove", "", "defaults");
        switch (args[offset]) {
            case "add":
            case "remove": {
                if (l==offset+1) return Arrays.asList("player", "address");
                if (args[offset+1].equalsIgnoreCase("player")) {
                    return Arrays.asList(server.getOnlinePlayerNames());
                }
                if ((offset+2)<l||l<(offset+4)) {
                    return BOOLEAN_TAB;
                }
            }
            case "defaults": sendMessage(sender, ".access.defaults:Usage", true); return BOOLEAN_TAB;
            default: return Collections.emptyList();
        }
    }

    //Stargate access allow player {USERNAME} true true true
    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return (index==3&&args.length==4&& Objects.equals(args[0], "access"));
    }

    private void access(@NonNull MinecraftServer server, @NonNull ICommandSender s, @NonNull SGBaseTE g, int o, String @NonNull [] args) throws CommandException {
        final int l = args.length;
        if (l<=o+1) {sendMessage(s, ".access:Usage", true);}
        final List<GateAccessData> gate = new ArrayList<>(g.getGateAccessData());
        final List<PlayerAccessData> players = new ArrayList<>(g.getPlayerAccessData());
        boolean isPlayer = Objects.equals(args[o + 1], "player");
        if (!(isPlayer || Objects.equals(args[o + 1], "address"))) {
            sendMessage(s, ".access:Usage", true); return;
        }
        boolean isDefault = args[o].equalsIgnoreCase("defaults");
        final EntityPlayerMP player;
        EntityPlayerMP player1 = null;
        final String address = !isPlayer?SGAddressing.normalizeAddress(args[o + 2]):null;
        try {
            player1 = isPlayer?getPlayer(server, s, args[o + 2]):null;
            if (!isPlayer) SGAddressing.validateAddress(address);
        } catch (ArrayIndexOutOfBoundsException | SGAddressing.AddressingError e) {
            if (!isDefault) {
                throw new SyntaxErrorException();
            }
        } finally {
            player = player1;
        }
        try {

            switch (args[o].toLowerCase()) {
                case "add": if (isPlayer) players.add(new PlayerAccessData(player, parseBoolean(args[o+3]), parseBoolean(args[o+4]), parseBoolean(args[o+5])));
                            else gate.add(new GateAccessData(address, parseBoolean(args[o+3]), parseBoolean(args[o+4])));
                            break;
                case "remove": if (isPlayer) players.removeIf(i -> i.isFor(player));
                               else gate.removeIf(i -> i.matches(address));
                               break;
                case "defaults": {
                    g.defaultAllowIncoming = parseBoolean(args[o + 1]);
                    g.defaultAllowOutgoing = parseBoolean(args[o + 2]);
                    g.defaultAllowGateAccess = parseBoolean(args[o + 3]);
                    g.defaultAllowIrisAccess = parseBoolean(args[o + 4]);
                    g.defaultAllowAdminAccess = parseBoolean(args[o + 5]);
                }
            }
        } catch (ArrayIndexOutOfBoundsException ignored) {
            if (isDefault) {
                throw new WrongUsageException(PREFIX+".access.default:Usage");
            }
            throw new WrongUsageException(PREFIX+".access."+args[o]+(isPlayer?"player:Usage":"gate:Usage"));
        }
        g.setClientAccessLists(gate, players);
    }

    private @NonNull List<String> configTabCompletions(@NonNull ICommandSender sender, BlockPos pos, Map<String, Field> settings, int offset, String @NonNull ... args) {
        if (args.length==offset) return Collections.emptyList();
        if (args.length==offset+1) return Arrays.asList(settings.keySet().toArray(new String[]{}));

        final Field field = settings.get(args[offset]);
        sendMessage(sender, ".config:CompletionType", true, field.getType().getSimpleName());
        switch (field.getType().getSimpleName()) {
            case "boolean": return BOOLEAN_TAB;
            case "double": return Collections.singletonList("0.0");
            case "long":
            case "int": return Collections.singletonList("0");
            case "BlockPos": return getTabCompletionCoordinate(args, offset+1, pos);
            case "IrisState": return Arrays.asList("Open", "Closing", "Closed", "Opening"); //IrisState names
            default: return Collections.emptyList();
        }
    }

    private void config(@NonNull ICommandSender s, @NonNull SGBaseTE gate, Map<String, Field> settings, int offset, String @NonNull ... args) throws CommandException {
        final int l = args.length;
        if (l==offset) { sendMessage(s, ".config:Usage", true); return; }
        if (Objects.equals(args[offset], "all")) {
            for (Field field: settings.values()) { cSendValue(s, field, gate); }
            return;
        }
        final Field f = settings.get(args[offset]);
        if (f==null) { sendMessage(s, ".config:NoField", true, (Object[]) null); return; }
        if (l==offset+1) {cSendValue(s, f, gate); return;}
        try {
            final String value = args[offset+1];
            switch (f.getType().getSimpleName()) {
                case "boolean": f.setBoolean(gate, parseBoolean(value)); break;
                case "int": f.setInt(gate, parseInt(value)); break;
                case "double": f.setDouble(gate, parseDouble(value)); break;
                case "long": f.setLong(gate, parseLong(value)); break;
                case "IrisState": f.set(gate, IrisState.valueOf(value)); break;
                case "String": f.set(gate, removeQuotes(value)); break;
                case "BlockPos": f.set(gate, parseBlockPos(s, args, offset+1, true)); break;
                default: throw new CommandException(PREFIX+".config:UnsportedFieldType", f.getName(), f.getType().getCanonicalName());
            }
            gate.markChanged();
            cSendValue(s, f, gate);
        } catch (IllegalAccessException e) { throw  new CommandException(PREFIX+".config:IllegalAccessException", e.getMessage()); }
    }

    private void cSendValue(@NonNull ICommandSender sender, @NonNull Field field, @NonNull SGBaseTE te) throws CommandException {
        try {sendMessage(sender, ".config:GetValue", true, field.getName(), field.getType().getSimpleName(), String.valueOf(field.get(te)));}
        catch (IllegalAccessException e) {throw new CommandException(PREFIX+".config:IllegalAccessException", e.getMessage());}
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
        throw new CommandException(PREFIX+":GateNotFound", String.valueOf(pos));
    }


    private static void sendMessage(@NonNull ICommandSender sender, String key, boolean usePrefix, Object... args) {
        if (args==null) sender.sendMessage(new TextComponentTranslation(usePrefix?PREFIX+key:key));
        else sender.sendMessage(new TextComponentTranslation(usePrefix?PREFIX+key:key, args));

    }
    private static String getHomeAddress(@NonNull SGBaseTE te) {
        try { return te.getHomeAddress(); }
        catch (SGAddressing.AddressingError e) { return e.getMessage(); }
    }
    private void openConfiguratorScreen(ICommandSender sender, SGBaseTE localGate) throws CommandException {
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
