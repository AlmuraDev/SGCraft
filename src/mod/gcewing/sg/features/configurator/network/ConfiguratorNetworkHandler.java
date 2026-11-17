package gcewing.sg.features.configurator.network;

import static gcewing.sg.tileentity.SGBaseTE.sendBasicMsg;
import static gcewing.sg.tileentity.SGBaseTE.sendErrorMsg;

import gcewing.sg.BaseDataChannel;
import gcewing.sg.SGCraft;
import gcewing.sg.features.configurator.client.gui.ConfiguratorScreen;
import gcewing.sg.features.dynmap.DynmapMarkers;
import gcewing.sg.network.SGChannel;
import gcewing.sg.tileentity.SGBaseTE;
import gcewing.sg.tileentity.data.GateAccessData;
import gcewing.sg.tileentity.data.PlayerAccessData;
import gcewing.sg.util.GateUtil;
import gcewing.sg.util.GateType;
import gcewing.sg.util.PlayerNames;
import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

public class ConfiguratorNetworkHandler extends SGChannel {

    protected static BaseDataChannel configuratorChannel;

    public ConfiguratorNetworkHandler(String name) {
        super(name);
        configuratorChannel = this;
    }

    public static void sendConfiguratorInputToServer(SGBaseTE te,
            int secondsToStayOpen,
            double ringRotationSpeed,
            double maxEnergyBuffer,
            double energyPerFuelItem,
            int gateOpeningsPerFuelItem,
            double distanceFactorMultiplier,
            double interDimensionalMultiplier,
            boolean oneWayTravel,
            boolean hasIrisUpgrade,
            boolean hasChevronUpgrade,
            int gateType,
            boolean reverseWormholeKills,
            boolean closeFromEitherEnd,
            boolean preserveInventory,
            boolean requiresNoPower,
            boolean chevronsLockOnDial,
            boolean returnToPreviousIrisState,
            boolean transientDamage,
            boolean transparency,
            int orientation,
            boolean useDHDFuelSource,
            boolean allowRedstoneOutput,
            boolean allowRedstoneInput,
            boolean canPlayerBreakPlayerGate,
            boolean canPlayerBreakGeneratedGate,
            boolean displayGateAddress,
            boolean isGenerated,
            boolean hasShield,
            boolean invulnerable,
            boolean showOnDynmap,
            boolean showAddressOnDynmap) {

        ChannelOutput data = configuratorChannel.openServer("ConfiguratorInput");
        writeCoords(data, te);
        data.writeInt(secondsToStayOpen);
        data.writeDouble(ringRotationSpeed);
        data.writeDouble(maxEnergyBuffer);
        data.writeDouble(energyPerFuelItem);
        data.writeInt(gateOpeningsPerFuelItem);
        data.writeDouble(distanceFactorMultiplier);
        data.writeDouble(interDimensionalMultiplier);
        data.writeBoolean(oneWayTravel);
        data.writeBoolean(hasIrisUpgrade);
        data.writeBoolean(hasChevronUpgrade);
        data.writeInt(gateType);
        data.writeBoolean(reverseWormholeKills);
        data.writeBoolean(closeFromEitherEnd);
        data.writeBoolean(preserveInventory);
        data.writeBoolean(requiresNoPower);
        data.writeBoolean(chevronsLockOnDial);
        data.writeBoolean(returnToPreviousIrisState);
        data.writeBoolean(transientDamage);
        data.writeBoolean(transparency);
        data.writeInt(orientation);
        data.writeBoolean(useDHDFuelSource);
        data.writeBoolean(allowRedstoneOutput);
        data.writeBoolean(allowRedstoneInput);
        data.writeBoolean(canPlayerBreakPlayerGate);
        data.writeBoolean(canPlayerBreakGeneratedGate);
        data.writeBoolean(displayGateAddress);
        data.writeBoolean(isGenerated);
        data.writeBoolean(hasShield);
        data.writeBoolean(invulnerable);
        data.writeBoolean(showOnDynmap);
        data.writeBoolean(showAddressOnDynmap);
        data.close();
    }

    @ServerMessageHandler("ConfiguratorInput")
    public void handleConfiguratorInputFromClient(EntityPlayer player, ChannelInput data) {
        BlockPos pos = readCoords(data);
        SGBaseTE te = loadedGateAt(player, pos);

        int secondsToStayOpen = data.readInt();
        double ringRotationSpeed = data.readDouble();
        double maxEnergyBuffer = data.readDouble();
        double energyPerFuelItem = data.readDouble();
        int gateOpeningsPerFuelItem = data.readInt();
        double distanceFromMultiplier = data.readDouble();
        double interDimensionalMultiplier = data.readDouble();
        boolean oneWayTravel = data.readBoolean();
        boolean hasIrisUpgrade = data.readBoolean();
        boolean hasChevronUpgrade = data.readBoolean();
        int gateType = data.readInt();
        boolean reverseWormholeKills = data.readBoolean();
        boolean closeFromEitherEnd = data.readBoolean();
        boolean preserveInventory = data.readBoolean();
        boolean requiresNoPower = data.readBoolean();
        boolean chevronsLockOnDial = data.readBoolean();
        boolean returnToPreviousIrisState = data.readBoolean();
        boolean transientDamage = data.readBoolean();
        boolean transparency = data.readBoolean();
        int orientation = data.readInt();
        boolean useDHDFuelSource = data.readBoolean();
        boolean allowRedstoneOutput = data.readBoolean();
        boolean allowRedstoneInput = data.readBoolean();
        boolean canPlayerBreakPlayerGate = data.readBoolean();
        boolean canPlayerBreakGeneratedGate = data.readBoolean();
        boolean displayGateAddress = data.readBoolean();
        boolean isGenerated = data.readBoolean();
        boolean hasShield = data.readBoolean();
        boolean invulnerable = data.readBoolean();
        boolean showOnDynmap = data.readBoolean();
        boolean showAddressOnDynmap = data.readBoolean();

        boolean isPermissionsAdmin = SGCraft.hasPermissionSystem() && SGCraft.hasPermission(player, "sgcraft.admin"); // Fallback for a full permissions system override to the Access System

        // Only what the Configurator's screen allows (see ConfiguratorScreen): a client can send any number, and a ring
        // speed of 0 left both gates dialling forever, a negative distance factor made each dial add energy
        secondsToStayOpen = clamp(secondsToStayOpen, 0, 50000);
        ringRotationSpeed = clamp(ringRotationSpeed, 1, 10);
        maxEnergyBuffer = clamp(maxEnergyBuffer, 0, 5000000);
        energyPerFuelItem = clamp(energyPerFuelItem, 0, 250000);
        gateOpeningsPerFuelItem = clamp(gateOpeningsPerFuelItem, 1, 50000);
        distanceFromMultiplier = clamp(distanceFromMultiplier, 0, 1000);
        interDimensionalMultiplier = clamp(interDimensionalMultiplier, 0, 1000);

        if (canConfigure(player, te)) {
            if (SGCraft.hasPermission(player, "sgcraft.configurator.secondsToStayOpen") || isPermissionsAdmin) {
                te.secondsToStayOpen = secondsToStayOpen;
                te.ticksToStayOpen = te.secondsToStayOpen * 20;
            }
            if (SGCraft.hasPermission(player, "sgcraft.configurator.ringRotationSpeed") || isPermissionsAdmin) te.ringRotationSpeed = ringRotationSpeed;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.maxEnergyBuffer") || isPermissionsAdmin) te.maxEnergyBuffer = maxEnergyBuffer;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.energyPerFuelItem") || isPermissionsAdmin) te.energyPerFuelItem = energyPerFuelItem;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.gateOpeningsPerFuelItem") || isPermissionsAdmin) te.gateOpeningsPerFuelItem = gateOpeningsPerFuelItem;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.distanceFactorMultiplier") || isPermissionsAdmin) te.distanceFactorMultiplier = distanceFromMultiplier;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.interDimensionalMultiplier") || isPermissionsAdmin) te.interDimensionMultiplier = interDimensionalMultiplier;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.oneWayTravel") || isPermissionsAdmin) te.oneWayTravel = oneWayTravel;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.hasIrisUpgrade") || isPermissionsAdmin) {
                // Iris Upgrade is the metal iris and Shield the shield; either one fits the upgrade
                te.hasIrisUpgrade = hasIrisUpgrade || hasShield;
                te.hasShield = hasShield;
            }
            if (SGCraft.hasPermission(player, "sgcraft.configurator.hasChevronUpgrade") || isPermissionsAdmin) te.hasChevronUpgrade = hasChevronUpgrade;
            if ((SGCraft.hasPermission(player, "sgcraft.configurator.gateType") || isPermissionsAdmin)
                    && (gateType != GateType.ORI.id || te.getShape().isSupergate())) te.gateType = gateType; // only a supergate can be Ori
            if (SGCraft.hasPermission(player, "sgcraft.configurator.reverseWormholeKills") || isPermissionsAdmin) te.reverseWormholeKills = reverseWormholeKills;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.closeFromEitherEnd") || isPermissionsAdmin) te.closeFromEitherEnd = closeFromEitherEnd;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.preserveInventory") || isPermissionsAdmin) te.preserveInventory = preserveInventory;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.noPowerRequired") || isPermissionsAdmin) te.requiresNoPower = requiresNoPower;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.chevronsLockOnDial") || isPermissionsAdmin) te.chevronsLockOnDial = chevronsLockOnDial;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.returnToPreviousIrisState") || isPermissionsAdmin) te.returnToPreviousIrisState = returnToPreviousIrisState;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.transientDamage") || isPermissionsAdmin) te.transientDamage = transientDamage;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.transparency") || isPermissionsAdmin) te.transparency = transparency;
            // A gate lying flat can be turned face up or face down; one standing up stays so, as
            // its ring stands where it was built
            boolean turnable = te.isFlat() && (orientation == 2 || orientation == 3);
            if (turnable && (SGCraft.hasPermission(player, "sgcraft.configurator.orientation") || isPermissionsAdmin)) te.gateOrientation = orientation;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.useDHDFuelSource") || isPermissionsAdmin) te.useDHDFuelSource = useDHDFuelSource;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.allowRedstoneOutput") || isPermissionsAdmin) te.allowRedstoneOutput = allowRedstoneOutput;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.allowRedstoneInput") || isPermissionsAdmin) te.allowRedstoneInput = allowRedstoneInput;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.canPlayerBreakPlayerGate") || isPermissionsAdmin) te.canPlayerBreakPlayerGate = canPlayerBreakPlayerGate;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.canPlayerBreakGeneratedGate") || isPermissionsAdmin) te.canPlayerBreakGeneratedGate = canPlayerBreakGeneratedGate;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.displayGateAddress") || isPermissionsAdmin) te.displayGateAddress = displayGateAddress;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.isGenerated") || isPermissionsAdmin) te.isGenerated = isGenerated;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.invulnerable") || isPermissionsAdmin) te.invulnerable = invulnerable;
            if (SGCraft.hasPermission(player, "sgcraft.configurator.dynmap") || isPermissionsAdmin) {
                te.showOnDynmap = showOnDynmap;
                te.showAddressOnDynmap = showOnDynmap && showAddressOnDynmap;
            }
            te.enforceOriRules(); // whatever was sent, an Ori gate keeps to its own
            sendBasicMsg(player, "changesSaved");
            te.updateIrisEntity();
            te.markChanged(); // saved with the world, not just sent to players nearby
            DynmapMarkers.update(te);
        }
    }

    // Whether the player may change this gate's settings. A client can send any gate's position, so the player must
    // be at the gate holding a Configurator, as when opening its screens, and be the gate's admin (or have the
    // permissions system's sgcraft.admin).
    private static boolean canConfigure(EntityPlayer player, SGBaseTE te) {
        if (te == null) {
            return false; // gone, or not loaded
        }
        boolean holdingConfigurator = player.getHeldItemMainhand().getItem() == SGCraft.configurator
            || player.getHeldItemOffhand().getItem() == SGCraft.configurator;
        // Generous: ConfiguratorItem picks a gate up to toolSearchRange blocks away in each direction
        double range = 2 * Math.max(SGCraft.toolSearchRange, GateUtil.reach(player));
        if (!holdingConfigurator || GateUtil.distanceToGate(te, player.getPositionEyes(1)) > range) {
            return false;
        }
        boolean isPermissionsAdmin = SGCraft.hasPermissionSystem() && SGCraft.hasPermission(player, "sgcraft.admin");
        return SGCraft.hasPermission(player, "sgcraft.gui.configurator") && te.allowAdminAccess(player) || isPermissionsAdmin;
    }

    private static double clamp(double value, double min, double max) {
        return Double.isNaN(value) ? min : Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    // After an access setting changes: save it, tell everyone who can see the gate what they may now do there, and
    // send the editor the updated lists for their screen
    private static void accessChanged(EntityPlayer player, SGBaseTE te) {
        te.markChanged();
        te.sendPlayerAccessToWatchers();
        sendAccessListsToClient(player, te);
    }

    // The gate's full access lists, for an admin's Configurator screens; other players are never sent them
    public static void sendAccessListsToClient(EntityPlayer player, SGBaseTE te) {
        ChannelOutput data = configuratorChannel.openPlayer(player, "AccessLists");
        writeCoords(data, te);
        List<GateAccessData> gates = te.getGateAccessData();
        data.writeInt(gates.size());
        for (GateAccessData entry : gates) {
            data.writeUTF(entry.getAddress());
            data.writeBoolean(entry.hasIncomingAccess());
            data.writeBoolean(entry.hasOutgoingAccess());
        }
        List<PlayerAccessData> players = te.getPlayerAccessData();
        data.writeInt(players.size());
        for (PlayerAccessData entry : players) {
            data.writeUTF(entry.getPlayerName());
            data.writeUTF(entry.getUuid() == null ? "" : entry.getUuid().toString());
            data.writeBoolean(entry.hasGateAccess());
            data.writeBoolean(entry.hasIrisAccess());
            data.writeBoolean(entry.isAdmin());
        }
        data.close();
    }

    @ClientMessageHandler("AccessLists")
    public void handleAccessListsFromServer(EntityPlayer player, ChannelInput data) {
        BlockPos pos = readCoords(data);
        List<GateAccessData> gates = new ArrayList<>();
        for (int i = data.readInt(); i > 0; i--) {
            gates.add(new GateAccessData(data.readUTF(), data.readBoolean(), data.readBoolean()));
        }
        List<PlayerAccessData> players = new ArrayList<>();
        for (int i = data.readInt(); i > 0; i--) {
            String name = data.readUTF();
            String uuid = data.readUTF();
            players.add(new PlayerAccessData(name, uuid.isEmpty() ? null : UUID.fromString(uuid), data.readBoolean(), data.readBoolean(), data.readBoolean()));
        }
        SGBaseTE te = player == null ? null : SGBaseTE.at(player.world, pos);
        if (te != null) {
            te.setClientAccessLists(gates, players);
        }
    }

    // The UUID of the player with this name: one online now, or who has been on the server (see PlayerNames; Mojang
    // isn't asked, which held up the server). Null for a name nobody here has had; the entry is then tied to whoever
    // first uses the gate under that name.
    @Nullable
    private static UUID uuidForName(EntityPlayer editor, String name) {
        GameProfile profile = new PlayerNames(editor.getServer()).find(name);
        return profile != null ? profile.getId() : null;
    }

    // How many entries each of a gate's access lists may have
    private static final int maxAccessEntries = 256;

    public static void sendGateAddressAccessInputToServer(SGBaseTE te, String address, boolean defaultAllowIncoming, boolean defaultAllowOutgoing, boolean allowIncoming, boolean allowOutgoing) {
        ChannelOutput data = configuratorChannel.openServer("GateAddressAccessInput");
        writeCoords(data, te);
        data.writeUTF(address);
        data.writeBoolean(defaultAllowIncoming);
        data.writeBoolean(defaultAllowOutgoing);
        data.writeBoolean(allowIncoming);
        data.writeBoolean(allowOutgoing);
        data.close();
    }

    @ServerMessageHandler("GateAddressAccessInput")
    public void handleGateAddressAccessInputFromClient(EntityPlayer player, ChannelInput data) {
        BlockPos pos = readCoords(data);
        SGBaseTE te = loadedGateAt(player, pos);

        String address = data.readUTF();
        boolean defaultAllowIncoming = data.readBoolean();
        boolean defaultAllowOutgoing = data.readBoolean();
        boolean allowIncoming = data.readBoolean();
        boolean allowOutgoing = data.readBoolean();

        if (canConfigure(player, te)) {
            if (address.isEmpty()) { // indicates the user clicked the bottom save button
                te.defaultAllowIncoming = defaultAllowIncoming;
                te.defaultAllowOutgoing = defaultAllowOutgoing;
            }

            if (!address.isEmpty()) {
                te.setAllowIncomingAddress(address, allowIncoming);
                te.setAllowOutgoingAddress(address, allowOutgoing);
            }

            accessChanged(player, te);
        }
    }

    public static void sendPlayerAccessInputToServer(SGBaseTE te, String playerName, boolean defaultAllowAccess, boolean defaultAllowIris, boolean defaultAllowAdmin, boolean allowAccess, boolean allowIris, boolean allowAdmin) {
        ChannelOutput data = configuratorChannel.openServer("PlayerAccessInput");
        writeCoords(data, te);
        data.writeUTF(playerName);
        data.writeBoolean(defaultAllowAccess);
        data.writeBoolean(defaultAllowIris);
        data.writeBoolean(defaultAllowAdmin);
        data.writeBoolean(allowAccess);
        data.writeBoolean(allowIris);
        data.writeBoolean(allowAdmin);
        data.close();
    }

    @ServerMessageHandler("PlayerAccessInput")
    public void handlePlayerAccessInputFromClient(EntityPlayer player, ChannelInput data) {
        BlockPos pos = readCoords(data);
        SGBaseTE te = loadedGateAt(player, pos);

        String playerName = data.readUTF();
        boolean defaultAllowAccess = data.readBoolean();
        boolean defaultAllowIris = data.readBoolean();
        boolean defaultAllowAdmin = data.readBoolean();
        boolean allowAccess = data.readBoolean();
        boolean allowIris = data.readBoolean();
        boolean allowAdmin = data.readBoolean();

        if (canConfigure(player, te)) {
            if (playerName.isEmpty()) {
                te.defaultAllowGateAccess = defaultAllowAccess;
                te.defaultAllowIrisAccess = defaultAllowIris;
                te.defaultAllowAdminAccess = defaultAllowAdmin;
            } else {
                te.setAllowGateAccessAccess(playerName, allowAccess);
                te.setAllowAccessToIrisController(playerName, allowIris);
                te.setAllowAccessAdmin(playerName, allowAdmin);
            }

            accessChanged(player, te);
        }
    }

    public static void sendGAAEntryUpdateToServer(SGBaseTE te, String oldAddress, String newAddress, int function) {
        ChannelOutput data = configuratorChannel.openServer("GAAInputEntry");
        writeCoords(data, te);
        data.writeUTF(oldAddress);
        data.writeUTF(newAddress);
        data.writeInt(function);
        data.close();
    }

    @ServerMessageHandler("GAAInputEntry")
    public void handleGAAEntryUpdateFromClient(EntityPlayer player, ChannelInput data) {
        BlockPos pos = readCoords(data);
        SGBaseTE localGate = loadedGateAt(player, pos);
        String oldAddress = data.readUTF();
        String newAddress = data.readUTF();
        int function = data.readInt();

        if (!canConfigure(player, localGate)) {
            return;
        }

        List<GateAccessData> list = localGate.getGateAccessData();
        GateAccessData existing = oldAddress.isEmpty() ? null : localGate.findGateAccessEntry(oldAddress);

        if (function == 1 || function == 2) { // Add, or change an entry's address
            // Only full, valid addresses, written XXXX-XXX-XX, and each only once
            String address = GateAccessData.formatForList(newAddress);
            if (address == null) {
                sendErrorMsg(player, "invalidAccessListAddress", newAddress);
                return;
            }
            GateAccessData clash = localGate.findGateAccessEntry(address);
            if (clash != null && clash != existing) {
                sendErrorMsg(player, "duplicateAccessListAddress", address);
                return;
            }
            if (function == 1 && oldAddress.isEmpty()) {
                if (list.size() >= maxAccessEntries) {
                    return;
                }
                list.add(new GateAccessData(address, true, true));
            } else if (function == 2 && existing != null) {
                existing.setAddress(address);
            }
        } else if (function == 3 && existing != null) { // Delete
            list.remove(existing);
        }

        accessChanged(player, localGate);
    }

    public static void sendPAEntryUpdateToServer(SGBaseTE te, String oldName, String newName, int function) {
        ChannelOutput data = configuratorChannel.openServer("PAInputEntry");
        writeCoords(data, te);
        data.writeUTF(oldName);
        data.writeUTF(newName);
        data.writeInt(function);
        data.close();
    }

    @ServerMessageHandler("PAInputEntry")
    public void handlePAEntryUpdateFromClient(EntityPlayer player, ChannelInput data) {
        BlockPos pos = readCoords(data);
        SGBaseTE localGate = loadedGateAt(player, pos);
        String oldName = data.readUTF();
        String newName = data.readUTF();
        int function = data.readInt();

        if (!canConfigure(player, localGate)) {
            return;
        }

        List<PlayerAccessData> list = localGate.getPlayerAccessData();
        PlayerAccessData existing = oldName.isEmpty() ? null : localGate.findPlayerAccessEntry(oldName);

        if (function == 1 || function == 2) { // Add, or change which player an entry is for
            String name = newName.trim();
            if (!PlayerAccessData.isValidName(name)) {
                sendErrorMsg(player, "invalidAccessListPlayer", name);
                return;
            }
            // Each player only once, whether by name or, when known, by UUID
            UUID uuid = uuidForName(player, name);
            for (PlayerAccessData other : list) {
                if (other != existing && (other.getPlayerName().equalsIgnoreCase(name) || (uuid != null && uuid.equals(other.getUuid())))) {
                    sendErrorMsg(player, "duplicateAccessListPlayer", name);
                    return;
                }
            }
            if (function == 1 && oldName.isEmpty()) {
                if (list.size() >= maxAccessEntries) {
                    return;
                }
                // Adding yourself makes you an admin, so you can't lock yourself out of the gate's settings
                list.add(new PlayerAccessData(name, uuid, true, true, player.getUniqueID().equals(uuid)));
            } else if (function == 2 && existing != null) {
                existing.setPlayerName(name);
                existing.setUuid(uuid);
            }
        } else if (function == 3 && existing != null) { // Delete
            list.remove(existing);
        }

        accessChanged(player, localGate);
    }

    public static void openGuiAtClient(SGBaseTE te,
            EntityPlayer player,
            int guiType,
            boolean isAdmin,
            boolean secondsToStayOpenPerm,
            boolean gateRotationSpeedPerm,
            boolean energyBufferSizePerm,
            boolean energyPerNaquadahPerm,
            boolean openingsPerNaquadahPerm,
            boolean distanceFactorMultiplierPerm,
            boolean interDimensionalMultiplierPerm,
            boolean oneWayTravelOnlyPerm,
            boolean irisUpgradePerm,
            boolean chevronUpgradePerm,
            boolean pegasusGateTypePerm,
            boolean reverseWormholeKillsPerm,
            boolean closeFromEitherEndPerm,
            boolean preserveInventoryOnIrisDeathPerm,
            boolean noInputPowerRequiredPerm,
            boolean chevronsLockOnDialPerm,
            boolean returnToPreviousIrisStatePerm,
            boolean transientDamagePerm,
            boolean transparencyPerm,
            boolean dhdAsFuelSourcePerm,
            boolean allowRedstoneOutputPerm,
            boolean allowRedstoneInputPerm,
            boolean gateAccessPerm,
            boolean playerAccessPerm,
            boolean canPlayerBreakPlayerGatePerm,
            boolean canPlayerBreakGeneratedGatePerm,
            boolean displayGateAddressPerm,
            boolean isGeneratedPerm,
            boolean invulnerablePerm,
            boolean dynmapPerm) {

        ChannelOutput data = configuratorChannel.openPlayer(player,"OpenConfiguratorGUI");
        // Type is always 1 here. *for now*
        writeCoords(data, te);
        data.writeInt(guiType);
        data.writeBoolean(isAdmin);
        data.writeBoolean(secondsToStayOpenPerm);
        data.writeBoolean(gateRotationSpeedPerm);
        data.writeBoolean(energyBufferSizePerm);
        data.writeBoolean(energyPerNaquadahPerm);
        data.writeBoolean(openingsPerNaquadahPerm);
        data.writeBoolean(distanceFactorMultiplierPerm);
        data.writeBoolean(interDimensionalMultiplierPerm);
        data.writeBoolean(oneWayTravelOnlyPerm);
        data.writeBoolean(irisUpgradePerm);
        data.writeBoolean(chevronUpgradePerm);
        data.writeBoolean(pegasusGateTypePerm);
        data.writeBoolean(reverseWormholeKillsPerm);
        data.writeBoolean(closeFromEitherEndPerm);
        data.writeBoolean(preserveInventoryOnIrisDeathPerm);
        data.writeBoolean(noInputPowerRequiredPerm);
        data.writeBoolean(chevronsLockOnDialPerm);
        data.writeBoolean(returnToPreviousIrisStatePerm);
        data.writeBoolean(transientDamagePerm);
        data.writeBoolean(transparencyPerm);
        data.writeBoolean(dhdAsFuelSourcePerm);
        data.writeBoolean(allowRedstoneOutputPerm);
        data.writeBoolean(allowRedstoneInputPerm);
        data.writeBoolean(gateAccessPerm);
        data.writeBoolean(playerAccessPerm);
        data.writeBoolean(canPlayerBreakPlayerGatePerm);
        data.writeBoolean(canPlayerBreakGeneratedGatePerm);
        data.writeBoolean(displayGateAddressPerm);
        data.writeBoolean(isGeneratedPerm);
        data.writeBoolean(invulnerablePerm);
        data.writeBoolean(dynmapPerm);
        data.close();
    }

    @ClientMessageHandler("OpenConfiguratorGUI")
    public void handleGuiOpenRequest(EntityPlayer player, ChannelInput data) {

        BlockPos pos = readCoords(data);
        int guiType = data.readInt();
        boolean isAdmin = data.readBoolean();
        boolean secondsToStayOpenPerm = data.readBoolean();
        boolean gateRotationSpeedPerm  = data.readBoolean();
        boolean energyBufferSizePerm  = data.readBoolean();
        boolean energyPerNaquadahPerm  = data.readBoolean();
        boolean openingsPerNaquadahPerm = data.readBoolean();
        boolean distanceFactoryMultiplierPerm  = data.readBoolean();
        boolean interDimensionalMultiplierPerm  = data.readBoolean();
        boolean oneWayTravelOnlyPerm  = data.readBoolean();
        boolean irisUpgradePerm  = data.readBoolean();
        boolean chevronUpgradePerm  = data.readBoolean();
        boolean pegasusGateTypePerm  = data.readBoolean();
        boolean reverseWormholeKillsPerm  = data.readBoolean();
        boolean closeFromEitherEndPerm  = data.readBoolean();
        boolean preserveInventoryOnIrisDeathPerm  = data.readBoolean();
        boolean noInputPowerRequiredPerm  = data.readBoolean();
        boolean chevronsLockOnDialPerm  = data.readBoolean();
        boolean returnToPreviousIrisStatePerm  = data.readBoolean();
        boolean transientDamagePerm  = data.readBoolean();
        boolean transparencyPerm  = data.readBoolean();
        boolean dhdAsFuelSourcePerm  = data.readBoolean();
        boolean allowRedstoneOutputPerm  = data.readBoolean();
        boolean allowRedstoneInputPerm  = data.readBoolean();
        boolean gateAccessPerm = data.readBoolean();
        boolean playerAccessPerm = data.readBoolean();
        boolean canPlayerBreakPlayerGatePerm = data.readBoolean();
        boolean canPlayerBreakGeneratedGatePerm = data.readBoolean();
        boolean displayGateAddressPerm = data.readBoolean();
        boolean isGeneratedPerm = data.readBoolean();
        boolean invulnerablePerm = data.readBoolean();
        boolean dynmapPerm = data.readBoolean();

        if (guiType == 1) {
            new ConfiguratorScreen(player,
                    player.world,
                    pos,
                    isAdmin,
                    secondsToStayOpenPerm,
                    gateRotationSpeedPerm,
                    energyBufferSizePerm,
                    energyPerNaquadahPerm,
                    openingsPerNaquadahPerm,
                    distanceFactoryMultiplierPerm,
                    interDimensionalMultiplierPerm,
                    oneWayTravelOnlyPerm,
                    irisUpgradePerm,
                    chevronUpgradePerm,
                    pegasusGateTypePerm,
                    reverseWormholeKillsPerm,
                    closeFromEitherEndPerm,
                    preserveInventoryOnIrisDeathPerm,
                    noInputPowerRequiredPerm,
                    chevronsLockOnDialPerm,
                    returnToPreviousIrisStatePerm,
                    transientDamagePerm,
                    transparencyPerm,
                    dhdAsFuelSourcePerm,
                    allowRedstoneOutputPerm,
                    allowRedstoneInputPerm,
                    canPlayerBreakPlayerGatePerm,
                    canPlayerBreakGeneratedGatePerm,
                    displayGateAddressPerm,
                    isGeneratedPerm,
                    gateAccessPerm,
                    playerAccessPerm,
                    invulnerablePerm,
                    dynmapPerm)
                    .display();
        }
    }
}
