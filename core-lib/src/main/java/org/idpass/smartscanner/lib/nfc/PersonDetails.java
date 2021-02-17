/*
 * Copyright 2016 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
package org.idpass.smartscanner.lib.nfc;

import android.graphics.Bitmap;

import java.util.List;

public class PersonDetails {

    private String name;
    private String surname;
    private String personalNumber;
    private String gender;
    private String birthDate;
    private String expiryDate;
    private String serialNumber;
    private String nationality;
    private String issuerAuthority;
    private Bitmap faceImage;
    private String faceImageBase64;
    private Bitmap portraitImage;
    private String portraitImageBase64;
    private Bitmap signature;
    private String signatureBase64;
    private List<Bitmap> fingerprints;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPersonalNumber() {
        return personalNumber;
    }

    public void setPersonalNumber(String personalNumber) {
        this.personalNumber = personalNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getIssuerAuthority() {
        return issuerAuthority;
    }

    public void setIssuerAuthority(String issuerAuthority) {
        this.issuerAuthority = issuerAuthority;
    }

    public Bitmap getFaceImage() {
        return faceImage;
    }

    public void setFaceImage(Bitmap faceImage) {
        this.faceImage = faceImage;
    }

    public String getFaceImageBase64() {
        return faceImageBase64;
    }

    public void setFaceImageBase64(String faceImageBase64) {
        this.faceImageBase64 = faceImageBase64;
    }

    public Bitmap getPortraitImage() {
        return portraitImage;
    }

    public void setPortraitImage(Bitmap portraitImage) {
        this.portraitImage = portraitImage;
    }

    public String getPortraitImageBase64() {
        return portraitImageBase64;
    }

    public void setPortraitImageBase64(String portraitImageBase64) {
        this.portraitImageBase64 = portraitImageBase64;
    }

    public Bitmap getSignature() {
        return signature;
    }

    public void setSignature(Bitmap signature) {
        this.signature = signature;
    }

    public String getSignatureBase64() {
        return signatureBase64;
    }

    public void setSignatureBase64(String signatureBase64) {
        this.signatureBase64 = signatureBase64;
    }

    public List<Bitmap> getFingerprints() {
        return fingerprints;
    }

    public void setFingerprints(List<Bitmap> fingerprints) {
        this.fingerprints = fingerprints;
    }
}