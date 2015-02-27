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

drop database if exists beeterdb;
create database beeterdb;
 
use beeterdb;
 
create table users (
username varchar(20) not null primary key,
userpass char(32) not null,
name varchar(70) not null,
email varchar(255) not null
);
 
create table user_roles (
username varchar(20) not null,
rolename varchar(20) not null,
foreign key(username) references users(username) on delete cascade,
primary key (username, rolename)
);
 
create table stings (
stingid int not null auto_increment primary key,
username varchar(20) not null,
subject varchar(100) not null,
content varchar(500) not null,
last_modified timestamp default current_timestamp ON UPDATE CURRENT_TIMESTAMP,
creation_timestamp datetime not null default current_timestamp,
foreign key(username) references users(username)
);