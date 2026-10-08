/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.scheduler.taskmanagers.databasedrivers;

import java.sql.Statement;

import rife.database.*;
import rife.database.exceptions.DatabaseException;
import rife.database.queries.Insert;
import rife.database.queries.Query;
import rife.scheduler.Task;
import rife.scheduler.exceptions.TaskManagerException;
import rife.scheduler.taskmanagers.exceptions.*;

public class org_sqlite_JDBC extends generic {
    public org_sqlite_JDBC(Datasource datasource) {
        super(datasource);

        // INTEGER PRIMARY KEY generates the id when omitted from the insert.
        addTask_ = new Insert(getDatasource())
            .into(createTableTask_.getTable())
            .fieldParameter("type")
            .fieldParameter("planned")
            .fieldParameter("frequency", "frequencySpecification")
            .fieldParameter("busy");
    }

    public boolean install()
    throws TaskManagerException {
        try {
            executeUpdate(createTableTask_);
        } catch (DatabaseException e) {
            throw new InstallTasksErrorException(e);
        }
        return true;
    }

    public boolean remove()
    throws TaskManagerException {
        try {
            executeUpdate(dropTableTask_);
        } catch (DatabaseException e) {
            throw new RemoveTasksErrorException(e);
        }
        return true;
    }

    public int addTask(final Task task)
    throws TaskManagerException {
        if (null == task) throw new IllegalArgumentException("task can't be null.");

        final var id = new int[]{-1};
        try {
            if (0 == executeUpdate(addTask_, new DbPreparedStatementHandler<>() {
                public DbPreparedStatement getPreparedStatement(Query query, DbConnection connection) {
                    return connection.getPreparedStatement(query, Statement.RETURN_GENERATED_KEYS);
                }

                public int performUpdate(DbPreparedStatement statement) {
                    statement.setBean(task);
                    var result = statement.executeUpdate();
                    id[0] = statement.getFirstGeneratedIntKey();
                    return result;
                }
            })) {
                throw new AddTaskErrorException(task);
            }
        } catch (DatabaseException e) {
            throw new AddTaskErrorException(task, e);
        }

        if (id[0] < 0) {
            throw new GetTaskIdErrorException();
        }
        task.setId(id[0]);
        return id[0];
    }
}
