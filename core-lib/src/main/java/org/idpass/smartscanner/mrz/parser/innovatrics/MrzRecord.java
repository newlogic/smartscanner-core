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
package org.idpass.smartscanner.mrz.parser.innovatrics;



import org.idpass.smartscanner.mrz.parser.innovatrics.types.MrzDate;
import org.idpass.smartscanner.mrz.parser.innovatrics.types.MrzDocumentCode;
import org.idpass.smartscanner.mrz.parser.innovatrics.types.MrzFormat;
import org.idpass.smartscanner.mrz.parser.innovatrics.types.MrzSex;

import java.io.Serializable;

/**
 * An abstract MRZ record, contains basic information present in all MRZ record types.
 * @author Martin Vysny
 */
public abstract class MrzRecord implements Serializable {


    /**
     * The document code.
     */
    public MrzDocumentCode code;
    /**
     * Document code, see {@link MrzDocumentCode} for details on allowed values.
     */
    public char code1;
    /**
     * For MRTD: Type, at discretion of states, but 1-2 should be IP for passport card, AC for crew member and IV is not allowed.
     * For MRP: Type (for countries that distinguish between different types of passports)
     */
    public char code2;


    /**
     * An <a href="http://en.wikipedia.org/wiki/ISO_3166-1_alpha-3">ISO 3166-1 alpha-3</a> country code of issuing country, with additional allowed values (according to <a href="http://en.wikipedia.org/wiki/Machine-readable_passport">article on Wikipedia</a>):
     * <ul><li>D: Germany</li>
    <li>GBD: British dependent territories citizen(note: the country code of the overseas territory is presently used to indicate issuing authority and nationality of BOTC)</li>
    <li>GBN: British National (Overseas)</li>
    <li>GBO: British Overseas citizen</li>
    <li>GBP: British protected person</li>
    <li>GBS: British subject</li>
    <li>UNA: specialized agency of the United Nations</li>
    <li>UNK: resident of Kosovo to whom a travel document has been issued by the United Nations Interim Administration Mission in Kosovo (UNMIK)</li>
    <li>UNO: United Nations Organization</li>
    <li>XOM: Sovereign Military Order of Malta</li>
    <li>XXA: stateless person, as per the 1954 Convention Relating to the Status of Stateless Persons</li>
    <li>XXB: refugee, as per the 1951 Convention Relating to the Status of Refugees</li>
    <li>XXC: refugee, other than defined above</li>
    <li>XXX: unspecified nationality</li></ul>
     */
    public String issuingCountry;
    /**
     * Document number, e.g. passport number.
     */
    public String documentNumber;
    /**
     * The surname in uppercase.
     */
    public String surname;
    /**
     * The given names in uppercase, separated by spaces.
     */
    public String givenNames;
    /**
     * Date of birth.
     */
    public MrzDate dateOfBirth;
    /**
     * Sex
     */
    public MrzSex sex;
    /**
     * expiration date of passport
     */
    public MrzDate expirationDate;
    /**
     * An <a href="http://en.wikipedia.org/wiki/ISO_3166-1_alpha-3">ISO 3166-1 alpha-3</a> country code of nationality.
     * See {@link #issuingCountry} for additional allowed values.
     */
    public String nationality;
    /**
     * Detected MRZ format.
     */
    public final MrzFormat format;


    /**
     * check digits, usually common in every document.
     */
    public boolean validDocumentNumber = true;
    public boolean validDateOfBirth = true;
    public boolean validExpirationDate = true;
    public boolean validComposite = true;


    protected MrzRecord(MrzFormat format) {
        this.format = format;
    }

    @Override
    public String toString() {
        return "MrzRecord{" + "code=" + code + "[" + code1 + code2 + "], issuingCountry=" + issuingCountry + ", documentNumber=" + documentNumber
                + ", surname=" + surname + ", givenNames=" + givenNames + ", dateOfBirth=" + dateOfBirth + ", sex=" + sex + ", expirationDate="
                + expirationDate + ", nationality=" + nationality + '}';
    }

    /**
     * Parses the MRZ record.
     * @param mrz the mrz record, not null, separated by \n
     * @throws MrzParseException when a problem occurs.
     */
    public void fromMrz(String mrz) throws MrzParseException {
        if (format != MrzFormat.get(mrz)) {
            throw new MrzParseException("invalid format: " + MrzFormat.get(mrz), mrz, new MrzRange(0, 0, 0), format);
        }
        code = MrzDocumentCode.parse(mrz);
        code1 = mrz.charAt(0);
        code2 = mrz.charAt(1);
        issuingCountry = new MrzParser(mrz).parseString(new MrzRange(2, 5, 0));
    }
    
    /**
     * Helper method to set the full name. Changes both {@link #surname} and {@link #givenNames}.
     * @param name expected array of length 2, in the form of [surname, first_name]. Must not be null.
     */
    protected final void setName(String[] name) {
        surname = name[0];
        givenNames = name[1];
    }
    
    /**
     * Serializes this record to a valid MRZ record.
     * @return a valid MRZ record, not null, separated by \n
     */
    public abstract String toMrz();
}