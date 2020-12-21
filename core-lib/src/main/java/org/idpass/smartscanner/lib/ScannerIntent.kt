package org.idpass.smartscanner.lib

import android.content.Intent

/*
* TODO Remove when idpass-smarscanner-api is available in maven
* */
class ScannerIntent {

    companion object {
        @JvmStatic
        fun intentMrz(): Intent {
            val intent = Intent(ScannerConstants.IDPASS_SMARTSCANNER_INTENT)
            intent.putExtra(ScannerConstants.SCANNER, ScannerConstants.MRZ)
            return intent
        }

        @JvmStatic
        fun intentBarcode(): Intent {
            val intent = Intent(ScannerConstants.IDPASS_SMARTSCANNER_INTENT)
            intent.putExtra(ScannerConstants.SCANNER, ScannerConstants.BARCODE)
            return intent
        }

        @JvmStatic
        fun intentIDPassLite(): Intent {
            val intent = Intent(ScannerConstants.IDPASS_SMARTSCANNER_INTENT)
            intent.putExtra(ScannerConstants.SCANNER, ScannerConstants.IDPASS_LITE)
            return intent
        }
    }
}