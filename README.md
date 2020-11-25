# idpass-smart-scanner-core
The repository for the core Library to be used with the Base project of the ID Pass smart scanner 

## Setting Up
---------------
```bash
# 1. Clone this repository.
git clone https://github.com/newlogic/idpass-smart-scanner-core.git

# 2. Enter your newly-cloned folder.
cd idpass-smart-scanner-core

# 3. Init submodules - used in working (capacitor/cordova/etc) plugins
git submodule update --init --recursive

```
Note: Please do not forget to initialize submodules to be able to access plugin repos.

## Structure
---------------
Main structure items:

    ├─ core
    ├─ cordova
    ├─ capacitor

- **core** - contains the core library and demo android app for testing purposes
- **cordova** - submodule repo of cordova plugin
- **capacitor** - submodule repo of capacitor plugin

## Scanner Options
---------------
**mode** 
- the scanner is able to scan Barcodes or MRZ and is accessed by setting to either `barcode` or `mrz`
```
mode: 'mrz' 
```
**mrzFormat** 
- when mode is set to `mrz`, mrzFormat is able to be accessed and set by either `MRP` (Default) or `MRTD_TD1`. 
- format `MRTD_TD1` is used in retrieving optional data from scanned MRZ
- default is `MRP`
```
mrzFormat: 'MRTD_TD1'
```
**barcodeOptions** 
- When mode is set to `barcode`, barcodeOptions is able to be accessed and set by a list of barcode formats.
- Multiple barcode formats can be supported, see complete list below. 
- Default is `ALL`

Supported Formats:
- `ALL` (Support all formats)
- `AZTEC`
- `CODABAR`
- `CODE_39`
- `CODE_93`
- `CODE_128`
- `DATA_MATRIX`
- `EAN_8`
- `EAN_13`
- `QR_CODE`
- `UPC_A`
- `UPC_E`
- `PDF_417`

```
  barcodeOptions: { 
    barcodeFormats: [ `AZTEC`, 
                      `CODABAR`, 
		      `CODE_39`, 
		      `CODE_93`, 
		      `CODE_128`, 
		      `DATA_MATRIX`,
		      'EAN_8',
		      'EAN_13',
		      `QR_CODE`,
		      `UPC_A`,
		      `UPC_E`,
		      `PDF_417`
		  ] 
	       }
```

Config
---------------
**background**
- accepts hex color values, when empty or not set
- default is gray in hex value `#44000000`
```
background: '#89837c'
```
**branding** 
- displays ID Pass branding, set to either `true` or `false`
- default is `true`
```
branding: false`
```
**font** 
- currently supports 2 fonts only, NOTO_SANS_ARABIC (Arabic) and SOURCE_SANS_PRO (ID Pass font), default is SOURCE_SANS_PRO when empty or not set
- default is `SOURCE_SANS_PRO`
```
font: 'NOTO_SANS_ARABIC'
```
**imageResultType**
- currently supports 2 image result types: `base_64` or `path` (path of image string)
- default is `path`
```
imageResultType: 'base_64'
```
**isManualCapture** - enables manual capture mrz/barcode via capture button when not detected, set to either `true` or `false`
- default is `false`
```
isManualCapture: true
```
**label** - will show a label text below the scanner, default is empty
- default is empty string
```
label: 'sample label string'
```

## Plugin call (Capacitor/Cordova)
---------------
MRZ:
```
const result = await MLKitPlugin.executeScanner({
          action: 'START_SCANNER',
          options: {
            mode: 'mrz',
            mrzFormat: 'MRTD_TD1',
            config: {
              background: '#89837c',
              branding: false,
              isManualCapture: true
            }
          }
      });
```
BARCODE:
```
const result = await MLKitPlugin.executeScanner({
        action: 'START_MLKIT',
	options: {
          mode: 'barcode',
          barcodeOptions: { 
               barcodeFormats: [ 
	              `AZTEC`, 
                      `CODABAR`, 
		      `CODE_39`, 
		      `CODE_93`, 
		      `CODE_128`, 
		      `DATA_MATRIX`,
		      'EAN_8',
		      'EAN_13',
		      `QR_CODE`,
		      `UPC_A`,
		      `UPC_E`,
		      `PDF_417`
		 ] 
	  },
          config: {
            background: '#ffc234', // default transparent gray if empty, will accept hex color values only
            label: 'sample label'
          }
	}
      });
```

## Intent App Call Out
---------------
Smart Scanner is also able to be called from another app by calling this code block directly

- Call it via intent `"com.newlogic.mlkitlib.SCAN"`
- Add scanner options json object

Call via MRZ:
```
    val config = Config(background ="",font= "", label="", imageResultType = "path", isManualCapture = true, branding = true)
    val scannerOptions = ScannerOptions(mode = "mrz" config = config, mrzFormat = mrzFormat)
    private fun startIntentCallOut() {
        try {
            val intent = Intent("com.newlogic.idpass.SCAN")
            intent.putExtra("scanner_options", scannerOptions)
            startActivityForResult(intent, OP_MLKIT)
        } catch (ex: ActivityNotFoundException) {
            ex.printStackTrace()
            Log.e(TAG, "smart scanner is not installed!")
        }
    }
```

Call via barcode:
```
    val config = Config(background ="",font= "", label="", imageResultType = "path", isManualCapture = true, branding = true)
    val scannerOptions = ScannerOptions(mode = "barcode" config = config, barcodeOptions = listOf("QR_CODE"))
    private fun startIntentCallOut() {
        try {
            val intent = Intent("com.newlogic.mlkitlib.SCAN")
            intent.putExtra("scanner_options", scannerOptions)
            startActivityForResult(intent, OP_MLKIT)
        } catch (ex: ActivityNotFoundException) {
            ex.printStackTrace()
            Log.e(TAG, "smart scanner is not installed!")
        }
    }
```

## Scan Results
---------------
**MRZ** 
```
{
	"code": "TypeI",
	"code1": 73,
	"code2": 68,
	"dateOfBirth": "30/9/79",
	"documentNumber": "AB1234567",
	"expirationDate": "8/9/29",
	"format": "MRTD_TD1",
	"givenNames": " SALI",
	"image": "/data/user/0/com.newlogic.mlkit.demo/cache/Scanner-20201123103638.jpg",
	"issuingCountry": "IRQ",
	"mrz": "IDIRQAB12345671180000000002\u003c\u003c\u003c\n7909308M2909082IRQ\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c7\n\u003c\u003cSALI\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\u003c\n",
	"nationality": "IRQ",
	"sex": "Male",
	"surname": ""
}
```
**Barcode** 
```
{
	"corners": "65,-46 314,-14 306,171 65,141 ",
	"imagePath": "/data/user/0/com.newlogic.mlkit.demo/cache/Scanner-20201123103911.jpg",
	"value": "036000291452"
}
```
