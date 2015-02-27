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
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.*;

/**
 * Created by Sergio Machado on 27/02/15.
 */
@Path("/stings")
public class StingResource {
    private String GET_STINGS_QUERY = "select s.*, u.name from stings s, users u where u.username=s.username order by creation_timestamp desc";
    private String GET_STING_BY_ID_QUERY = "select s.*, u.name from stings s, users u where u.username=s.username and s.stingid=?";
    private String INSERT_STING_QUERY = "insert into stings (username, subject, content) values (?, ?, ?)";
    private String DELETE_STING_QUERY = "delete from stings where stingid=?";
    private String UPDATE_STING_QUERY = "update stings set subject=ifnull(?, subject), content=ifnull(?, content) where stingid=?";

    @GET
    @Produces(BeeterMediaType.BEETER_API_STING_COLLECTION)
    public StingCollection getStings() {
        StingCollection stings = new StingCollection();

        Connection conn = null;
        try {
            conn = Database.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(GET_STINGS_QUERY);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Sting sting = new Sting();
                sting.setStingid(rs.getInt("stingid"));
                sting.setUsername(rs.getString("username"));
                sting.setAuthor(rs.getString("name"));
                sting.setSubject(rs.getString("subject"));
                sting.setLastModified(rs.getTimestamp("last_modified")
                        .getTime());
                sting.setCreationTimestamp(rs
                        .getTimestamp("creation_timestamp").getTime());
                stings.addSting(sting);
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
    public Sting getSting(@PathParam("stingid") String stingid) {
        Sting sting = new Sting();

        Connection conn = null;
        try {
            conn = Database.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
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
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
        Connection conn = null;
        try {
            conn = Database.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
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

                sting = getSting(Integer.toString(stingid));
            } else {
                // Something has failed...
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
            e.printStackTrace();
        }

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(DELETE_STING_QUERY);
            stmt.setInt(1, Integer.valueOf(stingid));

            int rows = stmt.executeUpdate();
            if (rows == 0)
                ;// Deleting inexistent sting
        } catch (SQLException e) {
            e.printStackTrace();
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
        Connection conn = null;
        try {
            conn = Database.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(UPDATE_STING_QUERY);
            stmt.setString(1, sting.getSubject());
            stmt.setString(2, sting.getContent());
            stmt.setInt(3, Integer.valueOf(stingid));

            int rows = stmt.executeUpdate();
            if (rows == 1)
                sting = getSting(stingid);
            else {
                ;// Updating inexistent sting
            }

        } catch (SQLException e) {
            e.printStackTrace();
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
}
