package scheme.tools.admins;

import arc.math.Mathf;
import arc.math.geom.Position;
import arc.struct.Seq;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Player;
import mindustry.world.Block;
import mindustry.world.blocks.environment.Prop;
import mindustry.world.blocks.environment.StaticWall;
import scheme.ServerSide;
import scheme.tools.RainbowTeam;

import static arc.Core.*;
import static mindustry.Vars.*;
import static scheme.SchemeVars.*;

public class SchemeNet implements AdminsTools {

    public String keyName() { return "schemenet"; }

    public void manageRuleBool(boolean value, String name) {
        if (unusable() || isRestricted(RULESETTER)) return;
        send("rule", name, Boolean.toString(value));
    }

    public void manageRuleStr(String value, String name) {
        if (unusable() || isRestricted(RULESETTER)) return;
        send("rule", name, value);
    }

    public void manageTeamRuleBool(int teamId, boolean value, String name) {
        if (unusable() || isRestricted(RULESETTER)) return;
        send("teamrule", teamId, name, Boolean.toString(value));
    }

    public void manageTeamRuleStr(int teamId, String value, String name) {
        if (unusable() || isRestricted(RULESETTER)) return;
        send("teamrule", teamId, name, value);
    }

    public void manageUnit() {
        if (unusable() || isRestricted(SPAWN)) return;
        unit.select(false, true, false, (target, team, type, amount) -> {
            if (!canCreate(team, type)) return;
            send("unit", target.id, type.id);
            units.refresh();
        });
    }

    public void spawnUnits() {
        if (unusable() || isRestricted(SPAWN)) return;
        unit.select(true, true, true, (target, team, type, amount) -> {
            send("spawn", team.id, type.id, amount.intValue(), target.x, target.y);
            units.refresh();
        });
    }

    public void manageEffect() {
        if (unusable() || isRestricted(EFFECT)) return;
        effect.select(true, true, false, (target, team, status, amount) ->
            send("effect", target.id, status.id, amount));
    }

    public void manageItem() {
        if (unusable() || isRestricted(ITEM)) return;
        item.select(true, false, true, (target, team, it, amount) -> {
            if (!hasCore(team)) return;
            send("item", team.id, it.id, fixAmount(it, amount));
        });
    }

    public void manageTeam() {
        if (unusable() || isRestricted(TEAM)) return;
        team.select((target, t) -> {
            if (t != null) {
                RainbowTeam.remove(target);
                send("team", target.id, t.id);
            } else {
                RainbowTeam.add(target, nt -> send("team", target.id, nt.id));
            }
        });
    }

    public void manageTeam(Team t, Player target) {
        if (unusable() || isRestricted(TEAM)) return;
        if (t != null) {
            RainbowTeam.remove(target);
            send("team", target.id, t.id);
        } else {
            RainbowTeam.add(target, nt -> send("team", target.id, nt.id));
        }
    }

    public void placeCore() {
        if (unusable() || isRestricted(CORE)) return;
        send("core", player.tileX(), player.tileY());
    }

    public void despawn(Player target) {
        if (unusable() || isRestricted(DESPAWN)) return;
        send("despawn", target.id);
    }

    public void teleport(Position pos) {
        if (unusable() || isRestricted(TELEPORT)) return;
        send("tp", Mathf.round(pos.getX()), Mathf.round(pos.getY()));
    }

    public void fill(int sx, int sy, int ex, int ey) {
        if (unusable() || isRestricted(FILL)) return;
        tile.select((floor, block, overlay, building) -> {
            Block place = building != null ? building : block;
            send("fill",
                id(place), 0, id(floor), id(overlay),
                sx, sy, ex - sx, ey - sy);
        });
    }

    public void brush(int x, int y, int radius) {
        if (unusable() || isRestricted(BRUSH)) return;
        tile.select((floor, block, overlay, building) -> {
            Block place = building != null ? building : block;
            send("brush",
                id(place), 0, id(floor), id(overlay),
                radius, x, y);
        });
    }

    public void flush(Seq<BuildPlan> plans) {
        if (unusable() || isRestricted(FLUSH)) return;
        var groups = new java.util.LinkedHashMap<String, Seq<int[]>>();
        for (int i = 0; i < plans.size; i++) {
            BuildPlan plan = plans.get(i);
            String blockId, floorId, overlayId;
            if (plan.block.isFloor() && !plan.block.isOverlay()) {
                blockId = "null"; floorId = id(plan.block); overlayId = "null";
            } else if (plan.block instanceof Prop || plan.block instanceof StaticWall) {
                blockId = id(plan.block); floorId = "null"; overlayId = "null";
            } else if (plan.block.isOverlay()) {
                blockId = "null"; floorId = "null"; overlayId = id(plan.block);
            } else {
                blockId = id(plan.block); floorId = "null"; overlayId = "null";
            }
            String key = blockId + " " + floorId + " " + overlayId;
            groups.computeIfAbsent(key, k -> new Seq<>()).add(new int[]{plan.x, plan.y});
        }

        for (var entry : groups.entrySet()) {
            String[] params = entry.getKey().split(" ");
            Seq<int[]> points = entry.getValue();
            points.sort((a, b) -> a[1] != b[1] ? a[1] - b[1] : a[0] - b[0]);

            Seq<int[]> segs = new Seq<>();
            int i = 0;
            while (i < points.size) {
                int sx = points.get(i)[0], sy = points.get(i)[1], ex = sx;
                while (i + 1 < points.size && points.get(i + 1)[1] == sy && points.get(i + 1)[0] == ex + 1) {
                    ex = points.get(++i)[0];
                }
                segs.add(new int[]{sx, sy, ex - sx, 0});
                i++;
            }

            for (int j = 0; j < segs.size; j++) {
                int[] s = segs.get(j);
                for (int k = j + 1; k < segs.size; ) {
                    int[] n = segs.get(k);
                    if (n[0] == s[0] && n[2] == s[2] && n[1] == s[1] + s[3] + 1) {
                        s[3] = n[1] - s[1];
                        segs.remove(k);
                    } else k++;
                }
            }

            for (int j = 0; j < segs.size; j++) {
                int[] s = segs.get(j);
                send("fill", params[0], 0, params[1], params[2], s[0], s[1], s[2], s[3]);
            }
        }
    }

    public boolean unusable() {
        if (!settings.getBool("adminsenabled")) {
            ui.showInfoFade(disabled);
            return true;
        }
        if (!player.admin) {
            ui.showInfoFade("@admins.notanadmin");
            return true;
        }
        return false;
    }

    private static final int FLUSH = scheme.tools.DisabledTools.FLUSH;
    private static final int FILL = scheme.tools.DisabledTools.FILL;
    private static final int BRUSH = scheme.tools.DisabledTools.BRUSH;
    private static final int RULESETTER = scheme.tools.DisabledTools.RULESETTER;
    private static final int DESPAWN = scheme.tools.DisabledTools.DESPAWN;
    private static final int TELEPORT = scheme.tools.DisabledTools.TELEPORT;
    private static final int SPAWN = scheme.tools.DisabledTools.SPAWN;
    private static final int EFFECT = scheme.tools.DisabledTools.EFFECT;
    private static final int ITEM = scheme.tools.DisabledTools.ITEM;
    private static final int TEAM = scheme.tools.DisabledTools.TEAM;
    private static final int CORE = scheme.tools.DisabledTools.CORE;

    private static void send(String command, Object... args) {
        StringBuilder message = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) message.append(' ');
            message.append(args[i]);
        }
        Call.serverPacketReliable(ServerSide.PREFIX + command, message.toString());
    }

    private static String id(Block block) {
        return block == null ? "null" : String.valueOf(block.id);
    }
}
