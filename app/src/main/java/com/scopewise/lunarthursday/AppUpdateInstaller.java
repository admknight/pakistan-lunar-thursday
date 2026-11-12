package com.scopewise.lunarthursday;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Downloads a trusted signed GitHub APK inside the app, then invokes Android's installer. */
final class AppUpdateInstaller {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final AtomicBoolean RUNNING = new AtomicBoolean();
    private static final long MAX_APK_SIZE = 75L * 1024L * 1024L;
    private static final String PENDING = "downloaded_verified_update";
    private static final String PENDING_INSTALL = "downloaded_update_wants_install";

    private AppUpdateInstaller() { }

    static void prompt(Activity activity, UpdateManager.Result result) {
        if (result == null) return;
        if (RUNNING.get()) { toast(activity,"An update is already downloading");return; }
        if (result.digest == null || !result.digest.matches("[a-f0-9]{64}")) {
            toast(activity,"This release does not provide a verifiable SHA-256 digest"); return;
        }
        new AlertDialog.Builder(activity).setTitle(UiText.tr(activity,"Update Hilal Thursday"))
           .setMessage(UiText.tr(activity,"Download version ") + VersionPolicy.display(result.tag)
                 + UiText.tr(activity," securely inside this app? The APK is checked against its SHA-256 digest and the installed signing certificate before Android confirms installation."))
           .setNegativeButton(UiText.tr(activity,"Not now"),null)
           .setPositiveButton(UiText.tr(activity,"Download & install"),(d,w)->download(activity,result)).show();
    }

    static void resumeIfPermitted(Activity activity) {
        if (!ReminderStore.prefs(activity).getBoolean(PENDING_INSTALL,false)) return;
        if (!activity.getPackageManager().canRequestPackageInstalls()) return;
        File cached=cacheFile(activity);
        if (!cached.isFile()) {
            ReminderStore.prefs(activity).edit().remove(PENDING_INSTALL).remove(PENDING).apply();return;
        }
        ReminderStore.prefs(activity).edit().remove(PENDING_INSTALL).apply();
        try { verifyPackage(activity,cached); openInstaller(activity,cached); }
        catch(Exception ex) { cached.delete(); toast(activity,"Previously downloaded APK is no longer compatible"); }
    }

    private static File cacheFile(Activity activity) {
        File dir=new File(activity.getCacheDir(),"updates");
        if (!dir.exists() && !dir.mkdirs()) throw new IllegalStateException("Cannot create update cache");
        return new File(dir,"verified-update.apk");
    }

    private static void download(Activity activity, UpdateManager.Result target) {
        if (!RUNNING.compareAndSet(false,true)) return;
        final AtomicBoolean cancelled=new AtomicBoolean();
        LinearLayout layout=new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32,24,32,4);
        ProgressBar progress=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true); progress.setMax(100);
        layout.addView(progress,new LinearLayout.LayoutParams(-1,30));
        TextView message=new TextView(activity);
        message.setText(UiText.tr(activity,"Downloading securely…"));message.setGravity(Gravity.CENTER_VERTICAL);
        message.setPadding(0,16,0,14);
        layout.addView(message);
        AlertDialog dialog=new AlertDialog.Builder(activity)
           .setTitle(UiText.tr(activity,"Installing update"))
           .setView(layout)
           .setNegativeButton(UiText.tr(activity,"Cancel"),(d,w)->cancelled.set(true))
           .create();
        dialog.setOnCancelListener(d->cancelled.set(true));
        dialog.show();
        IO.execute(()->{
            File targetFile=null;
            try {
                targetFile=cacheFile(activity);
                File working=new File(targetFile.getParentFile(),"download.part");
                if(working.exists())working.delete();
                URL endpoint=new URL(target.url);
                HttpURLConnection connection=null;
                for(int hop=0;hop<7;hop++){
                    if(!isAllowedUrl(endpoint))throw new SecurityException("Untrusted download redirect");
                    connection=(HttpURLConnection)endpoint.openConnection();
                    connection.setConnectTimeout(12_000);connection.setReadTimeout(18_000);
                    connection.setInstanceFollowRedirects(false);
                    connection.setRequestProperty("User-Agent","HilalThursday-Android-Updater");
                    int code=connection.getResponseCode();
                    if(code==301||code==302||code==303||code==307||code==308){
                        String where=connection.getHeaderField("Location");
                        if(where==null)throw new SecurityException("Missing secure download target");
                        URL next=new URL(endpoint,where);connection.disconnect();endpoint=next;connection=null;continue;
                    }
                    if(code!=200)throw new IllegalStateException("Download unavailable (HTTP "+code+")");
                    break;
                }
                if(connection==null)throw new SecurityException("Too many download redirects");
                try {
                    long length=connection.getContentLengthLong();
                    if(length>MAX_APK_SIZE)throw new SecurityException("APK is larger than the allowed limit");
                    MessageDigest hash=MessageDigest.getInstance("SHA-256");
                    long total=0, lastUi=0;
                    try(InputStream input=connection.getInputStream();FileOutputStream out=new FileOutputStream(working)) {
                        byte[] bytes=new byte[32*1024];int n;
                        while((n=input.read(bytes))!=-1){
                            if(cancelled.get())throw new InterruptedException("Download cancelled");
                            total+=n;
                            if(total>MAX_APK_SIZE)throw new SecurityException("APK is larger than the allowed limit");
                            hash.update(bytes,0,n);out.write(bytes,0,n);
                            long stamp=SystemClock.elapsedRealtime();
                            if(stamp-lastUi>=250){lastUi=stamp;int pct=length>0?(int)(total*100/length):-1;
                                MAIN.post(()->{if(!activity.isFinishing()&&dialog.isShowing()){
                                    progress.setIndeterminate(pct<0);if(pct>=0)progress.setProgress(pct);
                                    message.setText(pct<0?"Downloading APK…": "Downloading APK… "+pct+"%");}});
                            }
                        }
                        out.getFD().sync();
                    }
                    if(length>=0&&length!=total)throw new SecurityException("Incomplete APK download");
                    byte[] digest=hash.digest();StringBuilder actual=new StringBuilder(64);
                    for(byte b:digest)actual.append(String.format(Locale.US,"%02x",b & 255));
                    if(!MessageDigest.isEqual(actual.toString().getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                      target.digest.getBytes(java.nio.charset.StandardCharsets.US_ASCII)))
                        throw new SecurityException("SHA-256 mismatch");
                    verifyPackage(activity,working);
                    if(targetFile.exists()&&!targetFile.delete())throw new IllegalStateException("Cannot replace cached APK");
                    if(!working.renameTo(targetFile))throw new IllegalStateException("Cannot cache verified APK");
                } finally {connection.disconnect();}
                if(cancelled.get())throw new InterruptedException("Download cancelled");
                ReminderStore.prefs(activity).edit().putString(PENDING,target.tag).apply();
                File signed=targetFile;
                MAIN.post(()->{if(dialog.isShowing())dialog.dismiss();if(!activity.isFinishing())startInstallation(activity,signed);});
            } catch(Exception failure) {
                File dir=new File(activity.getCacheDir(),"updates");
                new File(dir,"download.part").delete();
                String label=failure instanceof InterruptedException?"Download cancelled":
                    "Update could not be verified: "+failure.getMessage();
                MAIN.post(()->{if(dialog.isShowing())dialog.dismiss();if(!activity.isFinishing())toast(activity,label);});
            } finally {RUNNING.set(false);}
        });
    }

    private static boolean isAllowedUrl(URL url) {
        String host=url.getHost().toLowerCase(Locale.ROOT);
        boolean known=host.equals("github.com")||host.equals("release-assets.githubusercontent.com")
                ||host.equals("objects.githubusercontent.com");
        return known && url.getProtocol().equals("https") && url.getUserInfo()==null && url.getPort()==-1;
    }

    private static void verifyPackage(Activity activity,File file) throws Exception {
        PackageManager pm=activity.getPackageManager();
        int flags=Build.VERSION.SDK_INT>=28?PackageManager.GET_SIGNING_CERTIFICATES:PackageManager.GET_SIGNATURES;
        PackageInfo candidate=pm.getPackageArchiveInfo(file.getAbsolutePath(),flags);
        PackageInfo installed=pm.getPackageInfo(activity.getPackageName(),flags);
        if(candidate==null||!activity.getPackageName().equals(candidate.packageName))
            throw new SecurityException("Unexpected Android package identity");
        long newest=Build.VERSION.SDK_INT>=28?candidate.getLongVersionCode():candidate.versionCode;
        long existing=Build.VERSION.SDK_INT>=28?installed.getLongVersionCode():installed.versionCode;
        if(newest<=existing)throw new SecurityException("APK version must be newer than installed version");
        Signature[] signers=Build.VERSION.SDK_INT>=28?candidate.signingInfo.getApkContentsSigners():candidate.signatures;
        Signature[] trusted=Build.VERSION.SDK_INT>=28?installed.signingInfo.getApkContentsSigners():installed.signatures;
        if(signers==null||trusted==null||signers.length!=trusted.length||signers.length==0)
            throw new SecurityException("Missing or incompatible APK certificate");
        for(int i=0;i<signers.length;i++){
            boolean found=false;
            for(Signature t:trusted)if(Arrays.equals(signers[i].toByteArray(),t.toByteArray())){found=true;break;}
            if(!found)throw new SecurityException("APK signing key differs from installed app");
        }
    }

    private static void startInstallation(Activity activity,File file) {
        if(!activity.getPackageManager().canRequestPackageInstalls()){
            ReminderStore.prefs(activity).edit().putBoolean(PENDING_INSTALL,true).apply();
            new AlertDialog.Builder(activity).setTitle(UiText.tr(activity,"Android installation permission"))
                .setMessage(UiText.tr(activity,"Allow Hilal Thursday to install app updates in Android settings. Return to this app to continue. No web browser is needed."))
                .setNegativeButton(UiText.tr(activity,"Later"),(d,w)->ReminderStore.prefs(activity).edit().remove(PENDING_INSTALL).apply())
                .setPositiveButton(UiText.tr(activity,"Open Android settings"),(d,w)->{
                    try {activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                          Uri.parse("package:"+activity.getPackageName())));}
                    catch(RuntimeException ex){toast(activity,"Open app installation permissions in Android settings");}
                }).show();
            return;
        }
        openInstaller(activity,file);
    }

    private static void openInstaller(Activity activity,File file) {
        try {
            Uri uri=FileProvider.getUriForFile(activity,activity.getPackageName()+".fileprovider",file);
            Intent install=new Intent(Intent.ACTION_INSTALL_PACKAGE);
            install.setDataAndType(uri,"application/vnd.android.package-archive");
            install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(install);
        } catch(Exception ex){toast(activity,"Android installer unavailable: "+ex.getMessage());}
    }

    private static void toast(Activity activity,String message) {
        Toast.makeText(activity,UiText.tr(activity,message),Toast.LENGTH_LONG).show();
    }
}
