# ID PASS SmartScanner
This is the core repository of the ID PASS SmartScanner that provides developers with an API that enables them to scan MRZ, Barcode, and ID PASS Lite cards.

## Setting Up
---------------
```bash
# 1. Clone this repository.
git clone https://github.com/idpass/idpass-smart-scanner-core.git

# 2. Enter your newly-cloned folder.
cd idpass-smart-scanner-core

```

## Building
```bash
./gradlew build
```

## Features
- Scan MRZ
- Scan Barcode
- Scan ID PASS Lite
- Intent Call Out support (ODK/Non-ODK)
- Plugin Integration for [Capacitor](https://github.com/idpass/idpass-smart-scanner-capacitor)/[Cordova](https://github.com/idpass/idpass-smart-scanner-cordova)

## Quickstart
This library is used to scan MRZ, Barcode, and ID PASS Lite cards. This also has an embedded standalone `app` for testing purposes

<!--
### 1. Install
Declare Maven Central repository in the dependency configuration. For example, in `build.gradle`:

```groovy
repositories {
    mavenCentral()
}

dependencies {
    implementation "org.idpass:idpass-smartscanner:0.0.1-SNAPSHOT"
}
```
-->
### 2. Usage
---------------
App Intent Call out

Call scanner using the ff intent actions:
- `"org.idpass.smartscanner.MRZ_SCAN"`
- `"org.idpass.smartscanner.BARCODE_SCAN"`
- `"org.idpass.smartscanner.IDPASS_LITE_CAN"`

ODK Support can be called using the ff intent actions:
- `"org.idpass.smartscanner.odk.MRZ_SCAN"`
- `"org.idpass.smartscanner.odk.BARCODE_SCAN"`
- `"org.idpass.smartscanner.odk.IDPASS_LITE_CAN"`

```kotlin
    // Sample SmartScanner MRZ intent call out
    val OP_SCANNER = 1001 // Activity request code
    try {
        val action = "org.idpass.smartscanner.MRZ_SCAN"
        val intent = Intent(action)
        startActivityForResult(intent, OP_SCANNER)
    } catch (ex: ActivityNotFoundException) {
        ex.printStackTrace()
        Log.e(TAG, "smart scanner is not installed!")
    }

    // Get Result from the ActivityResult via Bundle
        public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == OP_SCANNER) {
            Log.d(TAG, "Plugin post SmartScanner Activity resultCode $resultCode")
            if (resultCode == RESULT_OK) {
                val bundle =intent?.getBundleExtra("result")
            }
        }
    }
```

For convenience, you may use the [smartscanner-android-api](https://github.com/idpass/smartscanner-android-api) which will simplify the app intent call out process.
