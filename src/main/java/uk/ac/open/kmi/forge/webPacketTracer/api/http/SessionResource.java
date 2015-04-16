package uk.ac.open.kmi.forge.webPacketTracer.api.http;

import uk.ac.open.kmi.forge.webPacketTracer.session.SessionManager;

import javax.ws.rs.*;
import javax.ws.rs.core.*;

@Path("sessions/{session}")
public class SessionResource {

    @Context
    UriInfo uri;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSession(@PathParam("session") String sessionId) {
        final String requestUri = Utils.getURIWithSlashRemovingQuery(this.uri.getRequestUri());
        final SessionManager sm = SessionManager.create();
        if (sm.doesExist(sessionId)) {
            return Response.ok(sessionId).
                    links(getSessionsLink()).
                    link(requestUri + "devices", "devices").
                    link(requestUri + "network", "network").build();
        }
        return Response.status(Response.Status.NOT_FOUND).
                links(getSessionsLink()).build();
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeDevice(@PathParam("session") String sessionId) {
        final String requestUri = Utils.getURIWithSlashRemovingQuery(this.uri.getRequestUri());
        final SessionManager sm = SessionManager.create();
        if (sm.doesExist(sessionId)) {
            sm.deleteSession(sessionId);
            return Response.ok(). // TODO return deleted session.
                    links(getSessionsLink()).build();
        }
        return Response.status(Response.Status.NOT_FOUND).
                links(getSessionsLink()).build();

    }

    private Link getSessionsLink() {
        return Link.fromUri(Utils.getParent(this.uri.getRequestUri())).rel("collection").build();
    }
}
