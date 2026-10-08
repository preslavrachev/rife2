/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.scheduler.taskoptionmanagers.databasedrivers;

import java.sql.SQLException;

import rife.database.Datasource;
import rife.scheduler.TaskOption;
import rife.scheduler.exceptions.TaskOptionManagerException;
import rife.scheduler.taskoptionmanagers.exceptions.DuplicateTaskOptionException;
import rife.scheduler.taskoptionmanagers.exceptions.InexistentTaskIdException;

public class org_sqlite_JDBC extends generic {
    public org_sqlite_JDBC(Datasource datasource) {
        super(datasource);
    }

    public boolean addTaskOption(final TaskOption taskOption)
    throws TaskOptionManagerException {
        try {
            return super.addTaskOption(taskOption);
        } catch (TaskOptionManagerException e) {
            throw translateConstraint(taskOption, e);
        }
    }

    public boolean updateTaskOption(final TaskOption taskOption)
    throws TaskOptionManagerException {
        try {
            return super.updateTaskOption(taskOption);
        } catch (TaskOptionManagerException e) {
            throw translateConstraint(taskOption, e);
        }
    }

    private TaskOptionManagerException translateConstraint(TaskOption taskOption, TaskOptionManagerException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && (sql.getErrorCode() & 0xff) == 19) {
                var message = sql.getMessage();
                if (message != null && message.contains("FOREIGN KEY constraint failed")) {
                    return new InexistentTaskIdException(taskOption.getTaskId(), exception);
                }
                if (message != null && message.contains("UNIQUE constraint failed: " + createTableTaskOption_.getTable() + ".task_id")) {
                    return new DuplicateTaskOptionException(taskOption.getTaskId(), taskOption.getName(), exception);
                }
            }
        }
        return exception;
    }
}
