/*
 * This file is part of the SCUBA smart card framework.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 *
 * Copyright (C) 2009 - 2018  The SCUBA team.
 *
 * $Id: Gender.java 314 2018-05-25 13:42:16Z martijno $
 */
package org.idpass.smartscanner.lib.nfc.scuba;


/**
 * Possible values for a person's gender.
 * Integer values correspond to Section 5.5.3 of ISO 19794-5.
 *
 * @author Martijn Oostdijk (martijn.oostdijk@gmail.com)
 * @author Cees-Bart Breunesse (ceesb@riscure.com)
 *
 * @version $Revision: 314 $
 */
public enum Gender {

    MALE { @Override
    public int toInt() {
        return 0x01;
    }
    },

    FEMALE {@Override
    public int toInt() {
        return 0x02;
    }
    },

    UNKNOWN { @Override
    public int toInt() {
        return 0xFF;
    }
    },

    UNSPECIFIED {@Override
    public int toInt() {
        return 0x00;
    }
    };

    /**
     * Gets the numerical code of this gender.
     *
     * @return the numerical code
     */
    public abstract int toInt();

    /**
     * Gets a gender object given a code.
     *
     * @param code the numerical code
     *
     * @return a gender
     */
    public static Gender getInstance(int code) {
        for(Gender g: Gender.values()) {
            if (g.toInt() == code) {
                return g;
            }
        }
        return UNKNOWN;
    }
}
