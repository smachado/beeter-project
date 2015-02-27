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

package edu.upc.eetac.dsa.smachado.beeter.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created by Sergio Machado on 27/02/15.
 */
public class Database {
    private static Database instance;
    private HikariDataSource ds;

    private Database() {
        HikariConfig config = new HikariConfig(Database.class.getClassLoader().getResource("hikari.properties").getFile());
        //config.addDataSourceProperty("jdbcCompliantTruncation", false);
        ds = new HikariDataSource(config);
    }

    private static Database getInstance() {
        if (instance == null) instance = new Database();
        return instance;
    }

    public final static Connection getConnection() throws SQLException {
        return getInstance().ds.getConnection();
    }
}
