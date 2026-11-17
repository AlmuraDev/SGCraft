package gcewing.sg.features.pdd.network;

import static gcewing.sg.tileentity.SGBaseTE.sendErrorMsg;

import gcewing.sg.BaseDataChannel;
import gcewing.sg.SGCraft;
import gcewing.sg.features.edd.ExplorerData;
import gcewing.sg.features.pdd.AddressData;
import gcewing.sg.features.pdd.PddItem;
import gcewing.sg.features.pdd.client.gui.PddEntryScreen;
import gcewing.sg.features.pdd.client.gui.PddScreen;
import gcewing.sg.generator.GeneratorAddressRegistry;
import gcewing.sg.network.SGChannel;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.util.SGAddressing;
import gcewing.sg.util.SGDimensionMap;
import gcewing.sg.util.SGState;
import gcewing.sg.util.WorldNames;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PddNetworkHandler extends SGChannel {

    protected static BaseDataChannel pddChannel;

    public PddNetworkHandler(String name) {
        super(name);
        pddChannel = this;
    }

    public static void addPddEntryFromServer(EntityPlayer player, String address, int nextIndex) {
        ChannelOutput data = pddChannel.openPlayer(player,"AddPddEntry");
        data.writeUTF(address);
        data.writeInt(nextIndex);
        data.close();
    }

    @ClientMessageHandler("EditPddEntry")
    public void handlePddEditAddressRequest(EntityPlayer player, ChannelInput data) {
        String address = data.readUTF();
        String name = data.readUTF();
        int currentIndex = data.readInt();
        int uuid = data.readInt();
        boolean autoClose = data.readBoolean();
        address = SGAddressing.formatAddress(address, "-", "-");
        new PddEntryScreen(null, player, name, address, currentIndex, uuid, false, autoClose,false, 2).display();
    }

    public static void editPddEntryFromServer(EntityPlayer player, String address, String name, int currentIndex, int uuid, boolean autoClose) {
        ChannelOutput data = pddChannel.openPlayer(player,"EditPddEntry");
        data.writeUTF(address);
        data.writeUTF(name);
        data.writeInt(currentIndex);
        data.writeInt(uuid);
        data.writeBoolean(autoClose);
        data.close();
    }

    @ClientMessageHandler("AddPddEntry")
    public void handlePddAddAddressRequest(EntityPlayer player, ChannelInput data) {
        String address = data.readUTF();
        int nextIndex = data.readInt();
        address = SGAddressing.formatAddress(address, "-", "-");
        new PddEntryScreen(null, player, "Name Here", address, nextIndex, 0, false, false,false, 1).display();
    }

    public static void updatePdd(EntityPlayer player, boolean value, int status) {
        ChannelOutput data = pddChannel.openPlayer(player,"UpdatePdd");
        data.writeInt(status);
        data.writeBoolean(value);
        data.close();
    }

    @ClientMessageHandler("UpdatePdd")
    public void handleUpdatePddListRequest(EntityPlayer player, ChannelInput data) {
        int status = data.readInt();
        boolean update = data.readBoolean();
        if (Minecraft.getMinecraft().currentScreen instanceof PddScreen) {
            PddScreen screen = (PddScreen) Minecraft.getMinecraft().currentScreen;
            if (status == 2)
                screen.stopDialing();
        }
    }

    public static void sendPddInputToServer(SGBaseTE te, int function, String origin, String destination) {
        ChannelOutput data = pddChannel.openServer("PddInput");
        writeCoords(data, te);
        data.writeInt(function);
        data.writeUTF(origin);
        data.writeUTF(destination);
        data.close();
    }

    @ServerMessageHandler("PddInput") public void handlePddInputFromClient(EntityPlayer player, ChannelInput data) throws SGAddressing.AddressingError {
        BlockPos pos = readCoords(data);
        int setting = data.readInt();
        String origin = data.readUTF();
        String destination = data.readUTF();

        boolean isPermissionsAdmin = SGCraft.hasPermissionSystem() && SGCraft.hasPermission(player, "sgcraft.admin"); // Fallback for a full permissions system override to the Access System
        if (!mayUseDialler(player)) {
            System.err.println("SGCraft - Hacked Client detected!");
            return;
        }
        SGBaseTE localGate = loadedGateAt(player, pos);
        if (localGate == null || !playerAtGate(player, localGate)) {
            return; // the PDD works the gate its player is at
        }

        if (!origin.isEmpty() && destination.length() >= 7) {
            if (SGAddressing.inSameDimension(origin, destination)) {
                destination = destination.substring(0, 7);
            }
        }

        if (setting == 1) { // Connect / Dial / Double Click
            if (!localGate.isConnected()) {
                if (localGate.allowGateAccess(player) || isPermissionsAdmin) {
                    localGate.connect(destination, player, false, false);
                }
            }
        }
        if (setting == 5) { // Dial a gate whose chevrons lock as they're entered: the gate enters each symbol in turn
            if (!localGate.isConnected() && localGate.chevronsLockOnDial) {
                if (localGate.allowGateAccess(player) || isPermissionsAdmin) {
                    localGate.startLockDial(destination, player);
                }
            }
        }
        if (setting == 2) {
            if (localGate.isConnected()) {
                if (localGate.allowGateAccess(player) || isPermissionsAdmin) {
                    localGate.disconnect(player);
                }
            }
        }

        if (setting == 3) {
            // Reserved for testing purposes.
        }

        if (setting == 4) {
            if (localGate.allowGateAccess(player) || isPermissionsAdmin) {
                if (localGate.state == SGState.Idle) {
                    localGate.isInitiator = true;
                }
                localGate.connectOrDisconnect("", player);
                localGate.clearIdleConnection();
                localGate.errorState = false; // Force this on the servers' TE.
            }
        }
    }

    // Whether the player may dial with a PDD or EDD
    private static boolean mayUseDialler(EntityPlayer player) {
        return SGCraft.hasPermission(player, "sgcraft.gui.pdd") || SGCraft.hasPermission(player, "sgcraft.gui.edd")
            || (SGCraft.hasPermissionSystem() && SGCraft.hasPermission(player, "sgcraft.admin"));
    }

    public static void sendPddEntryUpdateToServer(String name, String address, int index, int unid, boolean locked, boolean autoClose) {
        ChannelOutput data = pddChannel.openServer("PddInputEntry");
        data.writeUTF(name);
        data.writeUTF(address);
        data.writeInt(index);
        data.writeInt(unid);
        data.writeBoolean(locked);
        data.writeBoolean(autoClose);
        data.close();
    }

    @ServerMessageHandler("PddInputEntry")
    public void handlePddEntryUpdateFromClient(EntityPlayer player, ChannelInput data) {
        String name = data.readUTF();
        String address = data.readUTF();
        int index = data.readInt();
        int unid = data.readInt();
        boolean locked = data.readBoolean();
        boolean autoClose = data.readBoolean();

        if (!SGCraft.hasPermission(player, "sgcraft.gui.pdd.edit")) {
            System.err.println("SGCraft - Hacked Client detected!");
            return;
        }

        // Only a PDD's own list, within limits: a client could otherwise grow any item's data without end, until every
        // player sent the item (dropped, or in an item frame) was disconnected for a packet too big to read
        final ItemStack stack = player.getHeldItemMainhand();
        NBTTagCompound compound = stack.getItem() instanceof PddItem ? stack.getTagCompound() : null;
        if (compound == null) {
            return;
        }
        name = name.trim();
        if (name.length() > maxEntryNameLength) {
            name = name.substring(0, maxEntryNameLength);
        }
        if (index != -1) { // -1 deletes the entry
            String normalized = address.toUpperCase().replaceAll("-", "");
            try {
                SGAddressing.validateAddress(normalized);
            } catch (SGAddressing.AddressingError e) {
                sendErrorMsg(player, "malformedAddress");
                return;
            }
            if (normalized.length() != SGAddressing.maxAddressLength) {
                sendErrorMsg(player, "malformedAddress");
                return;
            }
            address = SGAddressing.formatAddress(normalized, "-", "-");
        }
        if (unid == 0 && AddressData.getAddresses(compound).size() >= maxEntries) {
            sendErrorMsg(player, "pddListFull", maxEntries);
            return;
        }
        // Only the server's own entries are locked (see AddressData.updateAddress); a player's are never
        AddressData.updateAddress(player, compound, unid, name, address, index, false, autoClose);
        stack.setTagCompound(compound);
        player.inventoryContainer.detectAndSendChanges();
        PddNetworkHandler.updatePdd(player, false, 1);
    }

    // How long an entry's name may be, and how many entries a PDD holds
    private static final int maxEntryNameLength = 64;
    private static final int maxEntries = 200;

    // Moves an address entry up (direction -1) or down (+1) the list on the PDD in the player's main hand
    public static void sendPddEntryMoveToServer(int unid, int direction) {
        ChannelOutput data = pddChannel.openServer("PddMoveEntry");
        data.writeInt(unid);
        data.writeInt(direction);
        data.close();
    }

    @ServerMessageHandler("PddMoveEntry")
    public void handlePddEntryMoveFromClient(EntityPlayer player, ChannelInput data) {
        int unid = data.readInt();
        int direction = data.readInt();

        if (!SGCraft.hasPermission(player, "sgcraft.gui.pdd.edit")) {
            System.err.println("SGCraft - Hacked Client detected!");
            return;
        }

        final ItemStack stack = player.getHeldItemMainhand();
        if (stack.getItem() instanceof PddItem && stack.getTagCompound() != null) {
            NBTTagCompound compound = stack.getTagCompound();
            if (AddressData.moveAddress(compound, unid, direction)) {
                stack.setTagCompound(compound);
                player.inventoryContainer.detectAndSendChanges();
            }
        }
    }

    // Asks the server which world each address leads to; the client can't tell, as the address only carries the
    // server's dimension index
    public static void requestWorldNamesFromServer(Collection<String> addresses) {
        ChannelOutput data = pddChannel.openServer("PddWorldNames");
        data.writeInt(addresses.size());
        for (String address : addresses) {
            data.writeUTF(address);
        }
        data.close();
    }

    @ServerMessageHandler("PddWorldNames")
    public void handleWorldNamesRequestFromClient(EntityPlayer player, ChannelInput data) {
        int count = Math.min(data.readInt(), 512);
        Map<String, String> names = new HashMap<>();
        for (int i = 0; i < count; i++) {
            String address = data.readUTF();
            String name = worldNameFor(player, address);
            if (name != null) {
                names.put(address, name);
            }
        }

        ChannelOutput reply = pddChannel.openPlayer(player, "PddWorldNamesReply");
        reply.writeInt(names.size());
        for (Map.Entry<String, String> entry : names.entrySet()) {
            reply.writeUTF(entry.getKey());
            reply.writeUTF(entry.getValue());
        }
        reply.close();
    }

    @ClientMessageHandler("PddWorldNamesReply")
    public void handleWorldNamesFromServer(EntityPlayer player, ChannelInput data) {
        int count = data.readInt();
        Map<String, String> names = new HashMap<>();
        for (int i = 0; i < count; i++) {
            names.put(data.readUTF(), data.readUTF());
        }
        if (Minecraft.getMinecraft().currentScreen instanceof PddScreen) {
            ((PddScreen) Minecraft.getMinecraft().currentScreen).setWorldNames(names);
        }
    }

    // The name of the world an address leads to, or null if it's not a valid address or that world doesn't exist.
    // A 7-symbol address has no dimension part, so leads to a gate in the dimension it's dialled from: the player's.
    public static String worldNameFor(EntityPlayer player, String address) {
        String normalized = address.toUpperCase().replaceAll("-", "");
        try {
            SGAddressing.validateAddress(normalized);
        } catch (SGAddressing.AddressingError e) {
            return null;
        }
        Integer dimension = player.dimension;
        if (normalized.length() == SGAddressing.maxAddressLength) {
            dimension = SGDimensionMap.dimensionForIndex(SGAddressing.dimensionSymbolsOf(normalized));
        }
        if (dimension == null || !DimensionManager.isDimensionRegistered(dimension)) {
            return null;
        }
        return WorldNames.nameOf(dimension); // known even while that world isn't loaded
    }

    // ---- Explorer Dialer Device: the server's generated gates ----

    // Asks for the generated gates, with this player's visits and names; the server replies only if they changed
    // since the revision the client already has
    // (null when the client has none yet, so needs it whatever the revision)
    public static void requestExplorerListFromServer(Integer knownRevision) {
        ChannelOutput data = pddChannel.openServer("EddListRequest");
        data.writeBoolean(knownRevision != null);
        data.writeInt(knownRevision == null ? 0 : knownRevision);
        data.close();
    }

    @ServerMessageHandler("EddListRequest")
    public void handleExplorerListRequestFromClient(EntityPlayer player, ChannelInput data) {
        boolean hasRevision = data.readBoolean();
        int knownRevision = data.readInt();
        if (!SGCraft.hasPermission(player, "sgcraft.gui.edd")) {
            return;
        }
        if (!hasRevision || knownRevision != ExplorerData.revisionFor(player)) {
            // At most once a second: a client can ask over and over, and the server built and sent the whole list each time
            long now = System.currentTimeMillis();
            Long last = lastListSent.get(player.getUniqueID());
            if (last != null && now - last >= 0 && now - last < 1000) {
                return;
            }
            lastListSent.put(player.getUniqueID(), now);
            sendExplorerList(player);
        }
    }

    // When each player was last sent the list of generated gates on asking for it
    private static final Map<UUID, Long> lastListSent = new HashMap<>();

    // Every generated gate, ordered by world and then address
    private static void sendExplorerList(EntityPlayer player) {
        List<String[]> entries = new ArrayList<>(); // address, name, world, visited
        for (String address : GeneratorAddressRegistry.getAllAddresses()) {
            try {
                SGAddressing.validateAddress(address);
            } catch (SGAddressing.AddressingError e) {
                continue;
            }
            if (address.length() != SGAddressing.maxAddressLength) {
                continue;
            }
            String world = worldNameFor(player, address);
            entries.add(new String[] {SGAddressing.formatAddress(address, "-", "-"), ExplorerData.getName(player, address),
                world == null ? "" : world, ExplorerData.isVisited(player, address) ? "1" : ""});
        }
        entries.sort(Comparator.<String[], String>comparing(e -> e[2].toLowerCase()).thenComparing(e -> e[0]));

        ChannelOutput reply = pddChannel.openPlayer(player, "EddList");
        reply.writeInt(ExplorerData.revisionFor(player));
        reply.writeInt(entries.size());
        for (String[] entry : entries) {
            reply.writeUTF(entry[0]);
            reply.writeUTF(entry[1]);
            reply.writeUTF(entry[2]);
            reply.writeBoolean(!entry[3].isEmpty());
        }
        reply.close();
    }

    @ClientMessageHandler("EddList")
    public void handleExplorerListFromServer(EntityPlayer player, ChannelInput data) {
        int revision = data.readInt();
        int count = data.readInt();
        List<AddressData> addresses = new ArrayList<>();
        Map<String, String> worlds = new HashMap<>();
        Set<String> visited = new HashSet<>();
        for (int i = 0; i < count; i++) {
            String address = data.readUTF();
            String name = data.readUTF();
            String world = data.readUTF();
            boolean wasVisited = data.readBoolean();
            // Kept in the order the server sent; the unid only has to tell the entries apart
            addresses.add(new AddressData(name, address, false, i, i + 1, false));
            if (!world.isEmpty()) {
                worlds.put(address, world);
            }
            if (wasVisited) {
                visited.add(address);
            }
        }
        if (Minecraft.getMinecraft().currentScreen instanceof PddScreen) {
            ((PddScreen) Minecraft.getMinecraft().currentScreen).setExplorerList(revision, addresses, worlds, visited);
        }
    }

    // Names a generated gate's address for this player; a blank name clears it
    public static void sendExplorerNameToServer(String address, String name) {
        ChannelOutput data = pddChannel.openServer("EddName");
        data.writeUTF(address);
        data.writeUTF(name);
        data.close();
    }

    @ServerMessageHandler("EddName")
    public void handleExplorerNameFromClient(EntityPlayer player, ChannelInput data) {
        String address = data.readUTF();
        String name = data.readUTF();
        if (!SGCraft.hasPermission(player, "sgcraft.gui.edd") || !GeneratorAddressRegistry.isGenerated(address)) {
            return;
        }
        ExplorerData.setName(player, address, name);
        sendExplorerList(player);
    }

    public static void sendEnterSymbolToServer(SGBaseTE te, String address, int digit) {
        ChannelOutput data = pddChannel.openServer("EnterImmediateSymbol");
        writeCoords(data, te);
        data.writeUTF(address);
        data.writeInt(digit);
        data.close();
    }


    // The PDD dialling a gate that locks each chevron as it's entered, one symbol at a time. It used to take any
    // player's word for any gate, in any state: one sent to a gate between two symbols of another dial crashed the
    // server (see SGBaseTE.immediateDialSymbol)
    @ServerMessageHandler("EnterImmediateSymbol")
    public void handleEnterSymbolFromClient(EntityPlayer player, ChannelInput data) {
        BlockPos pos = readCoords(data);
        String address = data.readUTF();
        int digit = data.readInt();

        if (!mayUseDialler(player)) {
            return;
        }
        SGBaseTE localGate = loadedGateAt(player, pos);
        if (localGate == null || !playerAtGate(player, localGate) || !localGate.allowGateAccess(player)) {
            return;
        }
        localGate.immediateDialSymbol(address, player, digit);
    }
}
