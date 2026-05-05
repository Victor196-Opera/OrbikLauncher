package com.orbik.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int GRID_COLUMNS = 3;
    private static final int REQUEST_PICK_WALLPAPER = 300;
    
    private GridLayout gridLayout;
    private LinearLayout mainLayout;
    private PackageManager packageManager;
    private BroadcastReceiver packageReceiver;
    private Handler handler = new Handler();
    private List<AppInfo> appList = new ArrayList<>();
    private Map<String, Drawable> iconCache = new HashMap<>();
    private File wallpaperFile;
    private boolean isEnglish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        hideStatusBar();
        wallpaperFile = new File(getFilesDir(), "wallpaper.jpg");
        packageManager = getPackageManager();
        Locale locale = getResources().getConfiguration().locale;
        isEnglish = locale.getLanguage().equals("en");
        setupUI();
        loadWallpaper();
        loadApps();
        registerPackageReceiver();
    }

    private void hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(true);
            try { getWindow().getInsetsController().hide(android.view.WindowInsets.Type.statusBars()); } catch (Exception e) {}
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        isEnglish = newConfig.locale.getLanguage().equals("en");
        if (!appList.isEmpty()) displayApps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideStatusBar();
        if (gridLayout.getChildCount() == 0 && !appList.isEmpty()) displayApps();
    }

    private String s(String vi, String en) {
        return isEnglish ? en : vi;
    }

    private void loadWallpaper() {
        if (wallpaperFile.exists()) {
            try {
                Bitmap saved = BitmapFactory.decodeFile(wallpaperFile.getAbsolutePath());
                if (saved != null) {
                    DisplayMetrics dm = getResources().getDisplayMetrics();
                    Bitmap cropped = cropCenter(saved, dm.widthPixels, dm.heightPixels);
                    mainLayout.setBackground(new BitmapDrawable(getResources(), cropped));
                    return;
                }
            } catch (Exception e) {}
        }
        try {
            WallpaperManager wm = WallpaperManager.getInstance(this);
            Drawable wp = wm.getDrawable();
            if (wp instanceof BitmapDrawable) {
                Bitmap original = ((BitmapDrawable) wp).getBitmap();
                if (original != null) {
                    DisplayMetrics dm = getResources().getDisplayMetrics();
                    Bitmap cropped = cropCenter(original, dm.widthPixels, dm.heightPixels);
                    mainLayout.setBackground(new BitmapDrawable(getResources(), cropped));
                    return;
                }
            }
            if (wp != null) { mainLayout.setBackground(wp); return; }
        } catch (Exception e) {}
        mainLayout.setBackgroundColor(0xFF1a1a2e);
    }

    private void pickWallpaper() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission("android.permission.READ_MEDIA_IMAGES") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.READ_MEDIA_IMAGES"}, 400);
                return;
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{"android.permission.READ_EXTERNAL_STORAGE"}, 400);
                return;
            }
        }
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try { startActivityForResult(intent, REQUEST_PICK_WALLPAPER); }
        catch (Exception e) { Toast.makeText(this, s("Không tìm thấy ứng dụng tệp", "No file app found"), Toast.LENGTH_SHORT).show(); }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        if (code == 400 && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) {
            pickWallpaper();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQUEST_PICK_WALLPAPER && res == RESULT_OK && data != null) {
            try {
                Uri uri = data.getData();
                InputStream is = getContentResolver().openInputStream(uri);
                Bitmap original = BitmapFactory.decodeStream(is);
                is.close();
                if (original != null) {
                    FileOutputStream fos = new FileOutputStream(wallpaperFile);
                    original.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                    fos.close();
                    DisplayMetrics dm = getResources().getDisplayMetrics();
                    Bitmap cropped = cropCenter(original, dm.widthPixels, dm.heightPixels);
                    mainLayout.setBackground(new BitmapDrawable(getResources(), cropped));
                    Toast.makeText(this, s("Đã thay đổi hình nền", "Wallpaper changed"), Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(this, s("Không thể đọc ảnh", "Cannot read image"), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private Bitmap cropCenter(Bitmap source, int targetW, int targetH) {
        int srcW = source.getWidth(), srcH = source.getHeight();
        float targetRatio = (float) targetW / targetH;
        int newW, newH;
        if ((float) srcW / srcH > targetRatio) { newH = srcH; newW = (int) (newH * targetRatio); }
        else { newW = srcW; newH = (int) (newW / targetRatio); }
        int x = (srcW - newW) / 2, y = (srcH - newH) / 2;
        Bitmap cropped = Bitmap.createBitmap(source, x, y, newW, newH);
        Bitmap scaled = Bitmap.createScaledBitmap(cropped, targetW, targetH, true);
        if (cropped != scaled) cropped.recycle();
        return scaled;
    }

    private String getWallpaperAppPackage() {
        if (!appList.isEmpty()) return appList.get(0).packageName;
        return null;
    }

    private void loadApps() {
        new Thread(new Runnable() { @Override public void run() {
            List<AppInfo> list = new ArrayList<>();
            Intent intent = new Intent(Intent.ACTION_MAIN, null);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> riList = packageManager.queryIntentActivities(intent, 0);
            for (ResolveInfo ri : riList) {
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName())) continue;
                list.add(new AppInfo(ri.loadLabel(packageManager).toString(), pkg, ri));
                try { iconCache.put(pkg, ri.loadIcon(packageManager)); } catch (Exception e) {}
            }
            Collections.sort(list, new Comparator<AppInfo>() {
                public int compare(AppInfo a, AppInfo b) { return a.appName.compareToIgnoreCase(b.appName); }
            });
            appList = list;
            runOnUiThread(new Runnable() { @Override public void run() { displayApps(); } });
        }}).start();
    }

    private void displayApps() {
        if (gridLayout == null || appList.isEmpty()) return;
        gridLayout.removeAllViews();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        float density = dm.density;
        int screenW = dm.widthPixels;
        int pad = Math.max((int)(16*density), (int)(screenW*0.03));
        int colW = (screenW - pad*(GRID_COLUMNS+1)) / GRID_COLUMNS;
        int iconSize = Math.min((int)(colW * 0.60), (int)(72 * density));
        int gapV = pad;

        String targetPkg = getWallpaperAppPackage();

        for (int i = 0; i < appList.size(); i++) {
            final AppInfo app = appList.get(i);
            boolean canChangeWallpaper = app.packageName.equals(targetPkg);
            View v = createAppView(app, iconSize, canChangeWallpaper);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams();
            p.width = colW; p.height = GridLayout.LayoutParams.WRAP_CONTENT;
            int col = i % GRID_COLUMNS;
            p.setMargins(col==0?pad:pad/2, gapV, col==GRID_COLUMNS-1?pad:pad/2, gapV);
            p.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1, 1f);
            v.setLayoutParams(p);
            gridLayout.addView(v);
        }
        gridLayout.requestLayout();
    }

    private View createAppView(final AppInfo app, int iconSize, final boolean canChangeWallpaper) {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER_HORIZONTAL);
        l.setPadding(4, 10, 4, 10);

        ImageView icon = new ImageView(this);
        icon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
        Drawable d = iconCache.get(app.packageName);
        if (d != null) icon.setImageDrawable(d);
        else icon.setImageResource(android.R.drawable.sym_def_app_icon);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);

        TextView name = new TextView(this);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tp.setMargins(0, (int)(4 * getResources().getDisplayMetrics().density), 0, 0);
        name.setLayoutParams(tp);
        name.setGravity(Gravity.CENTER);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        name.setTextColor(0xFFFFFFFF);
        name.setShadowLayer(2, 0, 1, 0xFF000000);
        name.setTypeface(null, Typeface.BOLD);
        name.setText(app.appName);
        l.addView(icon);
        l.addView(name);

        final String pkg = app.packageName;
        l.setOnClickListener(new View.OnClickListener() { @Override public void onClick(final View v) {
            handler.postDelayed(new Runnable() { public void run() { v.setScaleX(0.8f); v.setScaleY(0.8f); } }, 100);
            handler.postDelayed(new Runnable() { public void run() { v.setScaleX(1.0f); v.setScaleY(1.0f); } }, 200);
            handler.postDelayed(new Runnable() { public void run() { launchApp(pkg); } }, 300);
        }});
        l.setOnLongClickListener(new View.OnLongClickListener() { @Override public boolean onLongClick(View v) {
            showMenu(app.appName, pkg, canChangeWallpaper); return true;
        }});
        return l;
    }

    private void showMenu(String appName, String pkg, boolean canChangeWallpaper) {
        if (canChangeWallpaper) {
            new AlertDialog.Builder(this).setTitle(s("Danh mục", "Menu"))
                .setItems(new String[]{
                    s("Gỡ cài đặt", "Uninstall"),
                    s("Thông tin", "App Info"),
                    s("🖼️ Thay đổi hình nền", "🖼️ Change Wallpaper")
                }, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        if (w == 0) startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:"+pkg)));
                        else if (w == 1) startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+pkg)));
                        else pickWallpaper();
                    }
                }).setNegativeButton(s("Hủy", "Cancel"), null).show();
        } else {
            new AlertDialog.Builder(this).setTitle(s("Danh mục", "Menu"))
                .setItems(new String[]{
                    s("Gỡ cài đặt", "Uninstall"),
                    s("Thông tin", "App Info")
                }, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        if (w == 0) startActivity(new Intent(Intent.ACTION_UNINSTALL_PACKAGE, Uri.parse("package:"+pkg)));
                        else startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:"+pkg)));
                    }
                }).setNegativeButton(s("Hủy", "Cancel"), null).show();
        }
    }

    private void launchApp(String pkg) {
        try { startActivity(getPackageManager().getLaunchIntentForPackage(pkg)); }
        catch (Exception e) { Toast.makeText(this, s("Không thể mở", "Cannot open"), Toast.LENGTH_SHORT).show(); }
    }

    private void registerPackageReceiver() {
        packageReceiver = new BroadcastReceiver() { @Override public void onReceive(Context c, Intent i) { loadApps(); }};
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED); f.addAction(Intent.ACTION_PACKAGE_REMOVED); f.addAction(Intent.ACTION_PACKAGE_REPLACED);
        f.addDataScheme("package");
        registerReceiver(packageReceiver, f);
    }

    private void setupUI() {
        mainLayout = new LinearLayout(this);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(this);
        sv.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        sv.setVerticalScrollBarEnabled(false);
        gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(GRID_COLUMNS);
        gridLayout.setPadding(0, 0, 0, 24);
        sv.addView(gridLayout);
        mainLayout.addView(sv);
        setContentView(mainLayout);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (packageReceiver != null) try { unregisterReceiver(packageReceiver); } catch (Exception e) {}
        iconCache.clear();
    }

    static class AppInfo {
        String appName, packageName; ResolveInfo resolveInfo;
        AppInfo(String n, String p, ResolveInfo r) { appName=n; packageName=p; resolveInfo=r; }
    }
}
