package gcewing.sg.tileentity.data;

import static com.google.common.base.Preconditions.checkNotNull;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

public class PlayerAccessData {
    public static final String PLAYERACCESSLIST = "playeraccesslist";
    private static final String PLAYER = "player";
    private static final String UUID_KEY = "uuid";
    private static final String ACCESS = "access";
    private static final String IRIS = "iris";
    private static final String ADMIN = "admin";

    // What Minecraft allows in a player name
    private static final Pattern VALID_NAME = Pattern.compile("[A-Za-z0-9_]{1,16}");

    private String playerName;
    // The player the entry belongs to. Null until known (an entry added for a name the server has never seen); the
    // first player to use the gate under that name is then tied to it.
    @Nullable private UUID uuid;
    private boolean access;
    private boolean iris;
    private boolean admin;

    public PlayerAccessData(final @NonNull EntityPlayer player, final boolean access, boolean iris, boolean admin) {
        this(player.getName(), player.getUniqueID(), access, iris, admin);
    }
    public PlayerAccessData(final String playerName, @Nullable final UUID uuid, final boolean access, boolean iris, boolean admin) {
        this.playerName = playerName;
        this.uuid = uuid;
        this.access = access;
        this.iris = iris;
        this.admin = admin;
    }

    public static boolean isValidName(String name) {
        return name != null && VALID_NAME.matcher(name).matches();
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    @Nullable
    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(@Nullable UUID uuid) {
        this.uuid = uuid;
    }

    // An entry belongs to a player by UUID once known; before that, by name
    public boolean isFor(EntityPlayer player) {
        return uuid != null ? uuid.equals(player.getUniqueID()) : playerName.equalsIgnoreCase(player.getName());
    }

    public boolean hasGateAccess() {
        return access;
    }

    public void setGateAccess(boolean value) {
        this.access = value;
    }

    public boolean hasIrisAccess() {
        return iris;
    }

    public void setIrisAccess(boolean value) {
        this.iris = value;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean value) {
        this.admin = value;
    }

    public static List<PlayerAccessData> getPlayerAccessList(final NBTTagCompound compound) {
        checkNotNull(compound);

        final List<PlayerAccessData> playerAccessData = new ArrayList<>();
        final NBTTagList list = compound.getTagList(PLAYERACCESSLIST, Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            final NBTTagCompound entry = list.getCompoundTagAt(i);

            UUID uuid = null;
            if (entry.hasKey(UUID_KEY, Constants.NBT.TAG_STRING)) {
                try {
                    uuid = UUID.fromString(entry.getString(UUID_KEY));
                } catch (IllegalArgumentException ignored) {
                }
            }
            playerAccessData.add(new PlayerAccessData(entry.getString(PLAYER), uuid, entry.getBoolean(ACCESS), entry.getBoolean(IRIS),
                entry.getBoolean(ADMIN)));
        }

        return playerAccessData;
    }

    public static NBTTagCompound writeAddresses(final NBTTagCompound compound, final List<PlayerAccessData> playerAccessData) {
        checkNotNull(compound);
        checkNotNull(playerAccessData);

        final NBTTagList list = new NBTTagList();
        for (final PlayerAccessData data : playerAccessData) {
            final NBTTagCompound entry = new NBTTagCompound();
            entry.setString(PLAYER, data.getPlayerName());
            if (data.getUuid() != null) {
                entry.setString(UUID_KEY, data.getUuid().toString());
            }
            entry.setBoolean(ACCESS, data.hasGateAccess());
            entry.setBoolean(IRIS, data.hasIrisAccess());
            entry.setBoolean(ADMIN, data.isAdmin());
            list.appendTag(entry);
        }
        compound.setTag(PLAYERACCESSLIST, list);

        return compound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PlayerAccessData that = (PlayerAccessData) o;
        return access == that.access && iris == that.iris && admin == that.admin && Objects.equals(playerName, that.playerName)
            && Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerName, uuid, access, iris, admin);
    }
}
