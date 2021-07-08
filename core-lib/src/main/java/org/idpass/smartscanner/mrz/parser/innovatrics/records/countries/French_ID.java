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
package org.idpass.smartscanner.mrz.parser.innovatrics.records.countries;


import org.idpass.smartscanner.mrz.parser.innovatrics.MrzParser;
import org.idpass.smartscanner.mrz.parser.innovatrics.MrzRange;
import org.idpass.smartscanner.mrz.parser.innovatrics.MrzRecord;
import org.idpass.smartscanner.mrz.parser.innovatrics.types.MrzDocumentCode;
import org.idpass.smartscanner.mrz.parser.innovatrics.types.MrzFormat;

/**
 * Format used for French ID Cards.
 * <p/>
 * The structure of the card:
 * 2 lines of 36 characters :
<pre>First line : IDFRA{name}{many < to complete line}{6 numbers unknown}
Second line : {card number on 12 numbers}{Check digit}{given names separated by "<<" and maybe troncated if too long}{date of birth YYMMDD}{Check digit}{sex M/F}{1 number checksum}</pre>
 * @author Pierrick Martin, Marin Moulinier
 */
public class French_ID extends MrzRecord {

    private static final long serialVersionUID = 1L;

    public French_ID() {
        super(MrzFormat.FRENCH_ID);
        code = MrzDocumentCode.TypeI;
        code1 = 'I';
        code2 = 'D';
    }
    /**
     * For use of the issuing State or 
    organization.
     */
    public String optional;

    @Override
    public void fromMrz(String mrz) {
        super.fromMrz(mrz);
        final MrzParser p = new MrzParser(mrz);
        //Special because surname and firstname not on the same line
        String[] name = new String[]{"", ""};
        name[0] = p.parseString(new MrzRange(5, 30, 0));
        name[1] = p.parseString(new MrzRange(13, 27, 1));
        setName(name);
        nationality = p.parseString(new MrzRange(2, 5, 0));
        optional = p.parseString(new MrzRange(30, 36, 0));
        documentNumber = p.parseString(new MrzRange(0, 12, 1));
        validDocumentNumber = p.checkDigit(12, 1, new MrzRange(0, 12, 1), "document number");
        dateOfBirth = p.parseDate(new MrzRange(27, 33, 1));
        validDateOfBirth = p.checkDigit(33, 1, new MrzRange(27, 33, 1), "date of birth") && dateOfBirth.isDateValid();
        sex = p.parseSex(34, 1);
        final String finalChecksum = mrz.replace("\n","").substring(0, 36 + 35);
        validComposite = p.checkDigit(35, 1, finalChecksum, "final checksum");
        // TODO expirationDate is missing
    }

    @Override
    public String toString() {
        return "French_ID{" + super.toString() + ", optional=" + optional + '}';
    }

    @Override
    public String toMrz() {
        final StringBuilder sb = new StringBuilder("IDFRA");
        // first row
        sb.append(MrzParser.toMrz(surname, 25));
        sb.append(MrzParser.toMrz(optional, 6));
        sb.append('\n');
        // second row
        sb.append(MrzParser.toMrz(documentNumber, 12));
        sb.append(MrzParser.computeCheckDigitChar(MrzParser.toMrz(documentNumber, 12)));
        sb.append(MrzParser.toMrz(givenNames, 14));
        sb.append(dateOfBirth.toMrz());
        sb.append(MrzParser.computeCheckDigitChar(dateOfBirth.toMrz()));
        sb.append(sex.mrz);
        sb.append(MrzParser.computeCheckDigitChar(sb.toString().replace("\n","")));
        sb.append('\n');
        return sb.toString();
    }
}