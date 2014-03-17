/*
 * SymmetricDS is an open source database synchronization solution.
 *   
 * Copyright (C) Chris Henson <chenson42@users.sourceforge.net>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 */

package org.jumpmind.symmetric.model;

import org.jumpmind.symmetric.util.ICoded;

public enum DataEventType implements ICoded {

    /**
     * Insert DML type.
     */
    INSERT("I"),

    /**
     * Update DML type.
     */
    UPDATE("U"),

    /**
     * Delete DML type.
     */
    DELETE("D"),

    /**
     * An event that indicates that a table needs to be reloaded.
     */
    RELOAD("R"),

    /**
     * An event that indicates that the data payload has a sql statement that needs to be executed. This is more of a
     * remote control feature (that would have been very handy in past lives).
     */
    SQL("S"),

    /**
     * An event that indicates that the data payload is a table creation.
     */
    CREATE("C"),

    /**
     * An event that indicates that all SymmetricDS configuration table data should be streamed to the client.
     */
    CONFIG("X"),

    /**
     * An event the indicates that the data payload is going to be a Java bean shell script that is to be run at the
     * client.
     */
    BSH("B");

    private String code;

    DataEventType(String code) {
        this.code = code;
    }

    public String getCode() {
        return this.code;
    }

    public static DataEventType getEventType(String s) {
        if (s.equals(INSERT.getCode())) {
            return INSERT;
        } else if (s.equals(UPDATE.getCode())) {
            return UPDATE;
        } else if (s.equals(DELETE.getCode())) {
            return DELETE;
        } else if (s.equals(RELOAD.getCode())) {
            return RELOAD;
        } else if (s.equals(SQL.getCode())) {
            return SQL;
        } else if (s.equals(CREATE.getCode())) {
            return CREATE;
        } else if (s.equals(CONFIG.getCode())) {
            return CONFIG;
        } else if (s.equals(RELOAD.getCode())) {
            return RELOAD;
        } else if (s.equals(BSH.getCode())) {
            return BSH;
        } else {
            throw new IllegalStateException(String.format("Invalid data event type of %s", s));
        }
    }
}