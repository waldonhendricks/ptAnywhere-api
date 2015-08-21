package uk.ac.open.kmi.forge.ptAnywhere.api.http;

import uk.ac.open.kmi.forge.ptAnywhere.analytics.InteractionRecord;
import uk.ac.open.kmi.forge.ptAnywhere.gateway.PTCallable;
import uk.ac.open.kmi.forge.ptAnywhere.pojo.HalfLink;
import uk.ac.open.kmi.forge.ptAnywhere.pojo.InnerLink;
import uk.ac.open.kmi.forge.ptAnywhere.pojo.Link;
import uk.ac.open.kmi.forge.ptAnywhere.session.SessionManager;

import static uk.ac.open.kmi.forge.ptAnywhere.api.http.URLFactory.PORT_PARAM;
import static uk.ac.open.kmi.forge.ptAnywhere.api.http.URLFactory.DEVICE_PARAM;

import javax.servlet.ServletContext;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;


abstract class AbstractPortLinkHandler extends  PTCallable<Link> {
    final String deviceId;
    final String portName;
    final URLFactory uf;
    public AbstractPortLinkHandler(SessionManager sm, String deviceId, String portName, URI baseURI) {
        super(sm);
        this.deviceId = deviceId;
        this.portName = portName;
        this.uf = new URLFactory(baseURI, sm.getSessionId(), deviceId);
    }
    @Override
    public Link internalRun() {
        final InnerLink il = handleLink();
        if (il==null) return null;
        return Link.createFromInnerLink(il, this.uf);
    }
    abstract InnerLink handleLink();
}

class PortLinkGetter extends AbstractPortLinkHandler {
    public PortLinkGetter(SessionManager sm, String deviceId, String portName, URI baseURI) {
        super(sm, deviceId, portName, baseURI);
    }
    @Override
    public InnerLink handleLink() {
        return this.connection.getDataAccessObject().getLink(this.deviceId, this.portName);
    }
}

class LinkDeleter extends AbstractPortLinkHandler {
    public LinkDeleter(SessionManager sm, String deviceId, String portName, URI baseURI) {
        super(sm, deviceId, portName, baseURI);
    }
    @Override
    public InnerLink handleLink() {
        final InnerLink il = this.connection.getDataAccessObject().getLink(this.deviceId, this.portName);
        final boolean success = this.connection.getDataAccessObject().removeLink(this.deviceId, this.portName);
        if (success)
            return il;
        return null;
    }
}

class LinkCreator extends AbstractPortLinkHandler {
    final HalfLink linkToCreate;
    public LinkCreator(SessionManager sm, String deviceId, String portName, HalfLink linkToCreate, URI baseURI) {
        super(sm, deviceId, portName, baseURI);
        this.linkToCreate = linkToCreate;
    }
    @Override
    public InnerLink handleLink() {
        final boolean success = this.connection.getDataAccessObject().createLink(this.deviceId, this.portName, this.linkToCreate);
        if (success)
            // Improvable performance
            return this.connection.getDataAccessObject().getLink(this.deviceId, this.portName);
        return null;
    }
}

public class PortLinkResource {

    final UriInfo uri;
    final SessionManager sm;

    public PortLinkResource(UriInfo uri, SessionManager sm) {
        this.uri = uri;
        this.sm = sm;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLink(@PathParam(DEVICE_PARAM) String deviceId,
                            @PathParam(PORT_PARAM) String portName) {
        final Link l = new PortLinkGetter(this.sm, deviceId, Utils.unescapePort(portName), this.uri.getBaseUri()).call();
        if (l==null)
            return Response.noContent().
                    links(getPortLink()).build();
        return Response.ok(l).
                links(getPortLink()).build();
                // TODO create endpoints links
    }
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeLink(@PathParam(DEVICE_PARAM) String deviceId,
                               @PathParam(PORT_PARAM) String portName,
                               @Context ServletContext servletContext) {
        final Link deletedLink = new LinkDeleter(this.sm, deviceId, Utils.unescapePort(portName), this.uri.getBaseUri()).call();
        if (deletedLink==null)
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(deletedLink).
                    links(getPortLink()).build();
        final InteractionRecord ir =  APIApplication.createInteractionRecord(servletContext);
        ir.deviceDisconnected(this.sm.getSessionId(), deletedLink.getUrl(), deletedLink.getEndpoints());
        return Response.ok(deletedLink).
                links(getPortLink()).build();
    }
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createLink(HalfLink newLink,
                             @PathParam(DEVICE_PARAM) String deviceId,
                             @PathParam(PORT_PARAM) String portName,
                               @Context ServletContext servletContext) {
        final Link createdLink = new LinkCreator(this.sm, deviceId, Utils.unescapePort(portName), newLink, this.uri.getBaseUri()).call();
        if (createdLink==null)
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(newLink).
                    links(getPortLink()).build();
        final InteractionRecord ir =  APIApplication.createInteractionRecord(servletContext);
        ir.deviceConnected(sm.getSessionId(), createdLink.getUrl(), createdLink.getEndpoints());
        return Response.created(this.uri.getRequestUri()).entity(createdLink).
                links(getPortLink()).build();
                // TODO create endpoints links
    }
    private javax.ws.rs.core.Link getPortLink() {
        return javax.ws.rs.core.Link.fromUri(Utils.getParent(this.uri.getRequestUri())).rel("port").build();  // Rename it to from?
    }
}