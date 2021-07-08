/*
 * Java parser for the MRZ records, as specified by the ICAO organization.
 * Copyright (C) 2011 Innovatrics s.r.o.
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.idpass.smartscanner.mrz.parser.innovatrics.types;


import android.util.Log;

import org.idpass.smartscanner.mrz.parser.innovatrics.MrzParseException;
import org.idpass.smartscanner.mrz.parser.innovatrics.MrzRange;
import org.idpass.smartscanner.mrz.parser.innovatrics.MrzRecord;
import org.idpass.smartscanner.mrz.parser.innovatrics.records.MRP;
import org.idpass.smartscanner.mrz.parser.innovatrics.records.MrtdTd1;
import org.idpass.smartscanner.mrz.parser.innovatrics.records.MrtdTd2;
import org.idpass.smartscanner.mrz.parser.innovatrics.records.MrvA;
import org.idpass.smartscanner.mrz.parser.innovatrics.records.MrvB;
import org.idpass.smartscanner.mrz.parser.innovatrics.records.countries.French_ID;
import org.idpass.smartscanner.mrz.parser.innovatrics.records.countries.Iraq_ID;
import org.idpass.smartscanner.mrz.parser.innovatrics.records.countries.Senegal_ID;
import org.idpass.smartscanner.mrz.parser.innovatrics.records.countries.Slovak_ID_2_34;

import java.util.Arrays;

/**
 * Lists all supported MRZ formats. Note that the order of the enum constants are important, see for example {@link  #FRENCH_ID}.
 * @author Martin Vysny, Pierrick Martin
 */
public enum MrzFormat {
    /**
     * Senegal 3 line/30 characters per line format.
     * Need to occur before the {@link #MRTD_TD1} enum constant because of the same values for row/column.
     * See below for the "if" test.
     */
    SENEGAL_ID(3, 30, Senegal_ID.class) {

        public boolean isFormatOf(String[] mrzRows) {
            if (!super.isFormatOf(mrzRows)) {
                return false;
            }
            return mrzRows[0].startsWith("I<SEN");
        }
    },
    /**
     * MRTD td1 format: A three line long, 30 characters per line format.
     */
    MRTD_TD1(3, 30, MrtdTd1.class),
    /**
     * IRAQ ID: A three line long, 30 characters per line format.
     */
    IRAQ_ID(3, 30, Iraq_ID.class){

        public boolean isFormatOf(String[] mrzRows) {
            if (!super.isFormatOf(mrzRows)) {
                return false;
            }
            return mrzRows[0].startsWith("IDIRQ");
        }
    },
    /**
     * French 2 line/36 characters per line format, used with French ID cards.
     * Need to occur before the {@link #MRTD_TD2} enum constant because of the same values for row/column.
     * See below for the "if" test.
     */
    FRENCH_ID(2, 36, French_ID.class) {

        public boolean isFormatOf(String[] mrzRows) {
            if (!super.isFormatOf(mrzRows)) {
                return false;
            }
            return mrzRows[0].startsWith("IDFRA");
        }
    },
    /**
     * MRV type-B format: A two lines long, 36 characters per line format.
     * Need to occur before the {@link #MRTD_TD2} enum constant because of the same values for row/column.
     * See below for the "if" test.
     */
    MRV_VISA_B(2, 36, MrvB.class) {

        public boolean isFormatOf(String[] mrzRows) {
            if (!super.isFormatOf(mrzRows)) {
                return false;
            }
            return mrzRows[0].startsWith("V");
        } 
    },
    /**
     * MRTD td2 format: A two line long, 36 characters per line format.
     */
    MRTD_TD2(2, 36, MrtdTd2.class),
    /**
     * MRV type-A format: A two lines long, 44 characters per line format
     * Need to occur before {@link #PASSPORT} constant because of the same values for row/column.
     * See below for the "if" test.
     */
    MRV_VISA_A(2, 44, MrvA.class) {

        public boolean isFormatOf(String[] mrzRows) {
            if (!super.isFormatOf(mrzRows)) {
                return false;
            }
            return mrzRows[0].startsWith("V");
        }
    },
    /**
     * MRP Passport format: A two line long, 44 characters per line format.
     */
    PASSPORT(2, 44, MRP.class),
    /**
     * Unknown 2 line/34 characters per line format, used with old Slovak ID cards.
     */
    SLOVAK_ID_234(2, 34, Slovak_ID_2_34.class);
    public final int rows;
    public final int columns;
    private final Class<? extends MrzRecord> recordClass;

    MrzFormat(int rows, int columns, Class<? extends MrzRecord> recordClass) {
        this.rows = rows;
        this.columns = columns;
        this.recordClass = recordClass;
    }

    /**
     * Checks if this format is able to parse given serialized MRZ record.
     * @param mrzRows MRZ record, separated into rows.
     * @return true if given MRZ record is of this type, false otherwise.
     */
    public boolean isFormatOf(String[] mrzRows) {
        return rows == mrzRows.length && columns == mrzRows[0].length();
    }

    /**
     * Detects given MRZ format.
     * @param mrz the MRZ string.
     * @return the format, never null.
     */
    public static MrzFormat get(String mrz) {
        final int dummyRow = 44;
        String[] rows = mrz.split("\n");
        final int cols = rows[0].length();
        Log.d("SmartScanner", "mrz: " + mrz);
        Log.d("SmartScanner", "rows: " + Arrays.toString(rows));
        StringBuilder mrzBuilder = new StringBuilder(mrz);
        for (int i = 1; i < rows.length; i++) {
            if (rows[i].length() != cols) {
                //throw new MrzParseException("Different row lengths: 0: " + cols + " and " + i + ": " + rows[i].length(), mrz, new MrzRange(0, 0, 0), null);
                if (rows[i].length() != dummyRow) {
                    mrzBuilder.append("<");
                }
            }
        }
        mrz = mrzBuilder.toString();
        rows = mrz.split("\n");
        Log.d("SmartScanner", "mrz append: " + mrz);
        Log.d("SmartScanner", "rows append: " + Arrays.toString(rows));
        for (final MrzFormat f : values()) {
            if (f.isFormatOf(rows)) {
                return f;
            }
        }
        throw new MrzParseException("Unknown format / unsupported number of cols/rows: " + cols + "/" + rows.length, mrz, new MrzRange(0, 0, 0), null);
    }

    /**
     * Creates new record instance with this type.
     * @return never null record instance.
     */
    public final MrzRecord newRecord() {
        try {
            return recordClass.newInstance();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}