package scheme.compat;

import arc.files.Fi;
import arc.func.Cons;
import arc.util.Log;

import java.lang.reflect.Method;

import static mindustry.Vars.*;

public final class GameCompat {

    private GameCompat() {}

    public static void openFile(String extension, Cons<Fi> cons) {
        try {
            Object platformObj = platform;
            Class<?> platformClass = platformObj.getClass();

            for (Method method : platformClass.getMethods()) {
                if (!method.getName().equals("showFileChooser")) continue;
                Class<?>[] p = method.getParameterTypes();

                if (p.length == 3 && p[0] == boolean.class && p[1] == String.class && Cons.class.isAssignableFrom(p[2])) {
                    method.invoke(platformObj, true, extension, cons);
                    return;
                }

                if (p.length == 4 && p[0] == boolean.class && p[1] == String.class && p[2] == String.class && Cons.class.isAssignableFrom(p[3])) {
                    method.invoke(platformObj, true, extension, extension, cons);
                    return;
                }
            }

            Class<?> paramsClass = Class.forName("mindustry.ui.FileChooser$FileChooserParams");
            Object params = paramsClass.getConstructor().newInstance();
            trySet(params, "extensions", new String[]{extension});
            trySet(params, "open", true);
            trySet(params, "consumer", cons);
            trySet(params, "result", cons);

            Method open = null;
            try {
                Class<?> chooser = Class.forName("mindustry.ui.FileChooser");
                open = chooser.getMethod("open", String[].class);
                Object builder = open.invoke(null, (Object) new String[]{extension});
                Method submit = builder.getClass().getMethod("submit", Cons.class);
                submit.invoke(builder, cons);
                return;
            } catch (Throwable ignored) {}

            Method show = platformClass.getMethod("showFileChooser", paramsClass);
            show.invoke(platformObj, params);
        } catch (Throwable e) {
            Log.err("Scheme file chooser failed", e);
            ui.showException(e);
        }
    }

    public static void showLabel(String text, int duration, float fade, float x, float y) {
        try {
            Object uiObj = ui;
            for (Method method : uiObj.getClass().getMethods()) {
                if (!method.getName().equals("showLabel")) continue;
                Class<?>[] p = method.getParameterTypes();
                if (p.length == 5) {
                    method.invoke(uiObj, text, duration, fade, x, y);
                    return;
                }
                if (p.length == 6) {
                    method.invoke(uiObj, text, duration, fade, x, y, 0);
                    return;
                }
            }
        } catch (Throwable e) {
            Log.err("Scheme showLabel failed", e);
        }
    }

    private static void trySet(Object target, String field, Object value) {
        try {
            var f = target.getClass().getField(field);
            f.set(target, value);
        } catch (Throwable ignored) {}
    }
}
