package uk.ac.open.kmi.forge.webPacketTracer.api.http;

import java.net.URI;

public class URLFactory {

    static public final String SESSION_PATH = "sessions";
    static public final String SESSION_PARAM = "session";
    static public final String NETWORK_PATH = "network";
    static public final String LINKS_PATH = "links";
    static public final String LINK_PARAM = "link";
    static public final String CONTEXT_PATH = "contexts";
    static public final String CONTEXT_DEVICE_PATH = "device.jsonld";
    static public final String DEVICE_PATH = "devices";
    static public final String DEVICE_PARAM = "device";
    static public final String PORT_PATH = "ports";
    static public final String PORT_PARAM = "port";
    static public final String PORT_LINK_PATH = "link";

    final String baseUri;
    final String sessionId;

    URLFactory(URI baseUri, String sessionId) {
        this.baseUri = Utils.getURIWithSlashRemovingQuery(baseUri);
        this.sessionId = sessionId;
    }

    public String getSessionURL() {
        return this.baseUri + SESSION_PATH + "/" + this.sessionId + "/";
    }

    public String getDevicesURL() {
        return getSessionURL() + DEVICE_PATH + "/";
    }

    public String createDeviceURL(String id) {
        return getDevicesURL() + id + "/";
    }

    public String createLinkURL(String id) {
        return getSessionURL() + LINKS_PATH + "/" + id;
    }

    public String createPortsURL(String deviceId) {
        return createDeviceURL(deviceId) + PORT_PATH + "/";
    }

    public String createPortURL(String deviceId, String portId) {
        return createPortsURL(deviceId) + portId + "/";
    }

    public String createPortLinkURL(String deviceId, String portId) {
        return createPortsURL(deviceId) + portId + PORT_LINK_PATH;
    }
}