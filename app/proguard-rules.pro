# R8 / ProGuard — myRSync
# Keep rules are intentionally narrow. Broad `-keep class … { *; }` freezes
# members from obfuscation/shrinking and lowers Play App optimization scores.
# Manifest components, ViewPager fragments, Billing, and security-crypto are
# already kept/reachable via the manifest, direct references, or AAR consumer rules.

# Readable crash stacks in Play Console (upload mapping.txt with the AAB).
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Layout android:onClick looks up these method names by reflection.
# Keep only those members — not the entire Activity.
-keepclassmembers class com.linminitools.myrsync.MainActivity {
    public void createConfig(android.view.View);
    public void config_clickHandler(android.view.View);
}
-keepclassmembers class com.linminitools.myrsync.addConfig {
    public void addPath(android.view.View);
    public void addSshKeyPath(android.view.View);
    public void viewCommand(android.view.View);
    public void saveConfig(android.view.View);
    public void rsync_help(android.view.View);
    public void rsync_adv_opts_help(android.view.View);
    public void select_path_help(android.view.View);
    public void rsync_daemon_info(android.view.View);
}
-keepclassmembers class com.linminitools.myrsync.TaskerPluginEditActivity {
    public void editConfigs(android.view.View);
    public void savePluginConfig(android.view.View);
}

# JNI entry points only (if any).
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}
