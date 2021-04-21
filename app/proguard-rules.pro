# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * implements java.io.Serializable
-keep public class * extends androidx.fragment.*

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepattributes InnerClasses
-keep class org.idpass.smartscanner.lib.barcode.BarcodeResult
-keepclassmembers class org.idpass.smartscanner.lib.barcode.BarcodeResult** {
    *;
}

-keep class org.idpass.smartscanner.lib.idpasslite.IDPassLiteResult
-keepclassmembers class org.idpass.smartscanner.lib.idpasslite.IDPassLiteResultt** {
    *;
}

-keep class org.idpass.smartscanner.lib.mrz.MRZResult
-keepclassmembers class org.idpass.smartscanner.lib.mrz.MRZResult** {
    *;
}

-keep class org.idpass.smartscanner.lib.nfc.NFCResult
-keepclassmembers class org.idpass.smartscanner.lib.nfc.NFCResult** {
    *;
}

-keep public class org.idpass.lite.* {
  <fields>;
  <methods>;
}
-dontwarn org.idpass.lite.*
-keepattributes Exceptions, Signature, InnerClasses

-keep public class org.idpass.smartscanner.* {
  <fields>;
  <methods>;
}
-dontwarn org.idpass.smartscanner.*
-keepattributes Exceptions, Signature, InnerClasses
-keep class org.idpass.smartscanner.lib.scanner.config.ScannerOptions**
-keepclassmembers class org.idpass.smartscanner.lib.scanner.config.ScannerOptions** {
    *;
}

-keep public class org.jmrtd.* {
  <fields>;
  <methods>;
}
-dontwarn org.jmrtd.*
-keepattributes Exceptions, Signature, InnerClasses
-keep class org.jmrtd.JMRTDSecurityProvider**
-keepclassmembers class org.jmrtd.JMRTDSecurityProvider** {
    *;
}

-keep public class org.spongycastle.* {
  <fields>;
  <methods>;
}
-dontwarn org.spongycastle.*
-keepattributes Exceptions, Signature, InnerClasses

-keep public class net.sf.scuba.* {
  <fields>;
  <methods>;
  *;
}
-dontwarn net.sf.scuba.*
-keepattributes Exceptions, Signature, InnerClasses

-keep class net.sf.scuba.smartcards.IsoDepCardService**
-keepclassmembers class net.sf.scuba.smartcards.IsoDepCardService** {
    *;
}

-keep public class org.ejbca.* {
  <fields>;
  <methods>;
}
-dontwarn org.ejbca.*
-keepattributes Exceptions, Signature, InnerClasses

-keep public class org.slf4j.* {
  <fields>;
  <methods>;
}
-dontwarn org.slf4j.*
-keepattributes Exceptions, Signature, InnerClasses

-keep public class cz.adaptech.android.* {
  <fields>;
  <methods>;
}
-dontwarn cz.adaptech.android.*
-keepattributes Exceptions, Signature, InnerClasses

-keep public class com.google.protobuf.* {
  <fields>;
  <methods>;
}
-dontwarn com.google.protobuf.*
-keepattributes Exceptions, Signature, InnerClasses

-keep public class com.google.gson.* {
  <fields>;
  <methods>;
}
-dontwarn com.google.gson.*
-keepattributes Exceptions, Signature, InnerClasses

-keep public class com.bumptech.glide.* {
  <fields>;
  <methods>;
}
-dontwarn com.bumptech.glide.*
-keepattributes Exceptions, Signature, InnerClasses

-keep public class jp.wasabeef.glide.* {
  <fields>;
  <methods>;
}
-dontwarn jp.wasabeef.glide.*
-keepattributes Exceptions, Signature, InnerClasses

-keep public class com.jayway.jsonpath.* {
  <fields>;
  <methods>;
}
-dontwarn com.jayway.jsonpath.*
-keepattributes Exceptions, Signature, InnerClasses

-keep public class com.github.wnameless* {
  <fields>;
  <methods>;
}
-dontwarn com.github.wnameless*
-keepattributes Exceptions, Signature, InnerClasses

-keep public class org.apache.* {
  <fields>;
  <methods>;
}
-dontwarn org.apache.*
-keepattributes Exceptions, Signature, InnerClasses

-keep public class com.google.common.* {
  <fields>;
  <methods>;
}
-dontwarn com.google.common.*
-keepattributes Exceptions, Signature, InnerClasses

-ignorewarnings

-keepclasseswithmembernames class * {
    native <methods>;
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}

-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

-keepclassmembers class * extends android.app.Activity {
   public void *(android.view.View);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}