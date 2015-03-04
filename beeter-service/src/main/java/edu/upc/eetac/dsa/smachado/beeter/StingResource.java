/*
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Sergio Machado
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package edu.upc.eetac.dsa.smachado.beeter;

import edu.upc.eetac.dsa.smachado.beeter.db.Database;
import edu.upc.eetac.dsa.smachado.beeter.model.Sting;
import edu.upc.eetac.dsa.smachado.beeter.model.StingCollection;

import javax.sql.DataSource;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;

/**
 * Created by Sergio Machado on 27/02/15.
 */
@Path("/stings")
public class StingResource {
    private String GET_STING_BY_ID_QUERY = "select s.*, u.name from stings s, users u where u.username=s.username and s.stingid=?";
    private String GET_STINGS_QUERY = "select s.*, u.name from stings s, users u where u.username=s.username and s.creation_timestamp < ifnull(?, now()) order by creation_timestamp desc limit ?";
    private String GET_STINGS_QUERY_FROM_LAST = "select s.*, u.name from stings s, users u where u.username=s.username and s.creation_timestamp > ? order by creation_timestamp desc";
    private String INSERT_STING_QUERY = "insert into stings (username, subject, content) values (?, ?, ?)";
    private String DELETE_STING_QUERY = "delete from stings where stingid=?";
    private String UPDATE_STING_QUERY = "update stings set subject=ifnull(?, subject), content=ifnull(?, content) where stingid=?";

    @GET
    @Produces(BeeterMediaType.BEETER_API_STING_COLLECTION)
    public StingCollection getStings(@QueryParam("length") int length,
                                     @QueryParam("before") long before, @QueryParam("after") long after) {
        StingCollection stings = new StingCollection();

        Connection conn = null;
        try {
            conn = Database.getConnection();
        } catch (SQLException e) {
            throw new ServerErrorException("Could not connect to the database",
                    Response.Status.SERVICE_UNAVAILABLE);
        }

        PreparedStatement stmt = null;
        try {
            boolean updateFromLast = after > 0;
            stmt = updateFromLast ? conn
                    .prepareStatement(GET_STINGS_QUERY_FROM_LAST) : conn
                    .prepareStatement(GET_STINGS_QUERY);
            if (updateFromLast) {
                stmt.setTimestamp(1, new Timestamp(after));
            } else {
                if (before > 0)
                    stmt.setTimestamp(1, new Timestamp(before));
                else
                    stmt.setTimestamp(1, null);
                length = (length <= 0) ? 5 : length;
                stmt.setInt(2, length);
            }
            ResultSet rs = stmt.executeQuery();
            boolean first = true;
            long oldestTimestamp = 0;
            while (rs.next()) {
                Sting sting = new Sting();
                sting.setStingid(rs.getInt("stingid"));
                sting.setUsername(rs.getString("username"));
                sting.setAuthor(rs.getString("name"));
                sting.setSubject(rs.getString("subject"));
                oldestTimestamp = rs.getTimestamp("last_modified").getTime();
                sting.setLastModified(oldestTimestamp);
                if (first) {
                    first = false;
                    stings.setNewestTimestamp(sting.getLastModified());
                }
                stings.addSting(sting);
            }
            stings.setOldestTimestamp(oldestTimestamp);
        } catch (SQLException e) {
            throw new ServerErrorException(e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
                conn.close();
            } catch (SQLException e) {
            }
        }

        return stings;
    }

    @GET
    @Path("/{stingid}")
    @Produces(BeeterMediaType.BEETER_API_STING)
    public Response getSting(@PathParam("stingid") String stingid,
                             @Context Request request) {
        // Create CacheControl
        CacheControl cc = new CacheControl();

        Sting sting = getStingFromDatabase(stingid);

        // Calculate the ETag on last modified date of user resource
        EntityTag eTag = new EntityTag(Long.toString(sting.getLastModified()));

        // Verify if it matched with etag available in http request
        Response.ResponseBuilder rb = request.evaluatePreconditions(eTag);

        // If ETag matches the rb will be non-null;
        // Use the rb to return the response without any further processing
        if (rb != null) {
            return rb.cacheControl(cc).tag(eTag).build();
        }

        // If rb is null then either it is first time request; or resource is
        // modified
        // Get the updated representation and return with Etag attached to it
        rb = Response.ok(sting).cacheControl(cc).tag(eTag);

        return rb.build();
    }

    private Sting getStingFromDatabase(String stingid) {
        Sting sting = new Sting();

        Connection conn = null;
        try {
            conn = Database.getConnection();
        } catch (SQLException e) {
            throw new ServerErrorException("Could not connect to the database",
                    Response.Status.SERVICE_UNAVAILABLE);
        }

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(GET_STING_BY_ID_QUERY);
            stmt.setInt(1, Integer.valueOf(stingid));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                sting.setStingid(rs.getInt("stingid"));
                sting.setUsername(rs.getString("username"));
                sting.setAuthor(rs.getString("name"));
                sting.setSubject(rs.getString("subject"));
                sting.setContent(rs.getString("content"));
                sting.setLastModified(rs.getTimestamp("last_modified")
                        .getTime());
                sting.setCreationTimestamp(rs
                        .getTimestamp("creation_timestamp").getTime());
            } else {
                throw new NotFoundException("There's no sting with stingid="
                        + stingid);
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
                conn.close();
            } catch (SQLException e) {
            }
        }

        return sting;
    }

    @POST
    @Consumes(BeeterMediaType.BEETER_API_STING)
    @Produces(BeeterMediaType.BEETER_API_STING)
    public Response createSting(Sting sting, @Context UriInfo uriInfo) throws URISyntaxException {
        validateSting(sting);
        Connection conn = null;
        try {
            conn = Database.getConnection();
        } catch (SQLException e) {
            throw new ServerErrorException("Could not connect to the database",
                    Response.Status.SERVICE_UNAVAILABLE);
        }

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(INSERT_STING_QUERY,
                    Statement.RETURN_GENERATED_KEYS);

            stmt.setString(1, sting.getUsername());
            stmt.setString(2, sting.getSubject());
            stmt.setString(3, sting.getContent());
            stmt.executeUpdate();
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int stingid = rs.getInt(1);

                sting = getStingFromDatabase(Integer.toString(stingid));
            } else {
                // Something has failed...
            }
        } catch (SQLException e) {
            throw new ServerErrorException(e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
                conn.close();
            } catch (SQLException e) {
            }
        }

        URI createdUri = new URI(uriInfo.getAbsolutePath().toString() + "/" + sting.getStingid());
        return Response.created(createdUri).entity(sting).build();
    }

    @DELETE
    @Path("/{stingid}")
    public void deleteSting(@PathParam("stingid") String stingid) {
        Connection conn = null;
        try {
            conn = Database.getConnection();
        } catch (SQLException e) {
            throw new ServerErrorException("Could not connect to the database",
                    Response.Status.SERVICE_UNAVAILABLE);
        }

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(DELETE_STING_QUERY);
            stmt.setInt(1, Integer.valueOf(stingid));

            int rows = stmt.executeUpdate();
            if (rows == 0)
                throw new NotFoundException("There's no sting with stingid="
                        + stingid);
        } catch (SQLException e) {
            throw new ServerErrorException(e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
                conn.close();
            } catch (SQLException e) {
            }
        }
    }


    @PUT
    @Path("/{stingid}")
    @Consumes(BeeterMediaType.BEETER_API_STING)
    @Produces(BeeterMediaType.BEETER_API_STING)
    public Sting updateSting(@PathParam("stingid") String stingid, Sting sting) {
        validateUpdateSting(sting);
        Connection conn = null;
        try {
            conn = Database.getConnection();
        } catch (SQLException e) {
            throw new ServerErrorException("Could not connect to the database",
                    Response.Status.SERVICE_UNAVAILABLE);
        }

        PreparedStatement stmt = null;
        try {
            String sql = UPDATE_STING_QUERY;
            stmt = conn.prepareStatement(sql);
            stmt.setString(1, sting.getSubject());
            stmt.setString(2, sting.getContent());
            stmt.setInt(3, Integer.valueOf(stingid));

            int rows = stmt.executeUpdate();
            if (rows == 1)
                sting = getStingFromDatabase(stingid);
            else {
                throw new NotFoundException("There's no sting with stingid="
                        + stingid);
            }

        } catch (SQLException e) {
            throw new ServerErrorException(e.getMessage(),
                    Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
                conn.close();
            } catch (SQLException e) {
            }
        }

        return sting;
    }

    private void validateUpdateSting(Sting sting) {
        if (sting.getSubject() != null && sting.getSubject().length() > 100)
            throw new BadRequestException(
                    "Subject can't be greater than 100 characters.");
        if (sting.getContent() != null && sting.getContent().length() > 500)
            throw new BadRequestException(
                    "Content can't be greater than 500 characters.");
    }

    private void validateSting(Sting sting) {
        if (sting.getSubject() == null)
            throw new BadRequestException("Subject can't be null.");
        if (sting.getContent() == null)
            throw new BadRequestException("Content can't be null.");
        if (sting.getSubject().length() > 100)
            throw new BadRequestException("Subject can't be greater than 100 characters.");
        if (sting.getContent().length() > 500)
            throw new BadRequestException("Content can't be greater than 500 characters.");
    }
}
