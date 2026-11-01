# Enigma - release build rules.
#
# The app is pure Kotlin/Compose, so only a few entry points must survive shrinking.

# Android entry point.
-keep class com.enigma.android.MainActivity { *; }

# ViewModels are instantiated reflectively by the lifecycle library.
-keepclassmembers class com.enigma.android.viewmodel.** extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep annotations/metadata: Compose compiler output and kotlin-reflect rely on them.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, MethodParameters

# Coroutines ship their own consumer rules; silence the optional debug agent.
-dontwarn kotlinx.coroutines.**
