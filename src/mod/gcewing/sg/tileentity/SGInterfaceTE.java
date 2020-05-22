//------------------------------------------------------------------------------------------------
//
//   SG Craft - Base class for stargate computer interface tile entities
//
//------------------------------------------------------------------------------------------------

package gcewing.sg.tileentity;

import gcewing.sg.BaseTileEntity;
import gcewing.sg.util.SGAddressing;
import gcewing.sg.util.SGAddressing.AddressingError;
import gcewing.sg.util.SGState;

public class SGInterfaceTE extends BaseTileEntity {

    public SGBaseTE getBaseTE() {
        return SGBaseTE.get(getWorld(), pos.add(0, 1, 0));
    }
    
    // Signature is really prependArgs(Object..., Object[])
    
    public static Object[] prependArgs(Object... args) {
        int preLength = args.length - 1;
        Object[] post = (Object[])args[preLength];
        Object[] xargs = new Object[preLength + post.length];
        System.arraycopy(args, 0, xargs, 0, preLength);
        System.arraycopy(post, 0, xargs, preLength, post.length);
        return xargs;
    }
    
    public void rebroadcastNetworkPacket(Object packet) {
    }

    public static class CIStargateState {
        public String state;
        public int chevrons;
        public String direction;
        public CIStargateState(String state, int chevrons, String direction) {
            this.state = state;
            this.chevrons = chevrons;
            this.direction = direction;
        }
    }
        
    public SGBaseTE requireBaseTE() {
        SGBaseTE te = getBaseTE();
        if (te != null && te.isMerged)
            return te;
        throw new IllegalArgumentException("missingStargate");
    }
    
    public SGBaseTE requireIrisTE() {
        SGBaseTE te = requireBaseTE();
        if (te != null && te.hasIrisUpgrade)
            return te;
        else
            throw new IllegalArgumentException("missingIris");
    }
    
    String directionDescription(SGBaseTE te) {
        //Todo: this was changed from te.isConnected() since that doesn't help during dialing process.
        if (te.state != SGState.Idle) {
            if (te.isInitiator) {
                return "Outgoing";
            } else {
                return "Incoming";
            }
        } else {
            return "Not Connected";
        }
    }
    
    public CIStargateState ciStargateState() {
        SGBaseTE te = getBaseTE();
        if (te != null)
            return new CIStargateState(te.sgStateDescription(), te.numEngagedChevrons, directionDescription(te));
        else
            return new CIStargateState("Offline", 0, "");
    }
    
    public double ciEnergyAvailable() {
        SGBaseTE te = getBaseTE();
        return te != null ? te.availableEnergy() : 0;
    }
    
    public double ciEnergyToDial(String address, boolean pending) {
        SGBaseTE te = requireBaseTE();
        try {
            address = SGAddressing.normalizeAddress(address);
            SGBaseTE dte = SGAddressing.findAddressedStargate(address, te.getWorld(), pending);
            if (dte == null)
                throw new IllegalArgumentException("unknownAddress");
            double distanceFactor = dte.distanceFactorForCoordDifference(te, dte);
            return te.energyToOpen * distanceFactor;
        } catch (AddressingError e) {
            //System.out.printf("SGBaseTE.ciEnergyToDial: caught %s\n", e);
            throw new IllegalArgumentException(e.getMessage());
        }
    }
    
    public String ciLocalAddress() {
        SGBaseTE te = getBaseTE();
        try {
            return te != null ? te.getHomeAddress() : "";
        } catch (AddressingError e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
    
    public String ciRemoteAddress() {
        SGBaseTE te = requireBaseTE();
        try {
            return te.connectedLocation != null ? SGAddressing.addressForLocation(te.connectedLocation) : "";
        } catch (AddressingError e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
    
    public void ciDial(String address, boolean pending) {
        SGBaseTE te = requireBaseTE();
//         try {
//             address = SGAddressing.normalizedRelativeAddress(address, te.getHomeAddress());
//         }
//         catch (AddressingError e) {
//             throw new IllegalArgumentException(e.getMessage());
//         }
        address = SGAddressing.normalizeAddress(address);
        //System.out.printf("SGBaseTE.ciDial: dialling symbols %s\n", address);
        String error = te.connect(address, null, true, pending);
        if (error != null)
            throw new IllegalArgumentException(error);
    }
    
    public void ciDisconnect() {
        SGBaseTE te = requireBaseTE();
        String error = te.disconnect(null);
        if (error != null)
            throw new IllegalArgumentException(error);
    }
    
    public String ciIrisState() {
        SGBaseTE te = getBaseTE();
        return te != null && te.hasIrisUpgrade ? te.irisStateDescription() : "Offline";
    }
    
    public void ciOpenIris() {
        requireIrisTE().openIris();
    }
    
    public void ciCloseIris() {
        requireIrisTE().closeIris();
    }
    
    public void ciSendMessage(Object[] args) {
        SGBaseTE te = requireBaseTE();
        String error = te.sendMessage(args);
        if (error != null)
            throw new IllegalArgumentException(error);
    }
}
