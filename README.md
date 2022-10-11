# SmartScanner Core

ID scanning Android app and library. Supports MRZ, NFC, Barcodes, and [ID PASS Lite](https://github.com/idpass/idpass-lite) cards.

[![Get it on Google Play](https://buttercup.pw/static/img/googleplay.svg)](https://play.google.com/store/apps/details?id=org.newlogic.smartscanner)

**Note: The library's API might keep evolving before we reach v1.0, so be careful when upgrading between these pre-v1.0 versions. Starting at v1.0 we will be careful in introducing breaking API changes.**

## Features

- Scan MRZ
- Scan NFC
- Scan Barcode
- Scan [ID PASS Lite](https://github.com/idpass/idpass-lite)
- Supports Intent Call Out (ODK & Non-ODK)

This repository also includes an Android [demo app](app) to test what the library can do.

## Related projects

- [smartscanner-android-api](https://github.com/idpass/smartscanner-android-api) - Provides convenience methods to simplify the SmartScanner intent call out process
- [smartscanner-capacitor](https://github.com/idpass/smartscanner-capacitor) - SmartScanner [Capacitor](https://capacitorjs.com/) plugin
- [smartscanner-cordova](https://github.com/idpass/smartscanner-cordova) - SmartScanner [Cordova](https://cordova.apache.org/) plugin

## Installation

Declare Maven Central repository in the dependency configuration, then add this library in the dependencies. An example using `build.gradle`:

```groovy
repositories {
  mavenCentral()
}

dependencies {
  implementation "org.idpass:idpass-smartscanner:0.0.1-SNAPSHOT"
}
```

If you want to build this library from source, instructions to do so can be found in the [Building from source](https://github.com/idpass/smartscanner-core/wiki/Building-from-source) wiki page.

## Usage

The following table shows the intent actions available for each operation available in the library, both in ODK and Non-ODK:

| Scan Operation |                                            ODK |                                    Non-ODK |
| -------------- | ---------------------------------------------- | ------------------------------------------ |
| MRZ            | `org.idpass.smartscanner.odk.MRZ_SCAN`         | `org.idpass.smartscanner.MRZ_SCAN`         |
| NFC            | `org.idpass.smartscanner.odk.NFC_SCAN`         | `org.idpass.smartscanner.NFC_SCAN`         |
| Barcode        | `org.idpass.smartscanner.odk.BARCODE_SCAN`     | `org.idpass.smartscanner.BARCODE_SCAN`     |
| QR code        | `org.idpass.smartscanner.odk.QRCODE_SCAN`      | `org.idpass.smartscanner.QRCODE_SCAN`      |
| ID PASS Lite   | `org.idpass.smartscanner.odk.IDPASS_LITE_SCAN` | `org.idpass.smartscanner.IDPASS_LITE_SCAN` |

To perform an operation, create an intent for the desired operation. This example shows how to call an intent to scan an MRZ (ODK):

```kotlin
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

  private const val OP_SCANNER = 1001 // Activity request code

  override fun onStart() {
    super.onStart()

    try {
      val action = "org.idpass.smartscanner.odk.MRZ_SCAN"
      val intent = Intent(action)
      startActivityForResult(intent, OP_SCANNER)
    } catch (ex: ActivityNotFoundException) {
      ex.printStackTrace()
    }
  }

  public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
    super.onActivityResult(requestCode, resultCode, intent)

    if (requestCode == OP_SCANNER) {
      if (resultCode == Activity.RESULT_OK) {
        val bundle = intent?.getBundleExtra("result")
      }
    }
  }

}
```

The `bundle` variable is a [`Bundle`](https://developer.android.com/reference/kotlin/android/os/Bundle) object containing the scan results. For example, the scanned surname can be obtained through:

```kotlin
val surname = bundle.getString("surname")
```

Refer to the [Result fields reference](https://github.com/idpass/smartscanner-core/wiki/Result-fields-reference) for the different fields available from the scan results.

Refer to the [API Reference](https://github.com/idpass/smartscanner-core/wiki/API-Reference) for complete information about each scan operation and the different options available.

Finally, for convenience we recommend using the [smartscanner-android-api](https://github.com/idpass/smartscanner-android-api) library which simplifies the app intent call out process.

## Running the demo app

The Android demo app is available in this repository's `app` directory. Open this directory in Android Studio (version 4.1 and above) and the app can be built and run from there.

## License

[Apache-2.0 License](LICENSE)
