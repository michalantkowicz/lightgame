package com.mantkowicz.light.feature;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.mantkowicz.light.actor.Bobber;
import com.mantkowicz.light.actor.Fish;
import com.mantkowicz.light.actor.FishType;
import com.mantkowicz.light.actor.LookAtMeEvent;
import com.mantkowicz.light.actor.Status;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.TileContent;
import com.mantkowicz.light.board.WorldScale;
import com.mantkowicz.light.character.Player;
import com.mantkowicz.light.context.GameContext;
import com.mantkowicz.light.map.TileFishingSpot;
import com.mantkowicz.light.resources.Action;
import com.mantkowicz.light.resources.Direction;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.ui.collect.ItemThrower;
import com.mantkowicz.light.ui.fishing.BitePrompt;
import com.mantkowicz.light.ui.fishing.RhythmGame;
import com.mantkowicz.light.ui.fishing.RhythmRules;
import com.mantkowicz.light.ui.hud.Hud;
import com.mantkowicz.light.ui.hud.Portrait;
import com.mantkowicz.light.ui.hud.ToolButton;

import java.util.HashSet;
import java.util.Set;

/**
 * Fishing ({@link com.mantkowicz.light.debug.FeatureFlag#FISHING}) from the map's fishing spots (Tiled's FISHING_SPOT,
 * {@link TileFishingSpot}):
 * <ol>
 * <li>Standing still on a fishing spot, the rabbit has the fishing rod on the HUD's tool button.</li>
 * <li>Tapped, it casts (to the left, the only way it fishes): the bobber drops into the water {@link #CAST_TILES}
 * tiles away with a splash and bobs for a while ({@link #LEAST_WAIT_SECONDS} .. {@link #MOST_WAIT_SECONDS}, at random). Now and then
 * ({@link #NO_FISH_CHANCE}) no fish comes: it bobs {@link #MOST_WAIT_SECONDS}, the line comes out and the face is
 * sad, as when a fish gets away.</li>
 * <li>A fish bites: the bobber is pulled under with a splash, the rabbit's rod shakes, the face says "!!!" (happy),
 * and a paw points at the place, pulsing, with a circle emptying next to it ({@link BitePrompt}).</li>
 * <li>A tap anywhere on the screen within {@link #BITE_SECONDS} hooks it and the fishing game opens
 * ({@link RhythmGame}: fish down three lanes, catch them with the nets on the line): the face is happy at each catch
 * and sad at each miss. Won: the rabbit pulls the fish out, the
 * face puts its sunglasses on, and a fish (of a random colour) goes into the backpack (or, full, next to the
 * rabbit; with no free tile there either, it isn't collected). Lost, or no tap in time: the face is sad and the rabbit can cast again.</li>
 * </ol>
 * Casting centres the camera on the rabbit (so the bobber and the paw are on the screen). Walking off while casting
 * or waiting takes the line out of the water. How hard the fishing game is: {@link #LEVEL} ({@link RhythmRules}).
 * <p>
 * Only on the map the player is in ({@link PlayerFeature}); a map can switch it off (map property {@code featuresOff}
 * with {@code FISHING}). When the player goes to another map, the line comes out of the water and the bite's paw and
 * the fishing game's window go from the shared UI ({@link #playerLeft()}).
 */
public class FishingFeature implements PlayerFeature {
    /** How hard the fishing game is, 0 .. 1, unless set ({@link #setLevel}): 0.15 suits a 3-year-old, 0.8 an 8. */
    public static final float LEVEL = 0.15f;
    /** How hard the fishing game is now (later from the age setting), and its rules from that. */
    private static float level = LEVEL;
    private static RhythmRules rules = RhythmRules.forLevel(LEVEL);
    /** How often no fish comes to a cast, 0 .. 1. */
    public static final float NO_FISH_CHANCE = 0.3f;
    private static float noFishChance = NO_FISH_CHANCE;
    /** The bobber lands this many tiles to the left of the rabbit. */
    public static final int CAST_TILES = 2;
    /** The bobber bobs this long at least, and at most, before a fish takes it (whole bobs, at random in between). */
    public static final float LEAST_WAIT_SECONDS = 3, MOST_WAIT_SECONDS = 10;
    /** After a bite the player has this long to tap. */
    public static final float BITE_SECONDS = 1.2f;
    /** What the face says when a fish bites. */
    public static final String BITE = "!!!";
    /** The rod's name on the tool button. */
    public static final String ROD = "fishing rod";
    /** How long the face is sad, and wears its sunglasses after its put them on. */
    static final float SAD_SECONDS = 2, COOL_SECONDS = 2.5f;

    /** Where the fishing is at. */
    public enum Phase {
        READY, CASTING, WAITING, BITE, GAME, LANDING
    }

    private final GameContext context;
    private final Player player;
    private final Hud hud;
    private final ResourcesService resources;
    private final ItemThrower thrower;
    private final Set<Tile> spots = new HashSet<>();
    private Phase phase = Phase.READY;
    private float timer, coolIn = -1;
    private Bobber bobber;
    private BitePrompt prompt;
    private RhythmGame game;
    private Vector2 bitePlace;

    public FishingFeature(GameContext context, Player player, Hud hud) {
        this.context = context;
        this.player = player;
        this.hud = hud;
        this.resources = context.getResourcesService();
        this.thrower = new ItemThrower(context);
        for (Tile tile : context.getBoardService().getTiles()) {
            for (TileContent content : tile.getTileObjects()) {
                if (content instanceof TileFishingSpot) {
                    spots.add(tile);
                }
            }
        }
    }

    /** How hard the fishing game is from now on, 0 .. 1 (for the age setting to come, and tests). */
    public static void setLevel(float newLevel) {
        level = newLevel;
        rules = RhythmRules.forLevel(newLevel);
    }

    /** The fishing game's rules from now on, each one set by hand (e.g. more fish, or two lanes). */
    public static void setRules(RhythmRules newRules) {
        rules = newRules;
    }

    /** How often no fish comes to a cast from now on, 0 .. 1 ({@link #NO_FISH_CHANCE} as the game ships). */
    public static void setNoFishChance(float chance) {
        noFishChance = chance;
    }

    public static float getLevel() {
        return level;
    }

    @Override
    public void run(float delta) {
        if ((phase == Phase.CASTING || phase == Phase.WAITING) && Status.MOVEMENT.equals(player.getStatus())) {
            reelIn(); // walked off: the line comes out of the water
        }
        tick(delta);
        showRod();
    }

    /** Gone to another map: the line out of the water, the paw and the fishing game gone, the rod put away. */
    @Override
    public void playerLeft() {
        reelIn();
        if (prompt != null) {
            prompt.remove();
            prompt = null;
        }
        if (game != null) {
            game.remove();
            game = null;
        }
        coolIn = -1;
        if (rodShown()) {
            hud.getToolButton().resetTool();
        }
    }

    /** The cast lands, a caught fish is out of the water, the face's sunglasses are on. */
    private void tick(float delta) {
        if (timer > 0) {
            timer -= delta;
            if (timer <= 0 && phase == Phase.CASTING) {
                castLanded();
            } else if (timer <= 0 && phase == Phase.LANDING) {
                player.stopFishing();
                phase = Phase.READY;
            }
        }
        if (coolIn > 0) {
            coolIn -= delta;
            if (coolIn <= 0) {
                face().play(UiImage.TEEMO_COOL, COOL_SECONDS);
            }
        }
    }

    /**
     * Standing still on a fishing spot, and all the time it fishes there: the rod on the tool button (tapped while
     * it fishes, nothing happens); otherwise the hint again. Only over the hint, as every tool.
     */
    private void showRod() {
        boolean rod = isOnSpot() && (phase != Phase.READY || Status.IDLE.equals(player.getStatus()));
        if (rod && ToolButton.HINT.equals(hud.getToolButton().getToolName())) {
            hud.getToolButton().setTool(ROD,
                    resources.getAnimation(Sprite.FISHING_ROD_ICON, Action.ICON).getKeyFrame(0), this::cast);
        } else if (!rod && rodShown()) {
            hud.getToolButton().resetTool();
        }
    }

    /** Is the rod on the tool button now? */
    private boolean rodShown() {
        return ROD.equals(hud.getToolButton().getToolName());
    }

    private boolean isOnSpot() {
        return player.getTile() != null && spots.contains(player.getTile());
    }

    /** The rod tapped: the rabbit casts. */
    private void cast() {
        if (phase != Phase.READY || !isOnSpot()) {
            return;
        }
        phase = Phase.CASTING;
        player.fish(Action.CAST);
        // the camera centres on the rabbit (the bobber, two tiles left, and the paw are on the screen), but not so
        // far that it shows beyond the map's left edge
        context.getGameEventService().post(new LookAtMeEvent(player, cameraPoint()));
        timer = resources
                .getAnimation(Sprite.RABBIT_FISHING, Action.CAST, Direction.LEFT)
                .getAnimationDuration();
    }

    /** The cast is over: the bobber drops into the water, bobs a while, and a fish takes it (or none comes). */
    private void castLanded() {
        Vector2 middle = player.getCenter().sub(CAST_TILES * player.getTile().getWidth(), 0);
        bobber = new Bobber(resources, middle);
        bobber.lineFrom(rodTip());
        context.getGameStage().addActor(bobber);
        player.fish(Action.WAIT);
        phase = Phase.WAITING;
        float bobSeconds = resources.getAnimation(Sprite.FISHING_BOBBER, Action.BOB).getAnimationDuration();
        if (MathUtils.random() < noFishChance) {
            int bobs = Math.max(1, Math.round(MOST_WAIT_SECONDS / bobSeconds));
            bobber.dropIn(() -> bobber.bob(bobs, this::noFish));
            return;
        }
        int bobs = Math.max(1, Math.round(MathUtils.random(LEAST_WAIT_SECONDS, MOST_WAIT_SECONDS) / bobSeconds));
        bobber.dropIn(() -> bobber.bob(bobs, () -> bobber.pullUnder(this::bitten)));
    }

    /**
     * The rabbit's middle, moved right as far as needed so the view doesn't reach past the map's left edge (the bobber
     * two tiles left of it stays on the screen: a view is wider than three tiles).
     */
    private Vector2 cameraPoint() {
        Vector2 point = player.getCenter();
        float left = Float.MAX_VALUE;
        for (Tile tile : context.getBoardService().getTiles()) {
            left = Math.min(left, tile.getCenter().x - tile.getWidth() / 2f);
        }
        OrthographicCamera camera = context.getCamera();
        float halfView = camera.viewportWidth * camera.zoom / 2f;
        point.x = Math.max(point.x, left + halfView);
        return point;
    }

    /** Where the rod's tip is while the rabbit waits: 16 art pixels left of its picture's middle, 1 above. */
    private Vector2 rodTip() {
        return player.getCenter().add(-16 * WorldScale.PIXEL_SCALE, WorldScale.PIXEL_SCALE / 2f);
    }

    /** The bobber is gone under: the rod shakes, "!!!", and the paw shows where to tap. */
    private void bitten() {
        if (phase != Phase.WAITING) {
            return;
        }
        phase = Phase.BITE;
        bitePlace = bobber.getMiddle();
        bobber.remove();
        bobber = null;
        player.fish(Action.BITE);
        hud.say(BITE, null, BITE_SECONDS);
        face().play(UiImage.TEEMO_HAPPY, BITE_SECONDS);
        Stage ui = context.getUiStage();
        Vector2 target = ui.screenToStageCoordinates(context.getGameStage().stageToScreenCoordinates(bitePlace.cpy()));
        prompt = new BitePrompt(resources, ui, target, BITE_SECONDS,
                this::hooked, this::gotAway);
        ui.addActor(prompt);
    }

    /** It bobbed the longest wait and no fish came: the line comes out, the face is sad (as when one gets away). */
    private void noFish() {
        if (phase != Phase.WAITING) {
            return;
        }
        reelIn();
        face().play(UiImage.TEEMO_SAD, SAD_SECONDS);
    }

    /** Tapped in time: the fishing game. */
    private void hooked() {
        prompt = null;
        phase = Phase.GAME;
        Stage ui = context.getUiStage();
        game = new RhythmGame(resources, ui, rules, new RhythmGame.Listener() {
            @Override
            public void caught() {
                face().play(UiImage.TEEMO_HAPPY);
            }

            @Override
            public void missed() {
                face().play(UiImage.TEEMO_SAD);
            }

            @Override
            public void over(boolean won) {
                landed(won);
            }
        });
        ui.addActor(game);
    }

    /** No tap in time: the fish is gone. */
    private void gotAway() {
        prompt = null;
        face().play(UiImage.TEEMO_SAD, SAD_SECONDS);
        player.stopFishing();
        phase = Phase.READY;
    }

    /** The fishing game is over: a fish (won) or a sad face. */
    private void landed(boolean won) {
        game = null;
        if (!won) {
            face().play(UiImage.TEEMO_SAD, SAD_SECONDS);
            player.stopFishing();
            phase = Phase.READY;
            return;
        }
        phase = Phase.LANDING;
        player.fish(Action.PULL);
        timer = resources
                .getAnimation(Sprite.RABBIT_FISHING, Action.PULL, Direction.LEFT)
                .getAnimationDuration() + 0.3f;
        face().play(UiImage.TEEMO_COOL_ON);
        coolIn = resources.getUiAnimation(UiImage.TEEMO_COOL_ON).getAnimationDuration();
        Fish fish = new Fish(FishType.values()[MathUtils.random(FishType.values().length - 1)], resources);
        if (!player.getInventory().add(fish)) {
            // the backpack is full: next to the rabbit; with no free tile there either, it isn't collected
            thrower.throwOut(fish, player.getTile(), player.getCenter());
        }
    }

    /** Walked off while casting or waiting: no bobber, no pose. */
    private void reelIn() {
        if (bobber != null) {
            bobber.remove();
            bobber = null;
        }
        player.stopFishing();
        phase = Phase.READY;
        timer = 0;
    }

    private Portrait face() {
        return hud.getFace();
    }

    // for tests

    public Phase getPhase() {
        return phase;
    }

    public Bobber getBobber() {
        return bobber;
    }

    public BitePrompt getPrompt() {
        return prompt;
    }

    public RhythmGame getGame() {
        return game;
    }

    public Set<Tile> getSpots() {
        return spots;
    }
}
