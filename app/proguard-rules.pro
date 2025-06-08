# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Google Play Services
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**
-keep class * extends com.google.android.gms.internal.firebase.** { *; }

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception
-keep class com.google.firebase.messaging.** { *; }

# Razorpay
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes JavascriptInterface
-keepattributes *Annotation*
-dontwarn com.razorpay.**
-keep class com.razorpay.** {*;}
-optimizations !method/inlining/*

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Razorpay ProGuard rules
-keepclassmembers class * implements com.razorpay.PaymentResultListener {
  public void onPaymentSuccess(String);
  public void onPaymentError(int, String);
}

-keepclassmembers class * implements com.razorpay.PaymentResultWithDataListener {
  public void onPaymentSuccess(String, com.razorpay.PaymentData);
  public void onPaymentError(int, String, com.razorpay.PaymentData);
}

-keep class com.razorpay.** {*;}
-keep class com.razorpay.checkout.** {*;}