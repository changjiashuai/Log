package io.github.changjiashuai.log2file.aspect;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.Trace;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import com.tencent.mars.xlog.Log;
import com.tencent.mars.xlog.Xlog;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.CodeSignature;
import org.aspectj.lang.reflect.MethodSignature;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * 简单记录方法进入和离开的时间差---Hugo
 *
 * Email: changjiashuai@gmail.com
 *
 * Created by CJS on 2017/1/17 17:14.
 */
@Aspect
public class CJSLog {

    private static volatile boolean enabled = true;

    @Pointcut("within(@io.github.changjiashuai.log2file.weaving.DebugLog *)")
    public void withinAnnotatedClass() {
    }

    @Pointcut("execution(* *(..)) && withinAnnotatedClass()")
    public void methodInsideAnnotatedType() {
    }

    @Pointcut("execution(*.new(..)) && withinAnnotatedClass()")
    public void constructorInsideAnnotatedType() {
    }

    @Pointcut("execution(@io.github.changjiashuai.log2file.weaving.DebugLog * *(..)) || methodInsideAnnotatedType()")
    public void method() {
    }

    @Pointcut("execution(@io.github.changjiashuai.log2file.weaving.DebugLog *.new(..)) || constructorInsideAnnotatedType()")
    public void constructor() {
    }

    //是否启用XLogAspect 风格日志 ---启用后除了自己的log 被注解的地方会生成默认的log
    public static void setEnabled(boolean enabled) {
        CJSLog.enabled = enabled;
    }

    //启用XLog文件记录--->可单独启用 自己加log
    public static void initXlog(Context context, boolean isDebug, String logDir) {
        System.loadLibrary("stlport_shared");
        System.loadLibrary("marsxlog");

        //init xlog
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (isDebug) {
                Xlog.setConsoleLogOpen(true);
            } else {
                Xlog.setConsoleLogOpen(false);
            }
            Log.setLogImp(new Xlog());
        } else {
            int pid = android.os.Process.myPid();
            String processName = null;
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningAppProcessInfo appProcess : am.getRunningAppProcesses()) {
                if (appProcess.pid == pid) {
                    processName = appProcess.processName;
                    break;
                }
            }

            final String SDCARD = Environment.getExternalStorageDirectory().getAbsolutePath();
            if (TextUtils.isEmpty(logDir)) {
                logDir = "cjsLog";
            }
            final String logPath = SDCARD + File.separator + logDir + File.separator + "log";

            String logFileName = !processName.contains(":") ? logDir :
                    (logDir + "_" + processName.substring(processName.indexOf(":") + 1));

            if (isDebug) {
                Xlog.appenderOpen(Xlog.LEVEL_VERBOSE, Xlog.AppednerModeAsync, "", logPath, logFileName);
                Xlog.setConsoleLogOpen(true);
            } else {
                Xlog.appenderOpen(Xlog.LEVEL_INFO, Xlog.AppednerModeAsync, "", logPath, logFileName);
                Xlog.setConsoleLogOpen(false);
            }
            Log.setLogImp(new Xlog());
        }
    }

    public static void closeXLog() {
        Log.appenderClose();
    }

    @Around("method() || constructor()")
    public Object logAndExecute(ProceedingJoinPoint joinPoint) throws Throwable {
        enterMethod(joinPoint);

        long startNanos = System.nanoTime();
        Object result = joinPoint.proceed();
        long stopNanos = System.nanoTime();
        long lengthMillis = TimeUnit.NANOSECONDS.toMillis(stopNanos - startNanos);

        exitMethod(joinPoint, result, lengthMillis);

        return result;
    }

    private static void enterMethod(JoinPoint joinPoint) {
        if (!enabled) return;

        CodeSignature codeSignature = (CodeSignature) joinPoint.getSignature();

        Class<?> cls = codeSignature.getDeclaringType();
        String methodName = codeSignature.getName();
        String[] parameterNames = codeSignature.getParameterNames();
        Object[] parameterValues = joinPoint.getArgs();

        StringBuilder builder = new StringBuilder("\u21E2 ");
        builder.append(methodName).append('(');
        for (int i = 0; i < parameterValues.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(parameterNames[i]).append('=');
            builder.append(Strings.toString(parameterValues[i]));
        }
        builder.append(')');

        if (Looper.myLooper() != Looper.getMainLooper()) {
            builder.append(" [Thread:\"").append(Thread.currentThread().getName()).append("\"]");
        }

        Log.v(asTag(cls), builder.toString());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            final String section = builder.toString().substring(2);
            Trace.beginSection(section);
        }
    }

    private static void exitMethod(JoinPoint joinPoint, Object result, long lengthMillis) {
        if (!enabled) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            Trace.endSection();
        }

        Signature signature = joinPoint.getSignature();

        Class<?> cls = signature.getDeclaringType();
        String methodName = signature.getName();
        boolean hasReturnType = signature instanceof MethodSignature
                && ((MethodSignature) signature).getReturnType() != void.class;

        StringBuilder builder = new StringBuilder("\u21E0 ")
                .append(methodName)
                .append(" [")
                .append(lengthMillis)
                .append("ms]");

        if (hasReturnType) {
            builder.append(" = ");
            builder.append(Strings.toString(result));
        }

        Log.v(asTag(cls), builder.toString());
    }

    private static String asTag(Class<?> cls) {
        if (cls.isAnonymousClass()) {
            return asTag(cls.getEnclosingClass());
        }
        return cls.getSimpleName();
    }
}