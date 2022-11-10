package org.idpass.smartscanner.lib.barcode.pdf417;


import com.google.zxing.BarcodeFormat;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.journeyapps.barcodescanner.Decoder;
import com.journeyapps.barcodescanner.DecoderFactory;
import com.journeyapps.barcodescanner.InvertedDecoder;
import com.journeyapps.barcodescanner.MixedDecoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class PDF417DecoderFactory implements DecoderFactory {

    private final Collection<BarcodeFormat> decodeFormats;
    private Map<DecodeHintType, ?> hints;
    private String characterSet;
    private final int scanType;

    public PDF417DecoderFactory() {
        this.decodeFormats = new ArrayList<BarcodeFormat>(){{
            add(BarcodeFormat.PDF_417);
        }};
        this.scanType = 2;
    }

    public PDF417DecoderFactory(Map<DecodeHintType, ?> hints, String characterSet, int scanType) {
        this.decodeFormats =  new ArrayList<BarcodeFormat>(){{
            add(BarcodeFormat.PDF_417);
        }};
        this.hints = hints;
        this.characterSet = characterSet;
        this.scanType = scanType;
    }
    @Override
    public Decoder createDecoder(Map<DecodeHintType, ?> baseHints) {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);

        hints.putAll(baseHints);

        if (this.hints != null) {
            hints.putAll(this.hints);
        }

        if (this.decodeFormats != null) {
            hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
        }

        if (characterSet != null) {
            hints.put(DecodeHintType.CHARACTER_SET, characterSet);
        }

        MultiFormatReader reader = new MultiFormatReader();
        reader.setHints(hints);

        switch (scanType){
            case 1:
                return new InvertedDecoder(reader);
            case 2:
                return new MixedDecoder(reader);
            default:
                return new Decoder(reader);
        }
    }
}
