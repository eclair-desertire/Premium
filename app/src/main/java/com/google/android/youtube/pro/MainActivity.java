package com.google.android.youtube.pro;

import android.Manifest;
import android.app.Activity;
import android.app.*;
import android.os.*;
import android.view.*;
import android.view.View.*;
import android.widget.*;
import android.content.*;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.webkit.*;
import android.animation.*;
import android.view.animation.*;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import android.app.DownloadManager;

import java.text.*;

import org.json.*;

import android.webkit.WebView;
import android.webkit.WebSettings;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebViewClient;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.DialogFragment;
import android.util.Base64;

import java.io.InputStream;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.*;
import android.app.PictureInPictureParams;
import android.util.Rational;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class MainActivity extends Activity {

    private WebView web;
    private Intent i = new Intent();
    private boolean portrait = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        web = findViewById(R.id.web);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setSupportZoom(true);
        web.getSettings().setBuiltInZoomControls(true);
        web.getSettings().setDisplayZoomControls(false);
        web.loadUrl("https://m.youtube.com/");
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setDatabaseEnabled(true);
        web.addJavascriptInterface(new WebAppInterface(this), "Android");
        web.setWebChromeClient(new CustomWebClient());


        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView p1, String p2, Bitmap p3) {

                super.onPageStarted(p1, p2, p3);
            }

            @Override
            public void onPageFinished(WebView p1, String p2) {

                web.loadUrl("javascript:(function () { var script = document.createElement('script'); script.src='https://cdn.jsdelivr.net/npm/ytpro'; document.body.appendChild(script);  })();");

//For Using Local JS file uncomment the below line
                inject();
                super.onPageFinished(p1, p2);
            }
        });

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                web.loadUrl("https://m.youtube.com");
            } else {
                Toast.makeText(getApplicationContext(), "Please Grant Microphone Permission in order to use Speech Recognition", Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) {
            web.goBack();
        } else {
            finish();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        if (isInPictureInPictureMode) {
            web.loadUrl("javascript:document.getElementsByClassName('video-stream')[0].play();");
        } else {
            web.loadUrl("javascript:removePIP();");
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        web.loadUrl("javascript:PIPlayer();");
    }


    public class CustomWebClient extends WebChromeClient {
        private View mCustomView;
        private WebChromeClient.CustomViewCallback mCustomViewCallback;
        protected FrameLayout frame;
        private int mOriginalOrientation;
        private int mOriginalSystemUiVisibility;

        public CustomWebClient() {
        }


        public Bitmap getDefaultVideoPoster() {

            if (MainActivity.this == null) {
                return null;
            }
            return BitmapFactory.decodeResource(MainActivity.this.getApplicationContext().getResources(), 2130837573);
        }


        public void onShowCustomView(View paramView, WebChromeClient.CustomViewCallback viewCallback) {

            if (portrait) {
                this.mOriginalOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
            } else {
                this.mOriginalOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
            }

            if (this.mCustomView != null) {
                onHideCustomView();
                return;
            }
            this.mCustomView = paramView;
            this.mOriginalSystemUiVisibility = MainActivity.this.getWindow().getDecorView().getSystemUiVisibility();
            MainActivity.this.setRequestedOrientation(this.mOriginalOrientation);
            this.mOriginalOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
            this.mCustomViewCallback = viewCallback;
            ((FrameLayout) MainActivity.this.getWindow().getDecorView()).addView(this.mCustomView, new FrameLayout.LayoutParams(-1, -1));
            MainActivity.this.getWindow().getDecorView().setSystemUiVisibility(3846);
        }

        public void onHideCustomView() {

            ((FrameLayout) MainActivity.this.getWindow().getDecorView()).removeView(this.mCustomView);
            this.mCustomView = null;
            MainActivity.this.getWindow().getDecorView().setSystemUiVisibility(this.mOriginalSystemUiVisibility);
            MainActivity.this.setRequestedOrientation(this.mOriginalOrientation);


            if (portrait) {
                this.mOriginalOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
            } else {
                this.mOriginalOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
            }

            this.mCustomViewCallback = null;
            web.clearFocus();
        }

        @Override
        public void onPermissionRequest(final PermissionRequest request) {

            if (request.getOrigin().toString().contains("youtube.com")) {

                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 101);
                } else {
                    request.grant(request.getResources());
                }

            }
        }

    }

    private void downloadFile(String filename, String url, String mtype) {
        try {
            try {
                String encodedFileName = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");

                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setTitle(filename)
                        .setDescription(filename)
                        .setMimeType(mtype)
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(true)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS.toString(), encodedFileName)
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE |
                                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                downloadManager.enqueue(request);
                Toast.makeText(this, "Download Started", Toast.LENGTH_SHORT).show();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } catch (Exception ignored) {
            Toast.makeText(this, ignored.toString(), Toast.LENGTH_SHORT).show();
        }
    }


    public class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void showToast(String txt) {

            Toast.makeText(getApplicationContext(), txt + "", Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void gohome(String x) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
        }

        @JavascriptInterface
        public void downvid(String name, String url, String m) {
            downloadFile(name, url, m);
        }

        @JavascriptInterface
        public void fullScreen(boolean value) {
            portrait = value;
        }

        @JavascriptInterface
        public void oplink(String url) {
            Intent i = new Intent();
            i.setAction(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        }

        @JavascriptInterface
        public String getInfo() {
            PackageManager manager = getApplicationContext().getPackageManager();
            try {
                PackageInfo info = manager.getPackageInfo(getApplicationContext().getPackageName(), PackageManager.GET_ACTIVITIES);
                return info.versionName + "";
            } catch (PackageManager.NameNotFoundException e) {
                return "1.0";
            }

        }

        @JavascriptInterface
        public void pipvid(String x) {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                try {
                    PictureInPictureParams params;
                    if (portrait) {
                        params = new PictureInPictureParams.Builder().setAspectRatio(new Rational(9, 16)).build();
                    } else {
                        params = new PictureInPictureParams.Builder().setAspectRatio(new Rational(16, 9)).build();
                    }
                    enterPictureInPictureMode(params);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                }

            } else {
                Toast.makeText(getApplicationContext(), "PIP not Supported", Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void inject() {

        try {
            InputStream inputStream = getAssets().open("app.js");
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            String encoded = Base64.encodeToString(buffer, Base64.NO_WRAP);
            web.loadUrl("javascript:(function() {" +
                    "var parent = document.getElementsByTagName('head').item(0);" +
                    "var script = document.createElement('script');" +
                    "script.type = 'text/javascript';" +
                    "script.innerHTML = window.atob('" + encoded + "');" +
                    "parent.appendChild(script)" +
                    "})()");
        } catch (Exception e) {
        }
    }
}
