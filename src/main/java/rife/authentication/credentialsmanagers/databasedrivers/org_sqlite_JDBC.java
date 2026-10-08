/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.authentication.credentialsmanagers.databasedrivers;

import rife.authentication.credentialsmanagers.DatabaseUsers;
import rife.authentication.credentialsmanagers.RoleUserAttributes;
import rife.authentication.credentialsmanagers.exceptions.*;
import rife.authentication.exceptions.CredentialsManagerException;
import rife.database.Datasource;
import rife.database.exceptions.DatabaseException;
import rife.database.queries.Insert;

import java.sql.SQLException;

public class org_sqlite_JDBC extends generic {
    public org_sqlite_JDBC(Datasource datasource) {
        super(datasource);

        // SQLite generates role IDs for the existing INTEGER PRIMARY KEY.
        addRole_ = new Insert(datasource)
            .into(createTableRole_.getTable())
            .fieldParameter("name");
    }

    public boolean install()
    throws CredentialsManagerException {
        try {
            inTransaction(() -> {
                executeUpdate(createTableRole_);
                executeUpdate(createTableUser_);
                executeUpdate(createTableRoleLink_);
            });
        } catch (DatabaseException e) {
            throw new InstallCredentialsErrorException(e);
        }
        return true;
    }

    public boolean remove()
    throws CredentialsManagerException {
        try {
            inTransaction(() -> {
                executeUpdate(dropTableRoleLink_);
                executeUpdate(dropTableUser_);
                executeUpdate(dropTableRole_);
            });
        } catch (DatabaseException e) {
            throw new RemoveCredentialsErrorException(e);
        }
        return true;
    }

    public DatabaseUsers addRole(String role)
    throws CredentialsManagerException {
        if (role == null || role.isEmpty()) {
            throw new AddRoleErrorException(role);
        }
        try {
            if (executeUpdate(addRole_, s -> s.setString("name", role)) == 0) {
                throw new AddRoleErrorException(role);
            }
        } catch (DatabaseException e) {
            if (isUniqueViolation(e, createTableRole_.getTable(), "name")) {
                throw new DuplicateRoleException(role, e);
            }
            throw new AddRoleErrorException(role, e);
        }
        return this;
    }

    public DatabaseUsers addUser(String login, RoleUserAttributes attributes)
    throws CredentialsManagerException {
        try {
            _addUser(addUserWithId_, getFreeUserId_, getRoleId_, addRoleLink_, login, attributes);
        } catch (CredentialsManagerException e) {
            if (isUniqueViolation(e, createTableUser_.getTable(), "userId")) {
                throw new DuplicateUserIdException(attributes.getUserId(), e);
            }
            if (isUniqueViolation(e, createTableUser_.getTable(), "login")) {
                throw new DuplicateLoginException(login, e);
            }
            throw e;
        }
        return this;
    }

    private boolean isUniqueViolation(Throwable error, String table, String column) {
        for (var cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && (sql.getErrorCode() & 0xff) == 19 &&
                sql.getMessage() != null &&
                sql.getMessage().contains("UNIQUE constraint failed: " + table + "." + column)) {
                return true;
            }
        }
        return false;
    }
}
