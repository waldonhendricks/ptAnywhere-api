package uk.ac.open.kmi.forge.ptAnywhere.gateway.impl;

import com.cisco.pt.ipc.sim.Device;
import com.cisco.pt.ipc.sim.Network;
import com.cisco.pt.ipc.ui.IPC;
import com.cisco.pt.ipc.ui.LogicalWorkspace;
import uk.ac.open.kmi.forge.ptAnywhere.api.http.Utils;
import uk.ac.open.kmi.forge.ptAnywhere.exceptions.DeviceNotFoundException;
import uk.ac.open.kmi.forge.ptAnywhere.gateway.Cache;

import java.util.*;


/**
 * PacketTracer DAO implementation which caches device names.
 *
 * For more info, visit: https://github.com/PTAnywhere/ptAnywhere-api/issues/9
 */
public class CachedPacketTracerDAO extends BasicPacketTracerDAO {

    final Cache cache;
    final String networkId;

    public CachedPacketTracerDAO(IPC ipc, Cache cache) {
        this(ipc.appWindow().getActiveWorkspace().getLogicalWorkspace(), ipc.network(), cache);
    }

    protected CachedPacketTracerDAO(LogicalWorkspace workspace, Network network, Cache cache) {
        super(workspace, new CachingNetwork(network, cache));
        this.networkId = Utils.toSimplifiedId(network.getObjectUUID());
        this.cache = cache;
    }

    @Override
    public uk.ac.open.kmi.forge.ptAnywhere.pojo.Network getWholeNetwork() {
        final uk.ac.open.kmi.forge.ptAnywhere.pojo.Network ret = super.getWholeNetwork();
        // Additional loop. We could improve this, but it is cleaner this way.
        for (uk.ac.open.kmi.forge.ptAnywhere.pojo.Device d: ret.getDevices()) {
            this.cache.add(this.networkId, d.getId(), d.getLabel());
        }
        return ret;
    }

    @Override
    public com.cisco.pt.ipc.sim.Device getSimDeviceById(String simplifiedId) throws DeviceNotFoundException {
        final String name = this.cache.getName(this.networkId, simplifiedId);
        if (name==null) {  // Not in the cache
            return super.getSimDeviceById(simplifiedId);
        } else {
            return super.getSimDeviceByName(name);
        }
    }

    @Override
    public String getDeviceName(String simplifiedId) {
        final String name = this.cache.getName(this.networkId, simplifiedId);
        if (name!=null) return name;
        return super.getDeviceName(simplifiedId);
    }

    @Override
    protected Map<String, com.cisco.pt.ipc.sim.Device> getSimDevicesByIds(String... deviceIds) throws DeviceNotFoundException {
        final Map<String, Device> ret = new HashMap<String, Device>();
        final Set<String> toFindById = new HashSet<String>();
        for(String deviceId: deviceIds) {
            final String name = this.cache.getName(this.networkId, deviceId);
            if (name==null) {
                toFindById.add(deviceId);
            } else { // Device name in the cache
                // getSimDeviceByName might throw DeviceNotFoundException
                ret.put(deviceId, super.getSimDeviceByName(name) );
            }
        }
        if (ret.size()==deviceIds.length) return ret;
        return super.getSimDevicesByIds(ret, toFindById.toArray(new String[toFindById.size()]));
    }

    /***************************** Cache Updates  *******************************/
    @Override
    public uk.ac.open.kmi.forge.ptAnywhere.pojo.Device createDevice(uk.ac.open.kmi.forge.ptAnywhere.pojo.Device device) {
        // createDevice => already caches it when it calls to getSimDeviceById
        final uk.ac.open.kmi.forge.ptAnywhere.pojo.Device ret = super.createDevice(device);
        // However, the label might have been changed if it was specified in the original object.
        // As a consequence, we should forget the cached name.
        if (device.getLabel()!=null) {
            this.cache.remove(this.networkId, ret.getId());
            this.cache.add(this.networkId, ret.getId(), device.getLabel());
        }
        return ret;
    }

    @Override
    public uk.ac.open.kmi.forge.ptAnywhere.pojo.Device removeDevice(String deviceId) {
        final uk.ac.open.kmi.forge.ptAnywhere.pojo.Device ret = super.removeDevice(deviceId);
        this.cache.remove(this.networkId, deviceId);
        return ret;
    }

    @Override
    public uk.ac.open.kmi.forge.ptAnywhere.pojo.Device modifyDevice(uk.ac.open.kmi.forge.ptAnywhere.pojo.Device modification) {
        final uk.ac.open.kmi.forge.ptAnywhere.pojo.Device ret = super.modifyDevice(modification);
        this.cache.remove(this.networkId, modification.getId());
        this.cache.add(this.networkId, modification.getId(), ret.getLabel());
        return ret;
    }
}
