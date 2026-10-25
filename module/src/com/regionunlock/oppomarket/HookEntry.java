package com.regionunlock.oppomarket;

import android.app.Activity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * LSPosed hook for com.heytap.market (OPPO Software Store).
 *
 * Removes the restrictions around
 *   Settings / Services & diagnostics -> Service country/region.
 *
 * Supported target versions (both verified statically):
 *   26.7.0_CN  (versionCode 260700)
 *   26.10.2_CN (versionCode 261002)
 *
 * The app is R8-obfuscated, and the obfuscated names change between builds.
 * The stable anchors are:
 *   - com.heytap.mspsdk.regionv3.impl.g.\u0560(String) -> regionv3.c
 *   - com.heytap.market.overseastravel.RegionRiskRepo
 *   - com.heytap.market.mine.cn.overseastravel.RegionSwitchRepo
 * Everything else is resolved through candidate names / method signatures.
 */
public final class HookEntry implements IXposedHookLoadPackage {

    private static final String TAG = "OppoRegionUnlock";
    private static final String TARGET_PACKAGE = "com.heytap.market";

    private static final String CLS_REGION_SWITCH_REPO =
            "com.heytap.market.mine.cn.overseastravel.RegionSwitchRepo";
    private static final String CLS_REGION_RISK_REPO =
            "com.heytap.market.overseastravel.RegionRiskRepo";
    private static final String CLS_REGION_RISK_CALLBACK =
            "com.heytap.market.overseastravel.RegionRiskRepo$a";
    private static final String CLS_SDK_IMPL = "com.heytap.mspsdk.regionv3.impl.g";
    private static final String CLS_SDK_RESULT = "com.heytap.mspsdk.regionv3.c";

    // Stable / common member names.
    private static final String M_SDK_CHECK = "\u0560";       // g.\u0560(String) -> c
    private static final String M_RISK_PARSE = "\u0620";      // RegionRiskRepo.\u0620(RiskProcessResp) -> result
    private static final String M_RISK_ASYNC = "\u058f";      // RegionRiskRepo.\u058f(List, callback)
    private static final String M_RESULT_OK = "\u037f";       // result.isSuccess() / callback.\u037f(result) / result$c.\u037f
    private static final String M_CTA_FLAG = "\u052f";        // prefs helper.isOverseaCtaAccepted()Z

    // RegionSwitchRepo.isSupportRegionSwitch()Z
    private static final String M_IS_SUPPORT_SWITCH_267 = "\u078d";
    private static final String M_IS_SUPPORT_SWITCH_2610 = "\u078b";
    // RegionSwitchRepo.openRegionSelectionPage(Activity, callback)
    private static final String M_OPEN_PAGE_267 = "\u0792";
    private static final String M_OPEN_PAGE_2610 = "\u0790";

    // App-side support check (String)Z.
    private static final String CLS_PREFS_267 = "a.a.a.ytc";
    private static final String M_IS_SUPPORT_REGION_267 = "\u078b";
    private static final String CLS_PREFS_2610 = "a.a.a.atd";
    private static final String M_IS_SUPPORT_REGION_2610 = "\u0787";
    private static final String M_IS_SUPPORT_REGION_2610_B = "\u0788";

    // Final overseas-CTA gate ()Z.
    private static final String CLS_CTA_PREFS_267 = "a.a.a.asc";
    private static final String CLS_CTA_PREFS_2610 = "a.a.a.nod";

    // Region-switch facade implementations.
    // 26.7.0: a.a.a.otc (real) / a.a.a.y45 (stub), callback a.a.a.ntc
    // 26.10.2: a.a.a.pqd (real) / a.a.a.jr5 (stub), callback a.a.a.nqd
    private static final String CLS_FACADE_REAL_267 = "a.a.a.otc";
    private static final String CLS_FACADE_STUB_267 = "a.a.a.y45";
    private static final String CLS_CALLBACK_267 = "a.a.a.ntc";
    private static final String CLS_FACADE_REAL_2610 = "a.a.a.pqd";
    private static final String CLS_FACADE_STUB_2610 = "a.a.a.jr5";
    private static final String CLS_CALLBACK_2610 = "a.a.a.nqd";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!TARGET_PACKAGE.equals(lpparam.packageName)) {
            return;
        }

        ClassLoader cl = lpparam.classLoader;
        log("module loaded in " + lpparam.processName);

        // ------------------------------------------------------------------
        // 1) Make the "Service country/region" preference visible/clickable.
        // ------------------------------------------------------------------
        hookReturnTrueIfPresent(cl, CLS_FACADE_REAL_2610, "isSupportRegionSwitch");
        hookReturnTrueIfPresent(cl, CLS_FACADE_STUB_2610, "isSupportRegionSwitch");
        hookReturnTrueIfPresent(cl, CLS_FACADE_REAL_267, "isSupportRegionSwitch");
        hookReturnTrueIfPresent(cl, CLS_FACADE_STUB_267, "isSupportRegionSwitch");

        hookReturnTrueIfPresent(cl, CLS_REGION_SWITCH_REPO, M_IS_SUPPORT_SWITCH_267);
        hookReturnTrueIfPresent(cl, CLS_REGION_SWITCH_REPO, M_IS_SUPPORT_SWITCH_2610);

        // ------------------------------------------------------------------
        // 2) App-side "is this region supported?" check: accept any code.
        // ------------------------------------------------------------------
        hookReturnTrueIfPresent(cl, CLS_PREFS_267, M_IS_SUPPORT_REGION_267, String.class);
        hookReturnTrueIfPresent(cl, CLS_PREFS_2610, M_IS_SUPPORT_REGION_2610, String.class);
        hookReturnTrueIfPresent(cl, CLS_PREFS_2610, M_IS_SUPPORT_REGION_2610_B, String.class);

        // ------------------------------------------------------------------
        // 3) Overseas CTA flag: skip the "go abroad first" dialog and persist.
        // ------------------------------------------------------------------
        hookReturnTrueIfPresent(cl, CLS_CTA_PREFS_267, M_CTA_FLAG);
        hookReturnTrueIfPresent(cl, CLS_CTA_PREFS_2610, M_CTA_FLAG);

        // ------------------------------------------------------------------
        // 4) SDK target-region eligibility: always report success.
        // ------------------------------------------------------------------
        hookSdkTargetRegionCheck(cl);

        // ------------------------------------------------------------------
        // 5) Risk control: pass without touching the network.
        // ------------------------------------------------------------------
        hookRiskControl(cl);

        // ------------------------------------------------------------------
        // 6) If a build binds the no-op facade, forward the click to the repo.
        // ------------------------------------------------------------------
        hookFallbackOpenPage(cl, CLS_FACADE_STUB_267, M_OPEN_PAGE_267, CLS_CALLBACK_267);
        hookFallbackOpenPage(cl, CLS_FACADE_STUB_2610, M_OPEN_PAGE_2610, CLS_CALLBACK_2610);
    }

    // ----------------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------------

    private static void hookReturnTrueIfPresent(ClassLoader cl, String className,
                                                String methodName, Class<?>... parameterTypes) {
        try {
            Class<?> clazz = XposedHelpers.findClass(className, cl);
            Object[] args = new Object[parameterTypes.length + 1];
            System.arraycopy(parameterTypes, 0, args, 0, parameterTypes.length);
            args[parameterTypes.length] = XC_MethodReplacement.returnConstant(Boolean.TRUE);
            XposedHelpers.findAndHookMethod(clazz, methodName, args);
            log("hooked " + className + "#" + methodName);
        } catch (Throwable t) {
            // Expected for candidates that belong to the other version.
            log("skip " + className + "#" + methodName + " -> " + t);
        }
    }

    private static void hookSdkTargetRegionCheck(ClassLoader cl) {
        try {
            Class<?> impl = XposedHelpers.findClass(CLS_SDK_IMPL, cl);
            Class<?> result = XposedHelpers.findClass(CLS_SDK_RESULT, cl);
            final Constructor<?> ctor = XposedHelpers.findConstructorExact(
                    result, int.class, String.class, HashMap.class);

            XposedHelpers.findAndHookMethod(impl, M_SDK_CHECK, String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            String target = (String) param.args[0];
                            HashMap<String, String> item = new HashMap<String, String>();
                            if (target != null) {
                                item.put("selectedRegionCode", target);
                                item.put("selectedRegionName", target);
                            }
                            param.setResult(ctor.newInstance(0, "success", item));
                        }
                    });
            log("hooked SDK target-region check");
        } catch (Throwable t) {
            log("skip SDK target-region check -> " + t);
        }
    }

    private static void hookRiskControl(ClassLoader cl) {
        final Class<?> resultClass;
        final Object success;
        try {
            Class<?> repo = XposedHelpers.findClass(CLS_REGION_RISK_REPO, cl);
            Class<?> response = XposedHelpers.findClass(
                    "com.heytap.cdo.common.domain.dto.risk.RiskProcessResp", cl);

            // The result wrapper is a.a.a.u3d in 26.7.0 and a.a.a.l3e in
            // 26.10.2. Resolve it from the parse method's real return type.
            Method parse = repo.getDeclaredMethod(M_RISK_PARSE, response);
            resultClass = parse.getReturnType();
            Class<?> successClass = XposedHelpers.findClass(
                    resultClass.getName() + "$c", cl);
            success = XposedHelpers.getStaticObjectField(successClass, M_RESULT_OK);
            if (success == null) {
                log("risk success singleton is null, skip risk hooks");
                return;
            }

            XposedHelpers.findAndHookMethod(repo, M_RISK_PARSE, response,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) {
                            return success;
                        }
                    });
            log("hooked risk parse (" + resultClass.getName() + ")");

            Class<?> callbackClass = XposedHelpers.findClass(CLS_REGION_RISK_CALLBACK, cl);
            XposedHelpers.findAndHookMethod(repo, M_RISK_ASYNC, List.class, callbackClass,
                    new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) {
                            Object callback = param.args[1];
                            try {
                                XposedHelpers.callMethod(callback, "onStart");
                            } catch (Throwable ignored) {
                                // onStart is optional bookkeeping
                            }
                            try {
                                XposedHelpers.callMethod(callback, M_RESULT_OK,
                                        new Class<?>[]{resultClass}, success);
                            } catch (Throwable t) {
                                log("risk callback failed -> " + t);
                            }
                            return null;
                        }
                    });
            log("hooked async risk check");
        } catch (Throwable t) {
            log("skip risk control -> " + t);
        }
    }

    private static void hookFallbackOpenPage(ClassLoader cl, String implClassName,
                                             final String repoOpenMethod,
                                             String callbackClassName) {
        try {
            Class<?> impl = XposedHelpers.findClass(implClassName, cl);
            final Class<?> repo = XposedHelpers.findClass(CLS_REGION_SWITCH_REPO, cl);
            final Class<?> callback = XposedHelpers.findClass(callbackClassName, cl);

            XposedHelpers.findAndHookMethod(impl, "openRegionSelectionPage",
                    Activity.class, callback, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) {
                            try {
                                Object singleton = XposedHelpers.getStaticObjectField(
                                        repo, M_RESULT_OK);
                                XposedHelpers.callMethod(singleton, repoOpenMethod,
                                        new Class<?>[]{Activity.class, callback},
                                        param.args[0], param.args[1]);
                            } catch (Throwable t) {
                                log("fallback openRegionSelectionPage failed -> " + t);
                            }
                            return null;
                        }
                    });
            log("hooked fallback " + implClassName + "#openRegionSelectionPage");
        } catch (Throwable t) {
            log("skip fallback " + implClassName + " -> " + t);
        }
    }

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }
}
