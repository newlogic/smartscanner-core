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
package org.idpass.smartscanner.mrz.parser.innovatrics.records;


import org.idpass.smartscanner.mrz.parser.innovatrics.MrzParser;
import org.idpass.smartscanner.mrz.parser.innovatrics.MrzRange;
import org.idpass.smartscanner.mrz.parser.innovatrics.MrzRecord;
import org.idpass.smartscanner.mrz.parser.innovatrics.types.MrzFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MRTD TD1 format: A three line long, 30 characters per line format.
 * @author Martin Vysny
 */
public class MrtdTd1 extends MrzRecord {
    private static final long serialVersionUID = 1L;

    public MrtdTd1() {
        super(MrzFormat.MRTD_TD1);
    }
    /**
     * Optional data at the discretion
    of the issuing State. May contain
    an extended document number
    as per 6.7, note (j).
     */
    public String optional;
    /**
     * optional (for U.S. passport holders, 21-29 may be corresponding passport number)
     */
    public String optional2;

    @Override
    public void fromMrz(String mrz) {
        Logger log = LoggerFactory.getLogger(MrzParser.class);
        super.fromMrz(mrz);
        final MrzParser p = new MrzParser(mrz);
        documentNumber = p.parseString(new MrzRange(5, 14, 0));
        validDocumentNumber = p.checkDigit(14, 0, new MrzRange(5, 14, 0), "document number");
        optional = p.parseString(new MrzRange(15, 30, 0));
        dateOfBirth = p.parseDate(new MrzRange(0, 6, 1));
        validDateOfBirth = p.checkDigit(6, 1, new MrzRange(0, 6, 1), "date of birth") && dateOfBirth.isDateValid();
        sex = p.parseSex(7, 1);
        expirationDate = p.parseDate(new MrzRange(8, 14, 1));
        validExpirationDate = p.checkDigit(14, 1, new MrzRange(8, 14, 1), "expiration date") && expirationDate.isDateValid();
        nationality = p.parseString(new MrzRange(15, 18, 1));
        optional2 = p.parseString(new MrzRange(18, 29, 1));
        validComposite = p.checkDigit(29, 1, p.rawValue(new MrzRange(5, 30, 0), new MrzRange(0, 7, 1), new MrzRange(8, 15, 1), new MrzRange(18, 29, 1)), "mrz");
        log.debug(p.rawValue(new MrzRange(5, 30, 0), new MrzRange(0, 7, 1), new MrzRange(8, 15, 1), new MrzRange(18, 29, 1)));
        setName(p.parseName(new MrzRange(0, 30, 2)));
    }

    @Override
    public String toString() {
        return "MRTD-TD1{" + super.toString() + ", optional=" + optional + ", optional2=" + optional2 + '}';
    }

    @Override
    public String toMrz() {
        // first line
        final StringBuilder sb = new StringBuilder();
        sb.append(code1);
        sb.append(code2);
        sb.append(MrzParser.toMrz(issuingCountry, 3));
        final String dno = MrzParser.toMrz(documentNumber, 9) + MrzParser.computeCheckDigitChar(MrzParser.toMrz(documentNumber, 9)) + MrzParser.toMrz(optional, 15);
        sb.append(dno);
        sb.append('\n');
        // second line
        final String dob = dateOfBirth.toMrz() + MrzParser.computeCheckDigitChar(dateOfBirth.toMrz());
        sb.append(dob);
        sb.append(sex.mrz);
        final String ed = expirationDate.toMrz() + MrzParser.computeCheckDigitChar(expirationDate.toMrz());
        sb.append(ed);
        sb.append(MrzParser.toMrz(nationality, 3));
        sb.append(MrzParser.toMrz(optional2, 11));
        sb.append(MrzParser.computeCheckDigitChar(dno + dob + ed + MrzParser.toMrz(optional2, 11)));
        sb.append('\n');
        // third line
        sb.append(MrzParser.nameToMrz(surname, givenNames, 30));
        sb.append('\n');
        return sb.toString();
    }
}