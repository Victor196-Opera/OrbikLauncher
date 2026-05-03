package com.orbik.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private static final int GRID_COLUMNS = 3;
    private GridLayout gridLayout;
    private LinearLayout mainLayout;
    private List<AppInfo> appList;
    private PackageManager packageManager;
    private BroadcastReceiver packageReceiver;
    private boolean isAppsLoaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        setupFullScreen();
        setupUI();
        loadWallpaperFast();
        packageManager = getPackageManager();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                loadApps();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        displayApps();
                    }
                });
            }
        }).start();
        
        registerPackageReceiver();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!isAppsLoaded) {
            loadApps();
            displayApps();
        }
    }

    private void setupFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            try {
                Object controller = getWindow().getInsetsController();
                if (controller != null) {
                    int statusBars = WindowManager.LayoutParams.class.getField("LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES").getInt(null);
                    controller.getClass().getMethod("hide", int.class).invoke(controller, statusBars);
                    controller.getClass().getMethod("setSystemBarsBehavior", int.class).invoke(controller, 1);
                }
            } catch (Exception e) {
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        } else if (Build.VERSION.SDK_INT >= 19) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setupFullScreen();
        }
    }

    private void setupUI() {
        mainLayout = new LinearLayout(this);
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        mainLayout.setOrientation(LinearLayout.VERTICAL);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        scrollView.setVerticalScrollBarEnabled(false); // TẮT THANH CUỘN

        gridLayout = new GridLayout(this);
        gridLayout.setColumnCount(GRID_COLUMNS);
        gridLayout.setPadding(24, 48, 24, 24);

        scrollView.addView(gridLayout);
        mainLayout.addView(scrollView);
        setContentView(mainLayout);
    }

    private void loadWallpaperFast() {
        try {
            DisplayMetrics dm = getResources().getDisplayMetrics();
            mainLayout.setBackgroundColor(0xFF1a1a2e);
            
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        String path = getFilesDir().getAbsolutePath() + "/wallpaper.jpg";
                        File imgFile = new File(path);
                        if (imgFile.exists()) {
                            final Bitmap originalBitmap = BitmapFactory.decodeFile(path);
                            if (originalBitmap != null) {
                                final Bitmap scaled = Bitmap.createScaledBitmap(originalBitmap, 
                                        dm.widthPixels, dm.heightPixels, true);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mainLayout.setBackground(new BitmapDrawable(getResources(), scaled));
                                    }
                                });
                            }
                        }
                    } catch (Exception e) {}
                }
            }).start();
        } catch (Exception e) {
            mainLayout.setBackgroundColor(0xFF1a1a2e);
        }
    }

    private void loadApps() {
        appList = new ArrayList<>();
        Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = packageManager.queryIntentActivities(intent, 0);
        for (ResolveInfo ri : list) {
            appList.add(new AppInfo(ri.loadLabel(packageManager).toString(),
                    ri.activityInfo.packageName, ri));
        }
        Collections.sort(appList, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo a, AppInfo b) {
                return a.appName.compareToIgnoreCase(b.appName);
            }
        });
        isAppsLoaded = true;
    }

    private void displayApps() {
        gridLayout.removeAllViews();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int colW = (dm.widthPixels - (GRID_COLUMNS + 1) * 24) / GRID_COLUMNS;
        for (AppInfo app : appList) {
            View appView = createAppView(app, colW);
            gridLayout.addView(appView);
        }
    }

    private View createAppView(final AppInfo app, int width) {
        LinearLayout layout = new LinearLayout(this);
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = width;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        params.setMargins(4, 6, 4, 6);
        layout.setLayoutParams(params);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);
        layout.setPadding(10, 14, 10, 14);

        int iconSize = (int)(width * 0.55);
        ImageView icon = new ImageView(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
        iconParams.gravity = Gravity.CENTER;
        icon.setLayoutParams(iconParams);
        icon.setImageDrawable(app.resolveInfo.loadIcon(packageManager));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);

        TextView name = new TextView(this);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textParams.setMargins(0, 8, 0, 0);
        name.setLayoutParams(textParams);
        name.setText(app.appName);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        name.setTextColor(0xFFFFFFFF);
        name.setShadowLayer(2, 0, 1, 0xFF000000);
        name.setTypeface(null, Typeface.BOLD);

        layout.addView(icon);
        layout.addView(name);

        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                final Handler handler = new Handler();
                
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        v.setScaleX(0.8f);
                        v.setScaleY(0.8f);
                    }
                }, 100);
                
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        v.setScaleX(1.0f);
                        v.setScaleY(1.0f);
                    }
                }, 200);
                
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        launchApp(app.packageName);
                    }
                }, 300);
            }
        });
        
        layout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                uninstallApp(app.packageName);
                return true;
            }
        });

        return layout;
    }

    private void launchApp(String pkg) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
            if (i != null) startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Không thể mở", Toast.LENGTH_SHORT).show();
        }
    }

    private void uninstallApp(String pkg) {
        Intent i = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        i.setData(Uri.parse("package:" + pkg));
        i.putExtra(Intent.EXTRA_RETURN_RESULT, true);
        startActivityForResult(i, 1000);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 1000) {
            loadApps();
            displayApps();
        }
    }

    private void registerPackageReceiver() {
        packageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context c, Intent i) {
                loadApps();
                displayApps();
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addAction(Intent.ACTION_PACKAGE_REPLACED);
        registerReceiver(packageReceiver, f);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (packageReceiver != null) {
            try { unregisterReceiver(packageReceiver); } catch (Exception e) {}
        }
    }
}
