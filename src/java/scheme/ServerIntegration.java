package scheme;

import arc.Core;
import arc.Events;
import arc.struct.IntMap;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Time;
import mindustry.game.EventType.*;
import mindustry.gen.Call;
import mindustry.io.JsonIO;
import scheme.tools.DisabledTools;

import static arc.Core.*;
import static mindustry.Vars.*;

@SuppressWarnings("unchecked")
public class ServerIntegration {

    public static IntMap<String> SSUsers = new IntMap<>(8);
    public static int hostID = -1;
    public static boolean hasData;
    public static boolean schemeAvailable;

    public static void load() {
        Events.on(PlayerJoin.class, event -> {
            if (event.player == null || event.player.con == null) return;
            Call.clientPacketReliable(event.player.con, "SendMeSubtitle", player == null ? null : String.valueOf(player.id));
        });
        Events.on(PlayerLeave.class, event -> {
            if (event.player != null) SSUsers.remove(event.player.id);
        });

        Runnable registerServer = () -> {
            if (netServer == null) return;
            netServer.addPacketHandler("MySubtitle", (target, args) -> {
                try {
                    if (args != null && args.length() > 256) return;
                    SSUsers.put(target.id, args);
                    IntMap<String> single = new IntMap<>(1);
                    single.put(target.id, args);
                    Call.clientPacketReliable("Subtitles", JsonIO.write(single));

                    if (SSUsers.size > 1) {
                        Call.clientPacketReliable(target.con, "Subtitles", JsonIO.write(SSUsers));
                    }
                } catch (Exception e) {
                    Log.warn("Invalid MySubtitle packet from @", target.name, e);
                }
            });
        };

        registerServer.run();
        Events.on(ServerLoadEvent.class, e -> registerServer.run());

        if (headless || netClient == null) return;

        Events.run(HostEvent.class, ServerIntegration::clear);
        Events.run(ClientPreConnectEvent.class, ServerIntegration::clear);

        netClient.addPacketHandler("SendMeSubtitle", args -> {
            try {
                Call.serverPacketReliable("MySubtitle", settings.getString("subtitle", ""));
                if (args != null) hostID = Strings.parseInt(args, -1);
            } catch (Exception e) {
                Log.warn("Invalid SendMeSubtitle packet", e);
            }
        });

        netClient.addPacketHandler("Subtitles", args -> {
            try {
                if (args == null || args.isEmpty()) return;
                IntMap<String> received = JsonIO.read(IntMap.class, args);
                if (received == null) return;
                for (var entry : received) {
                    if (entry.value == null || entry.value.isEmpty()) SSUsers.remove(entry.key);
                    else SSUsers.put(entry.key, entry.value);
                }
                hasData = true;
            } catch (Exception e) {
                Log.warn("Invalid Subtitles packet", e);
            }
        });

        netClient.addBinaryPacketHandler("schemesize.available", (data) -> {
            try {
                if (data == null || data.length == 0) return;
                schemeAvailable = true;
                DisabledTools.clear();
                DisabledTools.set(data);
                if (settings.getInt("adminsway", scheme.tools.admins.AdminsTools.implementations.length) >= scheme.tools.admins.AdminsTools.implementations.length
                        || settings.getInt("adminsway", 0) == 1) {
                    scheme.SchemeVars.admins = scheme.ui.dialogs.AdminsConfigDialog.detectTools();
                }
            } catch (Exception e) {
                Log.warn("Invalid schemesize.available packet", e);
            }
        });

        Events.on(WorldLoadEndEvent.class, e -> {
            if (!net.client()) DisabledTools.clear();
            initHost();

            Runnable[] task = new Runnable[1];
            task[0] = () -> {
                if (!state.isGame()) {
                    Time.runTask(60f, task[0]);
                    return;
                }
                Core.app.post(() -> Call.serverBinaryPacketReliable("schemesize.available", new byte[]{0}));
            };
            task[0].run();
        });
    }

    public static void clear() {
        SSUsers.clear();
        hostID = -1;
        hasData = false;
        schemeAvailable = false;
        DisabledTools.clear();
    }

    public static void initHost() {
        if (!net.client() && player != null) {
            hasData = true;
            SSUsers.put(player.id, settings.getString("subtitle", ""));
        }
    }

    public static boolean isModded(int id) {
        return SSUsers.containsKey(id) || (player != null && player.id == id);
    }

    public static String type(int id) {
        if (hostID == id) return "trace.type.host";
        if (!hasData && net.client()) return "trace.type.nodata";
        return isModded(id) ? "trace.type.mod" : "trace.type.vanilla";
    }

    public static String tooltip(int id) {
        if (player != null && player.id == id) return "@trace.type.self";
        String sub = SSUsers.get(id);
        return bundle.get(type(id)) + (sub != null ? "\n" + sub : "");
    }
}
