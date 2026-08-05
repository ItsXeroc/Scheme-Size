package scheme;

import arc.Events;
import arc.math.geom.Geometry;
import arc.util.Log;
import arc.util.Strings;
import mindustry.content.Blocks;
import mindustry.game.EventType.*;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Player;
import mindustry.type.Item;
import mindustry.type.StatusEffect;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Floor;
import mindustry.world.blocks.environment.Prop;
import mindustry.world.blocks.environment.StaticWall;
import mindustry.world.blocks.storage.CoreBlock.CoreBuild;
import scheme.tools.DisabledTools;
import scheme.tools.RainbowTeam;

import java.lang.reflect.Field;

import static mindustry.Vars.*;

public class ServerSide {

    public static final String PREFIX = "schemesize.";

    private static int disabledFlags;
    private static boolean registered;

    public static void load() {
        Events.on(ServerLoadEvent.class, e -> register());
        register();
    }

    public static void register() {
        if (netServer == null || registered) return;
        registered = true;

        netServer.addBinaryPacketHandler(PREFIX + "available", (player, data) -> {
            Call.clientBinaryPacketReliable(player.con, PREFIX + "available", DisabledTools.encode(disabledFlags));
        });

        packet("item", (player, args) -> {
            if (args.length < 3 || disabled(DisabledTools.ITEM)) return;
            Team team = team(args[0]);
            Item item = content.item(parseId(args[1]));
            int amount = Strings.parseInt(args[2]);
            if (team == null || item == null || team.core() == null) return;
            team.core().items.add(item, amount);
        });

        packet("unit", (player, args) -> {
            if (args.length < 2 || disabled(DisabledTools.SPAWN)) return;
            Player target = Groups.player.getByID(Strings.parseInt(args[0]));
            UnitType type = content.unit(parseId(args[1]));
            if (target == null || type == null) return;
            if (target.unit() != null) target.unit().spawnedByCore(true);
            target.unit(type.spawn(target.team(), target));
        });

        packet("spawn", (player, args) -> {
            if (args.length < 3 || disabled(DisabledTools.SPAWN)) return;
            Team team = team(args[0]);
            UnitType type = content.unit(parseId(args[1]));
            int amount = Strings.parseInt(args[2]);
            float x = args.length > 3 ? Strings.parseFloat(args[3]) : player.x;
            float y = args.length > 4 ? Strings.parseFloat(args[4]) : player.y;
            if (team == null || type == null) return;
            if (amount == 0) {
                Groups.unit.each(u -> u.team == team && u.type == type, u -> u.spawnedByCore(true));
                return;
            }
            for (int i = 0; i < amount; i++) type.spawn(team, x, y);
        });

        packet("effect", (player, args) -> {
            if (args.length < 3 || disabled(DisabledTools.EFFECT)) return;
            Player target = Groups.player.getByID(Strings.parseInt(args[0]));
            StatusEffect effect = status(parseId(args[1]));
            float duration = Strings.parseFloat(args[2]);
            if (target == null || effect == null || target.unit() == null) return;
            if (duration == 0f) target.unit().unapply(effect);
            else target.unit().apply(effect, duration);
        });

        packet("team", (player, args) -> {
            if (args.length < 2 || disabled(DisabledTools.TEAM)) return;
            Player target = Groups.player.getByID(Strings.parseInt(args[0]));
            Team t = team(args[1]);
            if (target == null || t == null) return;
            RainbowTeam.remove(target);
            target.team(t);
        });

        packet("core", (player, args) -> {
            if (disabled(DisabledTools.CORE)) return;
            int tx = args.length > 0 ? Strings.parseInt(args[0]) : player.tileX();
            int ty = args.length > 1 ? Strings.parseInt(args[1]) : player.tileY();
            Tile tile = world.tile(tx, ty);
            if (tile == null) return;
            tile.setNet(tile.build instanceof CoreBuild ? Blocks.air : Blocks.coreShard, player.team(), 0);
        });

        packet("despawn", (player, args) -> {
            if (disabled(DisabledTools.DESPAWN)) return;
            Player target = args.length > 0 ? Groups.player.getByID(Strings.parseInt(args[0])) : player;
            if (target == null || target.unit() == null) return;
            target.unit().spawnedByCore(true);
            target.clearUnit();
        });

        packet("tp", (player, args) -> {
            if (args.length < 2 || disabled(DisabledTools.TELEPORT)) return;
            float x = Strings.parseFloat(args[0]);
            float y = Strings.parseFloat(args[1]);
            if (player.unit() == null) return;
            boolean spawned = player.unit().spawnedByCore;
            var unit = player.unit();
            unit.spawnedByCore(false);
            player.clearUnit();
            unit.set(x, y);
            Call.setPosition(player.con, x, y);
            Call.setCameraPosition(player.con, x, y);
            player.unit(unit);
            unit.spawnedByCore(spawned);
        });

        packet("fill", (player, args) -> {
            if (args.length < 8 || disabled(DisabledTools.FILL)) return;
            Block block = block(args[0]);
            Block floor = block(args[2]);
            Block overlay = block(args[3]);
            int sx = Strings.parseInt(args[4]);
            int sy = Strings.parseInt(args[5]);
            int w = Strings.parseInt(args[6]);
            int h = Strings.parseInt(args[7]);
            for (int x = sx; x <= sx + w; x++)
                for (int y = sy; y <= sy + h; y++)
                    edit(floor, block, overlay, x, y, player.team());
        });

        packet("brush", (player, args) -> {
            if (args.length < 7 || disabled(DisabledTools.BRUSH)) return;
            Block block = block(args[0]);
            Block floor = block(args[2]);
            Block overlay = block(args[3]);
            int radius = Strings.parseInt(args[4]);
            int cx = Strings.parseInt(args[5]);
            int cy = Strings.parseInt(args[6]);
            Geometry.circle(cx, cy, radius, (x, y) -> edit(floor, block, overlay, x, y, player.team()));
        });

        packet("rule", (player, args) -> {
            if (args.length < 2 || disabled(DisabledTools.RULESETTER)) return;
            setRule(state.rules, args[0], join(args, 1));
            Call.setRules(state.rules);
        });

        packet("teamrule", (player, args) -> {
            if (args.length < 3 || disabled(DisabledTools.RULESETTER)) return;
            Team t = team(args[0]);
            if (t == null) return;
            Rules.TeamRule tr = state.rules.teams.get(t);
            setRule(tr, args[1], join(args, 2));
            Call.setRules(state.rules);
        });

        Log.infoTag("Scheme", "Server-side admin handlers registered.");
    }

    private static boolean disabled(int flag) {
        return (disabledFlags & flag) != 0;
    }

    private static void packet(String name, Handler handler) {
        netServer.addPacketHandler(PREFIX + name, (player, raw) -> {
            try {
                if (!isAdmin(player)) return;
                if (raw == null) raw = "";
                String[] args = raw.trim().isEmpty() ? new String[0] : raw.trim().split("\\s+");
                handler.handle(player, args);
            } catch (Exception e) {
                Log.warn("Invalid @ packet from @: @", PREFIX + name, player.name, e);
            }
        });
    }

    private static boolean isAdmin(Player player) {
        return player != null && player.admin;
    }

    private static Team team(String raw) {
        int tid = Strings.parseInt(raw, -1);
        if (tid < 0 || tid >= Team.all.length) return null;
        return Team.all[tid];
    }

    private static int parseId(String raw) {
        return Strings.parseInt(raw, -1);
    }

    private static StatusEffect status(int sid) {
        if (sid < 0) return null;
        var list = content.statusEffects();
        return sid < list.size ? list.get(sid) : null;
    }

    private static Block block(String raw) {
        if (raw == null || raw.equals("null")) return null;
        int bid = Strings.parseInt(raw, -1);
        return bid < 0 ? null : content.block(bid);
    }

    private static String join(String[] args, int from) {
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) sb.append(' ');
            sb.append(args[i]);
        }
        return sb.toString();
    }

    private static void edit(Block floor, Block block, Block overlay, int x, int y, Team team) {
        Tile tile = world.tile(x, y);
        if (tile == null) return;
        Floor tileFloor = tile.floor();

        if ((floor != null && tile.floor() != floor) || (overlay != null && tile.overlay() != overlay))
            tile.setFloorNet(floor == null ? tileFloor : floor, overlay == null ? tile.overlay() : overlay);

        if (block != null && tile.block() != block) {
            if (block.isFloor() && !block.isOverlay()) tile.setFloorNet(block, tile.overlay());
            else if (block.isOverlay()) tile.setFloorNet(tile.floor(), block);
            else if (block instanceof Prop || block instanceof StaticWall) tile.setNet(block);
            else tile.setNet(block, team, 0);
        }
    }

    private static void setRule(Object target, String name, String value) {
        try {
            Field field = target.getClass().getField(name);
            Class<?> type = field.getType();
            if (type == boolean.class) field.setBoolean(target, Boolean.parseBoolean(value));
            else if (type == float.class) field.setFloat(target, Float.parseFloat(value));
            else if (type == int.class) field.setInt(target, Integer.parseInt(value));
            else field.set(target, value);
        } catch (Exception e) {
            Log.warn("Failed to set rule @ = @: @", name, value, e);
        }
    }

    public static void setDisabledFlags(int flags) {
        disabledFlags = flags;
    }

    private interface Handler {
        void handle(Player player, String[] args);
    }
}
