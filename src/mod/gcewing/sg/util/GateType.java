//------------------------------------------------------------------------------------------------
//
//   SG Craft - Visual and sound style of a stargate
//
//------------------------------------------------------------------------------------------------

package gcewing.sg.util;

public enum GateType {

    MILKY_WAY(1, "milkyway"),
    PEGASUS(2, "pegasus");

    // Value stored in SGBaseTE.gateType and in NBT
    public final int id;
    // Name used in texture paths
    public final String assetName;

    GateType(int id, String assetName) {
        this.id = id;
        this.assetName = assetName;
    }

    // Anything other than Pegasus is treated as Milky Way
    public static GateType fromId(int id) {
        return id == PEGASUS.id ? PEGASUS : MILKY_WAY;
    }

    public <T> T select(T milkyWay, T pegasus) {
        return this == PEGASUS ? pegasus : milkyWay;
    }

}
