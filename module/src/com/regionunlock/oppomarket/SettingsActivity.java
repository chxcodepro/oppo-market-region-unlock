package com.regionunlock.oppomarket;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Simple information screen shown from the launcher.
 * It also provides quick links into the OPPO store settings page.
 */
public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("OPPO 商店地区解锁");

        int pad = dp(18);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("LSPosed 模块已安装");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        TextView body = new TextView(this);
        body.setText("作用：解除 OPPO 软件商店「设置 / 服务与诊断 → 服务国家/地区」的切换限制。\n\n"
                + "使用步骤：\n"
                + "1. 在 LSPosed 管理器中启用本模块，作用域勾选「软件商店」(com.heytap.market)。\n"
                + "2. 强制停止软件商店，然后重新打开。\n"
                + "3. 进入「我的 → 设置 → 服务与诊断 → 服务国家/地区」，即可正常切换。\n\n"
                + "如果入口仍是灰色或不可点击：\n"
                + "• 确认模块已启用且作用域正确；\n"
                + "• 重启手机后再试；\n"
                + "• 在 LSPosed 日志中搜索 OppoRegionUnlock 查看 hook 结果。\n\n"
                + "说明：模块只解除“能否切换”的校验，可选地区仍由商店自身的地区列表决定。");
        body.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        body.setTextColor(Color.DKGRAY);
        body.setPadding(0, dp(14), 0, dp(18));
        root.addView(body);

        Button openSettings = new Button(this);
        openSettings.setText("打开软件商店的设置页");
        openSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openStoreSettings();
            }
        });
        root.addView(openSettings, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        Button openStore = new Button(this);
        openStore.setText("启动软件商店");
        openStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchStore();
            }
        });
        root.addView(openStore, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.WHITE);
        scroll.addView(root);
        setContentView(scroll);
    }

    private void openStoreSettings() {
        Intent intent = new Intent("oppo.intent.action.APP_SETTINGS");
        intent.setPackage("com.heytap.market");
        try {
            startActivity(intent);
        } catch (Exception e) {
            launchStore();
        }
    }

    private void launchStore() {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.heytap.market");
        if (intent != null) {
            startActivity(intent);
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("未找到软件商店")
                    .setMessage("请确认已安装 OPPO 软件商店 (com.heytap.market)。")
                    .setPositiveButton("知道了", null)
                    .show();
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
