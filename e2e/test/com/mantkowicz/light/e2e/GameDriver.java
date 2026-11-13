package com.mantkowicz.light.e2e;

import box2dLight.Light;
import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.mantkowicz.light.actor.Carrot;
import com.mantkowicz.light.actor.FloatingPicture;
import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.actor.Inventory;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.actor.ItemOnGround;
import com.mantkowicz.light.actor.PictureBubble;
import com.mantkowicz.light.board.Board;
import com.mantkowicz.light.board.BoardService;
import com.mantkowicz.light.board.Tile;
import com.mantkowicz.light.board.WallTile;
import com.mantkowicz.light.character.Chest;
import com.mantkowicz.light.character.Cow;
import com.mantkowicz.light.character.Dog;
import com.mantkowicz.light.character.Player;
import com.mantkowicz.light.character.SpriteActor;
import com.mantkowicz.light.debug.Debug;
import com.mantkowicz.light.e2e.headless.HeadlessGame;
import com.mantkowicz.light.event.GameEventService;
import com.mantkowicz.light.game.Main;
import com.mantkowicz.light.map.TileDog;
import com.mantkowicz.light.map.TileLight;
import com.mantkowicz.light.map.TileObject;
import com.mantkowicz.light.map.TilePlayer;
import com.mantkowicz.light.map.TileSprite;
import com.mantkowicz.light.map.TileSpriteActor;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.Sprite;
import com.mantkowicz.light.resources.SpriteSheet;
import com.mantkowicz.light.resources.UiFont;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.ui.hud.ActionButton;
import com.mantkowicz.light.ui.hud.Hud;
import com.mantkowicz.light.ui.hud.Portrait;
import com.mantkowicz.light.ui.hud.SettingsPanel;
import com.mantkowicz.light.ui.hud.SpeechBubble;
import com.mantkowicz.light.ui.hud.StaminaBar;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Plays the game like a player would: clicks tiles and actors, holds, drags, presses keys, waits.
 * Observes only what a player could see. This is the ONLY test class that knows the game's classes;
 * when the game is refactored, only this class may need (mechanical) changes - scenarios and goldens must not.
 */
public final class GameDriver {
    /** Geometry of every map: a square grid of 128 px cells. */
    private static final float CELL_SIZE = 128;
    private static final int START_FRAMES = 30;
    private static final long RANDOM_SEED = 7;
    private static final int FRAMES_PER_SECOND = 60;

    public enum Kind {
        PLAYER, DOG, CHEST, COW
    }

    private final HeadlessGame game;
    private final StringBuilder timeline = new StringBuilder();
    private String crash;
    private Vector2 pointer = new Vector2();

    private GameDriver(String mapName, float cameraZoom) {
        game = new HeadlessGame(mapName, cameraZoom);
    }

    /** Starts the game on the original house map (map.tmx) and lets it settle for half a second. Tests use a {@link TestMap} instead. */
    public static GameDriver start() {
        return start(HeadlessGame.TEST_MAP);
    }

    /** The map the real game starts with (Main.DEFAULT_MAP). */
    public static String defaultMap() {
        return Main.DEFAULT_MAP;
    }

    /** Starts the game on a map (a {@link TestMap}, or a shipped one like "maps/level1.tmx") and lets it settle for half a second. */
    public static GameDriver start(String mapName) {
        return start(mapName, HeadlessGame.TEST_CAMERA_ZOOM);
    }

    /** Starts the game on the test map with another camera zoom (0.5 = everything in the world twice as big). */
    public static GameDriver startZoomed(float cameraZoom) {
        return start(HeadlessGame.TEST_MAP, cameraZoom);
    }

    /** Starts the game on a map (e.g. a {@link TestMap}) with another camera zoom. */
    public static GameDriver startZoomed(String mapName, float cameraZoom) {
        return start(mapName, cameraZoom);
    }

    private static GameDriver start(String mapName, float cameraZoom) {
        MathUtils.random.setSeed(RANDOM_SEED); // the game's random choices (where a thrown item lands) repeat every run
        GameDriver driver = new GameDriver(mapName, cameraZoom);
        driver.frames(START_FRAMES);
        return driver;
    }

    // ---------------------------------------------------------------------------------------------- time

    public void frames(int count) {
        for (int i = 0; i < count && crash == null; i++) {
            try {
                game.frame();
            } catch (RuntimeException e) {
                crash = e.getClass().getName() + ": " + e.getMessage();
            }
        }
    }

    public void seconds(float seconds) {
        frames(Math.round(seconds * FRAMES_PER_SECOND));
    }

    // ---------------------------------------------------------------------------------------------- actions

    /** Short click (press, one frame, release) on the centre of a cell - with or without a tile on it. */
    public void click(Cell cell) {
        tap(gameScreenPosition(cellCenter(cell)));
    }

    /** Short click on the centre of an actor. */
    public void click(Kind kind) {
        tap(gameScreenPosition(actor(kind).getCenter()));
    }

    /** Presses on an actor and keeps holding; follow with {@link #seconds} and a release. */
    public void press(Kind kind) {
        movePointer(gameScreenPosition(actor(kind).getCenter()));
        inputProcessor().touchDown((int) pointer.x, (int) pointer.y, 0, 0);
    }

    /** While holding, drags the pointer onto a radial menu button. */
    public void dragToMenuButton(int index) {
        dragTo(menuScreenPosition(menuButtons().get(index)));
    }

    /** While holding, drags the pointer onto a tile. */
    public void dragTo(Cell cell) {
        dragTo(gameScreenPosition(cellCenter(cell)));
    }

    /** Short click on a UI button (COLLECT, OK, CANCEL, ...) by its text. */
    public void clickButton(String text) {
        for (Actor actor : uiActors()) {
            if (isButton(actor, text)) {
                tap(uiScreenPosition(actor));
                return;
            }
        }
        throw new IllegalStateException("No \"" + text + "\" button on screen");
    }

    /** A button with this text, or the action button (a picture, no text) named so, e.g. "COLLECT". */
    private static boolean isButton(Actor actor, String text) {
        return actor instanceof TextButton button && text.contentEquals(button.getText())
                || actor instanceof ActionButton && text.equals(actor.getName());
    }

    // ---------------------------------------------------------------------------------------------- the HUD

    /**
     * What the HUD shows, one line each, positions in UI pixels (800 wide, y up): the face, the backpack, the
     * stamina bar, the hint button, the bubble over the face, the gear and how full its hold circle is, whether the
     * settings window is open.
     */
    public String describeHud() {
        Hud hud = hud();
        Snapshot snapshot = new Snapshot();
        Portrait face = hud.getFace();
        snapshot.line("face: %s %s %s%s", face.getPictureName(), uiBox(face), face.getIdle().name(),
                face.getReaction() == null ? "" : " playing " + face.getReaction());
        List<String> backpack = new ArrayList<>();
        for (Item item : hud.getBackpack()) {
            backpack.add(item == null ? "-" : item.getClass().getSimpleName());
        }
        snapshot.line("backpack: %s %s", backpack, uiBox(hud.getRow().getChildren().get(1)));
        StaminaBar stamina = hud.getStamina();
        snapshot.line("stamina: %d of %d segments, face %s %s", stamina.getFullSegments(), stamina.getSegments(),
                stamina.getMood(), uiBox(stamina));
        snapshot.line("hint: %s", uiBox(hud.getHint()));
        SpeechBubble bubble = hud.getBubble();
        snapshot.line("bubble: %s", bubble == null
                ? "none"
                : "\"" + bubble.getText() + "\" " + uiBox(bubble)
                        + " alpha=" + Snapshot.number(bubble.getColor().a));
        snapshot.line("gear: %s held=%d%%", uiBox(hud.getGear()), Math.round(hud.getGear().getProgress() * 100));
        snapshot.line("settings: %s", hud.getSettings() == null ? "closed" : "open");
        return snapshot.toString();
    }

    /** Presses the settings gear and keeps holding; follow with {@link #seconds} and {@link #release}. */
    public void pressGear() {
        movePointer(uiScreenPosition(hud().getGear()));
        inputProcessor().touchDown((int) pointer.x, (int) pointer.y, 0, 0);
    }

    /**
     * Makes the HUD's face play an animation for a while instead of its idle, as game code does (e.g. cheering while it
     * says something); null seconds: once.
     *
     * @param animation a {@code UiImage} name, e.g. "TEEMO_CHEER"
     */
    public void faceReacts(String animation, Float seconds) {
        if (seconds == null) {
            hud().getFace().play(UiImage.valueOf(animation));
        } else {
            hud().getFace().play(UiImage.valueOf(animation), seconds);
        }
    }

    /** What the HUD's face plays instead of its idle ("TEEMO_CHEER"), or "idle". */
    public String faceReaction() {
        UiImage reaction = hud().getFace().getReaction();
        return reaction == null ? "idle" : reaction.name();
    }

    /** Takes some of the player's stamina away, as time passing or hard work does (0..1, 1 = all of it). */
    public void useStamina(float amount) {
        ((Player) actor(Kind.PLAYER)).getStamina().use(amount);
    }

    /** Fills the player's stamina again, as sleeping does. */
    public void fillStamina() {
        ((Player) actor(Kind.PLAYER)).getStamina().fill();
    }

    /** Short tap on the hint button (the question mark right of the face). */
    public void tapHint() {
        tap(uiScreenPosition(hud().getHint()));
    }

    /** Short tap on the player's face in the HUD. */
    public void tapFace() {
        tap(uiScreenPosition(hud().getFace()));
    }

    /** Taps the check mark of the open settings window. */
    public void closeSettings() {
        SettingsPanel settings = hud().getSettings();
        if (settings == null) {
            throw new IllegalStateException("The settings window isn't open");
        }
        tap(uiScreenPosition(settings.getCloseButton()));
    }

    private Hud hud() {
        for (Actor actor : uiActors()) {
            if (actor instanceof Hud hud) {
                return hud;
            }
        }
        throw new IllegalStateException("No HUD on screen");
    }

    /** "at=(x,y) size=WxH" of a UI actor, in UI stage coordinates. */
    private static String uiBox(Actor actor) {
        Vector2 corner = actor.localToStageCoordinates(new Vector2());
        return "at=" + Snapshot.point(corner.x, corner.y) + " size=" + Snapshot.number(actor.getWidth()) + "x"
                + Snapshot.number(actor.getHeight());
    }

    /** The effective alpha of every UI button with this text, e.g. [1.0] for one fully shown COLLECT button. */
    public List<Float> buttonAlphas(String text) {
        List<Float> alphas = new ArrayList<>();
        for (Actor actor : uiActors()) {
            if (isButton(actor, text)) {
                alphas.add(effectiveAlpha(actor));
            }
        }
        return alphas;
    }

    /** Puts a wall tile on a cell, as if the map had one there (the map has no walls; WallTile only builds its physics body). */
    public void addWall(Cell cell) {
        ResourcesService resources = screenField("resourcesService");
        World world = screenField("world");
        WallTile wall = new WallTile(resources);
        wall.setPosition(cell.col() * CELL_SIZE, cell.row() * CELL_SIZE);
        wall.prepare(world);
        frames(1);
    }

    /** Would a light standing on one cell reach the centre of another? (A temporary light, removed afterwards.) */
    public boolean lightReaches(Cell from, Cell to) {
        Vector2 source = cellCenter(from);
        Vector2 target = cellCenter(to);
        PointLight light = new PointLight(screenField("rayHandler"), 128, Color.WHITE, source.dst(target) * 2, source.x,
                source.y);
        try {
            light.update();
            return light.contains(target.x, target.y);
        } finally {
            light.remove();
        }
    }

    /** Releases the pointer where it currently is. */
    public void release() {
        inputProcessor().touchUp((int) pointer.x, (int) pointer.y, 0, 0);
        frames(1);
    }

    /** Presses and releases a key; the game sees it as "just pressed" during the next frame. */
    public void pressKey(int key) {
        game.pressKey(key);
        frames(1);
    }

    /** Like resizing the desktop window. */
    public void resize(int width, int height) {
        game.resize(width, height);
        frames(1);
    }

    /** Closes the game; records a crash if disposing fails. */
    public void stop() {
        try {
            game.dispose();
        } catch (RuntimeException e) {
            crash = "on dispose: " + e.getClass().getName() + ": " + e.getMessage();
        }
    }

    private void tap(Vector2 screenPosition) {
        movePointer(screenPosition);
        inputProcessor().touchDown((int) pointer.x, (int) pointer.y, 0, 0);
        frames(1);
        inputProcessor().touchUp((int) pointer.x, (int) pointer.y, 0, 0);
        frames(1);
    }

    private void dragTo(Vector2 screenPosition) {
        movePointer(screenPosition);
        inputProcessor().touchDragged((int) pointer.x, (int) pointer.y, 0);
        frames(1);
    }

    /** The game also polls the pointer position (the menu checks what is under it on release). */
    private void movePointer(Vector2 screenPosition) {
        pointer = screenPosition;
        game.movePointer((int) pointer.x, (int) pointer.y);
    }

    // ---------------------------------------------------------------------------------------------- observations

    /** Records everything a player could see right now under the given name and returns it. */
    public String snapshot(String moment) {
        Snapshot snapshot = new Snapshot();
        snapshot.line("crash: %s", crash == null ? "none" : crash);
        if (crash == null) {
            describeScene(snapshot);
        }
        note("## " + moment + "\n" + snapshot);
        return snapshot.toString();
    }

    /** Adds a free-form line to the timeline (for per-frame traces). */
    public void note(String line) {
        timeline.append(line).append(line.endsWith("\n") ? "" : "\n");
    }

    /** All snapshots and notes so far, in order: this is what golden files contain. */
    public String timeline() {
        return timeline.toString();
    }

    /** What the player and the chest carry, slot by slot ("-" is an empty slot). */
    public String inventories() {
        Player player = (Player) actor(Kind.PLAYER);
        Chest chest = (Chest) actor(Kind.CHEST);
        return "player=" + describe(player.getInventory()) + " chest=" + describe(chest.getInventory());
    }

    /** Items lying on the ground, e.g. ["Carrot cell=(3,5)"], or "flying" while thrown; in the order they were thrown. */
    public List<String> itemsOnGround() {
        List<String> items = new ArrayList<>();
        for (Actor actor : gameStage().getActors()) {
            if (actor instanceof ItemOnGround lying) {
                items.add(lying.getItem().getClass().getSimpleName()
                        + (lying.isLanded() ? " cell=" + cellOf(lying.getTile()) : " flying"));
            }
        }
        return items;
    }

    /** Puts a new carrot into the player's backpack (its first free slot), as if picked up somewhere. */
    public void putCarrotIntoBackpack() {
        ResourcesService resources = screenField("resourcesService");
        if (!((Player) actor(Kind.PLAYER)).getInventory().add(new Carrot(resources))) {
            throw new IllegalStateException("The backpack is full");
        }
    }

    /** The backpack's items themselves (null for a free slot), to tell which one went where. */
    public List<Object> backpackItems() {
        return new ArrayList<>(((Player) actor(Kind.PLAYER)).getInventory().getItems());
    }

    /** The items lying on the ground (or flying) themselves, in the order they were thrown. */
    public List<Object> itemsOnGroundThemselves() {
        List<Object> items = new ArrayList<>();
        for (Actor actor : gameStage().getActors()) {
            if (actor instanceof ItemOnGround lying) {
                items.add(lying.getItem());
            }
        }
        return items;
    }

    /** Where the first item lying on the ground is (it must have landed). */
    public Cell itemCell() {
        for (Actor actor : gameStage().getActors()) {
            if (actor instanceof ItemOnGround lying && lying.isLanded()) {
                return cellOf(lying.getTile());
            }
        }
        throw new IllegalStateException("No item lying on the ground");
    }

    /** How many stars (popping out of an opened chest) are on screen. */
    public int starsOnScreen() {
        int stars = 0;
        TextureRegion star = ((ResourcesService) screenField("resourcesService")).getUiImage(UiImage.SPARKLE);
        for (Actor actor : gameStage().getActors()) {
            if (actor instanceof Image image && image.getDrawable() instanceof TextureRegionDrawable drawable
                    && drawable.getRegion().getTexture() == star.getTexture()
                    && drawable.getRegion().getRegionX() == star.getRegionX()
                    && drawable.getRegion().getRegionY() == star.getRegionY()) {
                stars++;
            }
        }
        return stars;
    }

    /** Is a backpack slot of the HUD shaking (its item jumped out by itself)? */
    public boolean backpackSlotShaking(int slot) {
        return hud().getSlots().get(slot).hasActions();
    }

    /** Short tap on a backpack slot of the HUD (0 = the left one). */
    public void tapBackpackSlot(int slot) {
        tap(uiScreenPosition(hud().getSlots().get(slot)));
    }

    /** Does any actor on the UI stage draw scene2d debug lines? */
    public boolean uiDebugLinesShown() {
        for (Actor actor : uiActors()) {
            if (actor.getDebug()) {
                return true;
            }
        }
        return false;
    }

    private static String describe(Inventory inventory) {
        List<String> slots = new ArrayList<>();
        for (Item item : inventory.getItems()) {
            slots.add(item == null ? "-" : item.getClass().getSimpleName());
        }
        return slots.toString();
    }

    /** Everything the board draws in one frame, in drawing order: picture name and position. */
    public String describeBoardDrawing() {
        Snapshot snapshot = new Snapshot();
        SpriteBatch recorder = new SpriteBatch() {
            @Override
            public void draw(TextureRegion region, float x, float y) {
                snapshot.line("%s at=%s", regionName(region), Snapshot.point(x, y));
            }

            @Override
            public void draw(TextureRegion region, float x, float y, float width, float height) {
                draw(region, x, y);
            }

            @Override
            public void draw(TextureRegion region, float x, float y, float originX, float originY, float width,
                    float height, float scaleX, float scaleY, float rotation) {
                draw(region, x, y);
            }
        };
        try {
            board().draw(recorder, 1);
        } finally {
            recorder.dispose();
        }
        return snapshot.toString();
    }

    /**
     * What a cell draws, bottom layer first: the map tile of each layer as "tileset#id", then which corner of that
     * tile's picture lands on the cell's bottom-left, top-left, top-right and bottom-right corners (BL,TL,TR,BR when
     * drawn as it is; mirrored or rotated tiles show it), and the opacity.
     */
    public List<String> cellDrawing(Cell cell) {
        List<String> drawn = new ArrayList<>();
        SpriteBatch recorder = new SpriteBatch() {
            @Override
            public void draw(TextureRegion region, float x, float y, float width, float height) {
                record(region, 0, 0, 1, 1, 0);
            }

            @Override
            public void draw(TextureRegion region, float x, float y, float originX, float originY, float width,
                    float height, float scaleX, float scaleY, float rotation) {
                record(region, originX - width / 2f, originY - height / 2f, scaleX, scaleY, rotation);
            }

            private void record(TextureRegion region, float originOffsetX, float originOffsetY, float scaleX,
                    float scaleY,
                    float rotation) {
                if (originOffsetX != 0 || originOffsetY != 0) {
                    throw new IllegalStateException("map tiles should turn around their centre");
                }
                AtlasRegion atlasRegion = (AtlasRegion) region;
                drawn.add(String.format("%s#%d %s alpha=%s", atlasRegion.name, atlasRegion.index,
                        landedCorners(scaleX, scaleY, rotation), Snapshot.number(getColor().a)));
            }
        };
        try {
            tile(cell).draw(recorder, 1);
        } finally {
            recorder.dispose();
        }
        return drawn;
    }

    /**
     * Scales and turns (around the centre, like the batch) each corner of a picture and says which picture corner ends
     * up on the cell's BL, TL, TR and BR corners.
     */
    private static String landedCorners(float scaleX, float scaleY, float rotation) {
        String[] names = {"BL", "TL", "TR", "BR"};
        float[][] corners = {{-1, -1}, {-1, 1}, {1, 1}, {1, -1}};
        String[] landed = new String[4];
        double cos = Math.cos(Math.toRadians(rotation)), sin = Math.sin(Math.toRadians(rotation));
        for (int i = 0; i < 4; i++) {
            double x = corners[i][0] * scaleX, y = corners[i][1] * scaleY;
            double turnedX = x * cos - y * sin, turnedY = x * sin + y * cos;
            int target = turnedX < 0 ? (turnedY < 0 ? 0 : 1) : (turnedY < 0 ? 3 : 2);
            landed[target] = names[i];
        }
        return String.join(",", landed);
    }

    /** How bright unlit places are, as box2dlights' ambient colour "r,g,b,a" (a = brightness, 1 = daylight). */
    public String ambientLight() {
        RayHandler rayHandler = screenField("rayHandler");
        Color ambient = field(rayHandler, RayHandler.class, "ambientLight");
        return String.format(java.util.Locale.ROOT, "%.2f,%.2f,%.2f,%.2f", ambient.r, ambient.g, ambient.b, ambient.a);
    }

    /**
     * Where the anchor of the player's sprite frame (the middle of the 48x48 picture before trimming, unless pack.json
     * moves it) is, relative to the player's bottom-left corner. It is drawn on the player's tile's centre.
     */
    public float[] playerFrameCentre() {
        Player player = (Player) actor(Kind.PLAYER);
        ResourcesService resources = screenField("resourcesService");
        SpriteSheet sheet = resources.getSpriteSheet(Sprite.RABBIT);
        TextureRegion picture = ((TextureRegionDrawable) player.getAvatar().getDrawable()).getRegion();
        float scale = player.getAvatar().getWidth() / picture.getRegionWidth();
        return new float[]{
                player.getAvatar().getX() + (sheet.anchorX() - sheet.trimX()) * scale,
                player.getAvatarRestY() + (sheet.anchorY() - sheet.trimY()) * scale};
    }

    /**
     * Where a pixel of the cow's sprite frame (art pixels from the 32x32 frame's top left) is drawn, relative to the
     * cow's bottom-left corner (its tile's corner).
     */
    public float[] cowFramePixel(int x, int y) {
        SpriteActor cow = (SpriteActor) actor(Kind.COW);
        ResourcesService resources = screenField("resourcesService");
        SpriteSheet sheet = resources.getSpriteSheet(cow.getSprite());
        TextureRegion picture = ((TextureRegionDrawable) cow.getAvatar().getDrawable()).getRegion();
        float scale = cow.getAvatar().getWidth() / picture.getRegionWidth();
        return new float[]{
                cow.getAvatar().getX() + (x - sheet.trimX()) * scale,
                cow.getAvatarRestY() + (sheet.frameHeight() - y - sheet.trimY()) * scale};
    }

    /**
     * Every UI piece and font the game lists (UiImage, UiFont) as the game loads it, one line each, e.g.
     * "DIALOG_BOX 30x28, nine-patch, borders 15x12" (the
     * smallest it can be drawn), "CONTINUE_INDICATOR 7x6, animation of 7 frames, 0.7 s", "PIXEL_7X7 line height 24, 'HELLO'
     * is 87 px wide".
     */
    public List<String> describeUi() {
        ResourcesService resources = screenField("resourcesService");
        List<String> lines = new ArrayList<>();
        for (UiImage image : UiImage.values()) {
            TextureRegion region = resources.getUiImage(image);
            StringBuilder line = new StringBuilder(
                    image + " " + region.getRegionWidth() + "x" + region.getRegionHeight());
            try {
                NinePatch patch = resources.getUiNinePatch(image);
                line.append(", nine-patch, borders ").append((int) (patch.getLeftWidth() + patch.getRightWidth()))
                        .append("x")
                        .append((int) (patch.getTopHeight() + patch.getBottomHeight()));
            } catch (IllegalArgumentException notANinePatch) {
                // a plain piece
            }
            try {
                Animation<TextureRegion> animation = resources.getUiAnimation(image);
                line.append(", animation of ").append(animation.getKeyFrames().length).append(" frames, ")
                        .append(Snapshot.number(animation.getAnimationDuration())).append(" s");
            } catch (IllegalArgumentException notAnAnimation) {
                // a still piece
            }
            lines.add(line.toString());
        }
        for (UiFont font : UiFont.values()) {
            BitmapFont bitmapFont = resources.getFont(font);
            lines.add(font + " line height " + (int) bitmapFont.getLineHeight() + ", 'HELLO' is "
                    + (int) new GlyphLayout(bitmapFont, "HELLO").width + " px wide");
        }
        return lines;
    }

    private static String regionName(TextureRegion region) {
        if (region instanceof AtlasRegion atlasRegion) {
            return atlasRegion.name;
        }
        return String.valueOf(region.getTexture());
    }

    public boolean crashed() {
        return crash != null;
    }

    public Cell playerCell() {
        return cellOf(actor(Kind.PLAYER).getTile());
    }

    public Cell cellOf(Kind kind) {
        return cellOf(actor(kind).getTile());
    }

    /** How far the player's picture is lifted above its position right now (0 = standing on the ground). */
    public float playerHopHeight() {
        GameBoardActor player = actor(Kind.PLAYER);
        return player.getAvatar().getY() - player.getAvatarRestY();
    }

    /** The cow's animation right now, e.g. "cow_brown/idle". */
    public String cowAnimation() {
        return ((Cow) actor(Kind.COW)).getCurrentAnimation();
    }

    /** What the cow is doing (its state machine's state), e.g. "STANDING", "WALKING", "WISHING". */
    public String cowState() {
        return ((Cow) actor(Kind.COW)).getState().name();
    }

    /** Is the cow's picture mirrored (it is drawn looking right; walking left it looks left)? */
    public boolean cowLooksLeft() {
        return ((Cow) actor(Kind.COW)).isMirrored();
    }

    /** Where the cow is drawn (bottom-left corner). */
    public float[] cowXY() {
        Actor cow = actor(Kind.COW);
        return new float[]{cow.getX(), cow.getY()};
    }

    /** What the bubble over the cow shows (what it wishes for), e.g. "carrot", or "none". */
    public String cowWish() {
        return bubbleOver(actor(Kind.COW));
    }

    /** The picture bubbles on the map are drawn in front of every other actor (only the cow has one). */
    private String bubbleOver(Group character) {
        Array<Actor> actors = gameStage().getActors();
        for (Actor actor : actors) {
            if (actor instanceof PictureBubble bubble) {
                if (actor != actors.peek()) {
                    throw new IllegalStateException("The bubble isn't in front of everything on the map");
                }
                return bubble.getPictureName();
            }
        }
        return "none";
    }

    /** The small pictures floating up on the map right now, e.g. ["heart"] over the player after stroking the cow. */
    public List<String> floatingPictures() {
        List<String> pictures = new ArrayList<>();
        for (Actor actor : gameStage().getActors()) {
            if (actor instanceof FloatingPicture) {
                pictures.add(actor.getName());
            }
        }
        return pictures;
    }

    /** The debug label under the cow (Debug.Tool.STATE_LABELS), or "none". */
    public String cowStateLabel() {
        for (Actor child : actor(Kind.COW).getChildren()) {
            if (child instanceof Label label) {
                return label.getText().toString();
            }
        }
        return "none";
    }

    /** Switches the debug tool that writes each creature's state under it on or off, for games started from now on. */
    public static void showStateLabels(boolean show) {
        if (show) {
            Debug.switchOn(Debug.Tool.STATE_LABELS);
        } else {
            Debug.switchOff(Debug.Tool.STATE_LABELS);
        }
    }

    /** Does the hint button's bubble show a picture after its text right now ("find" and a carrot)? */
    public boolean hintShowsPicture() {
        return hud().getBubble() != null && hud().getHintPicture() != null;
    }

    /** The chest's animation right now: "chest/open_front" while opening or open, "... (backwards)" closing or closed. */
    public String chestAnimation() {
        return ((Chest) actor(Kind.CHEST)).getCurrentAnimation();
    }

    /** The player's animation right now, e.g. "rabbit/walk_left". */
    public String playerAnimation() {
        return ((Player) actor(Kind.PLAYER)).getCurrentAnimation();
    }

    public String playerPosition() {
        Actor player = actor(Kind.PLAYER);
        return Snapshot.point(player.getX(), player.getY());
    }

    /** Where the player is drawn (bottom-left corner), as numbers for speed checks. */
    public float[] playerXY() {
        Actor player = actor(Kind.PLAYER);
        return new float[]{player.getX(), player.getY()};
    }

    /** Is the player walking (MOVEMENT) or standing (IDLE)? */
    public String playerStatus() {
        return String.valueOf(((Player) actor(Kind.PLAYER)).getStatus());
    }

    public String cameraPosition() {
        Camera camera = gameStage().getCamera();
        return Snapshot.point(camera.position.x, camera.position.y);
    }

    public String torchDistances() {
        List<String> distances = new ArrayList<>();
        for (Light light : lights()) {
            distances.add(Snapshot.number(light.getDistance()));
        }
        return String.join(" ", distances);
    }

    /** Is the tile currently inside the camera view (i.e. clickable by a player)? */
    public boolean isVisible(Cell cell) {
        Vector2 screen = gameScreenPosition(cellCenter(cell), false);
        return screen != null;
    }

    /** The map as the running game built it: one line per tile, sorted by cell. */
    public String describeBoard() {
        Snapshot snapshot = new Snapshot();
        for (Map.Entry<Cell, Tile> entry : tilesByCell().entrySet()) {
            Tile tile = entry.getValue();
            List<String> neighbours = new ArrayList<>();
            for (Tile neighbour : tile.getNeighbours()) {
                neighbours.add(cellOf(neighbour).toString());
            }
            neighbours.sort(null);
            List<String> objects = new ArrayList<>();
            for (TileObject object : tileObjects(tile)) {
                objects.add(kindOf(object) + (object.isAccessible() ? "" : "(blocking)"));
            }
            snapshot.line("%s at=%s size=%sx%s background=%s accessible=%s neighbours=%s objects=%s",
                    entry.getKey(), Snapshot.point(tile.getX(), tile.getY()), Snapshot.number(tile.getWidth()),
                    Snapshot.number(tile.getHeight()), backgroundName(tile), tile.isAccessible(), neighbours, objects);
        }
        return snapshot.toString();
    }

    /** The objects placed on the map, by cell: "PLAYER", "CHEST", "COW", "DOG", "LIGHT", "SPRITE". */
    public Map<Cell, List<String>> objectsByCell() {
        Map<Cell, List<String>> objects = new TreeMap<>(Comparator.comparing(Cell::toString));
        for (Map.Entry<Cell, Tile> entry : tilesByCell().entrySet()) {
            for (TileObject object : tileObjects(entry.getValue())) {
                objects.computeIfAbsent(entry.getKey(), cell -> new ArrayList<>()).add(kindOf(object));
            }
        }
        return objects;
    }

    /** The path the game would walk from one cell to another (start excluded), or an empty list. */
    public List<Cell> path(Cell from, Cell to) {
        List<Cell> cells = new ArrayList<>();
        for (Tile tile : boardService().getPath(tile(from), tile(to)).getPathNodes()) {
            cells.add(cellOf(tile));
        }
        return cells;
    }

    public List<Cell> neighbours(Cell cell) {
        List<Cell> cells = new ArrayList<>();
        for (Tile tile : tile(cell).getNeighbours()) {
            cells.add(cellOf(tile));
        }
        return cells;
    }

    /** The cells sharing a side with this one: from where a chest on it can be used. */
    public List<Cell> sideNeighbours(Cell cell) {
        List<Cell> cells = new ArrayList<>();
        for (Tile tile : tile(cell).getSideNeighbours()) {
            cells.add(cellOf(tile));
        }
        return cells;
    }

    /** The cells a chest or a creature on this one is used from ({@link Tile#getUsedFromNeighbours()}). */
    public List<Cell> usedFromNeighbours(Cell cell) {
        List<Cell> cells = new ArrayList<>();
        for (Tile tile : tile(cell).getUsedFromNeighbours()) {
            cells.add(cellOf(tile));
        }
        return cells;
    }

    public List<Cell> cells() {
        return new ArrayList<>(tilesByCell().keySet());
    }

    public boolean isAccessible(Cell cell) {
        return tile(cell).isAccessible();
    }

    private void describeScene(Snapshot snapshot) {
        Player player = (Player) actor(Kind.PLAYER);
        snapshot.line("player: cell=%s at=%s status=%s", cellOf(player.getTile()), playerPosition(),
                player.getStatus());
        if (has(Kind.DOG)) {
            snapshot.line("dog: cell=%s", cellOf(Kind.DOG));
        }
        snapshot.line("chest: cell=%s", chestCell());
        for (String item : itemsOnGround()) {
            snapshot.line("item on ground: %s", item);
        }
        OrthographicCamera camera = (OrthographicCamera) gameStage().getCamera();
        snapshot.line("camera: at=%s zoom=%s view=%sx%s", cameraPosition(), Snapshot.number(camera.zoom),
                Snapshot.number(camera.viewportWidth), Snapshot.number(camera.viewportHeight));
        snapshot.line("game stage shaded: %s", isGameStageShaded());
        for (Light light : lights()) {
            snapshot.line("torch: at=%s distance=%s color=%s", Snapshot.point(light.getX(), light.getY()),
                    Snapshot.number(light.getDistance()), light.getColor());
        }
        List<Group> buttons = menuButtons();
        snapshot.line("menu buttons: %d", buttons.size());
        for (Group button : buttons) {
            snapshot.line("  button at=%s scale=%s alpha=%s", Snapshot.point(button.getX(), button.getY()),
                    Snapshot.number(button.getScaleX()), alpha(button));
        }
        describeLabels(snapshot, "game stage", gameStage());
        describeLabels(snapshot, "notification stage", notificationStage());
        describeLabels(snapshot, "menu stage", menuStage());
        describeLabels(snapshot, "ui stage", uiStage());
        for (Actor actor : uiStage().getActors()) {
            float alpha = effectiveAlpha(actor);
            if (actor instanceof ActionButton && alpha > 0.01f) {
                snapshot.line("action button on ui stage: %s (a picture) at=%s alpha=%s", actor.getName(),
                        Snapshot.point(actor.getX(), actor.getY()), Snapshot.number(alpha));
            }
        }
    }

    /** Every label a player can see (effective alpha above zero), in drawing order. */
    private void describeLabels(Snapshot snapshot, String stageName, Stage stage) {
        List<Label> labels = new ArrayList<>();
        collectLabels(stage.getRoot(), labels);
        for (Label label : labels) {
            float alpha = effectiveAlpha(label);
            if (alpha > 0.01f && label.isVisible()) {
                Vector2 position = label.localToStageCoordinates(new Vector2());
                snapshot.line("text on %s: \"%s\" at=%s alpha=%s color=%s", stageName, label.getText(),
                        Snapshot.point(position), Snapshot.number(alpha), label.getStyle().fontColor);
            }
        }
    }

    private static void collectLabels(Actor actor, List<Label> labels) {
        if (!actor.isVisible()) {
            return;
        }
        if (actor instanceof Label label) {
            labels.add(label);
        }
        if (actor instanceof Group group) {
            for (Actor child : group.getChildren()) {
                collectLabels(child, labels);
            }
        }
    }

    private static float effectiveAlpha(Actor actor) {
        float alpha = 1;
        for (Actor current = actor; current != null; current = current.getParent()) {
            alpha *= current.getColor().a;
        }
        return alpha;
    }

    private static String alpha(Actor actor) {
        return Snapshot.number(effectiveAlpha(actor));
    }

    /** Is the fade shader (toggled with Q/W) active? The batch always has some shader, so compare with the fade one. */
    private boolean isGameStageShaded() {
        ShaderProgram fadeShader = field(gameStage(), gameStage().getClass(), "shaderProgram");
        return gameStage().getBatch().getShader() == fadeShader;
    }

    /** The chest leaves the board when collected, so it may be missing. */
    private String chestCell() {
        for (Actor actor : gameStage().getActors()) {
            if (actor instanceof Chest chest) {
                return chest.getTile() == null ? "off board" : cellOf(chest.getTile()).toString();
            }
        }
        return "gone";
    }

    // ---------------------------------------------------------------------------------------------- game internals

    private boolean has(Kind kind) {
        try {
            actor(kind);
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    private GameBoardActor actor(Kind kind) {
        Class<? extends GameBoardActor> type = switch (kind) {
            case PLAYER -> Player.class;
            case DOG -> Dog.class;
            case CHEST -> Chest.class;
            case COW -> Cow.class;
        };
        for (Actor actor : gameStage().getActors()) {
            if (type.isInstance(actor)) {
                return type.cast(actor);
            }
        }
        throw new IllegalStateException("No " + kind + " on the game stage");
    }

    private static String kindOf(TileObject object) {
        if (object instanceof TileSprite)
            return "SPRITE";
        if (object instanceof TileDog)
            return "DOG";
        if (object instanceof TileSpriteActor)
            return object.getObjectType().name();
        if (object instanceof TilePlayer)
            return "PLAYER";
        if (object instanceof TileLight)
            return "LIGHT";
        throw new IllegalStateException("Unknown tile object " + object.getClass());
    }

    /**
     * The tile's own picture, or for a map drawn from an atlas its layers' tilesets bottom first, e.g. "water+grass_tiles_v2"
     * (without the tile number: animated water changes it with the clock).
     */
    private static String backgroundName(Tile tile) {
        TextureRegion region = field(tile, Tile.class, "backgroundTexture");
        if (region == null) {
            List<String> layers = new ArrayList<>();
            SpriteBatch recorder = new SpriteBatch() {
                @Override
                public void draw(TextureRegion region, float x, float y, float width, float height) {
                    layers.add(regionName(region));
                }

                @Override
                public void draw(TextureRegion region, float x, float y, float originX, float originY, float width,
                        float height, float scaleX, float scaleY, float rotation) {
                    layers.add(regionName(region));
                }
            };
            try {
                tile.draw(recorder, 1);
            } finally {
                recorder.dispose();
            }
            return layers.isEmpty() ? "none" : String.join("+", layers);
        }
        if (region instanceof AtlasRegion atlasRegion) {
            return atlasRegion.name;
        }
        Texture texture = region.getTexture();
        return String.valueOf(texture);
    }

    private static List<TileObject> tileObjects(Tile tile) {
        return field(tile, Tile.class, "objects");
    }

    private Map<Cell, Tile> tilesByCell() {
        Map<Cell, Tile> tiles = new TreeMap<>();
        for (Tile tile : board().getTiles()) {
            tiles.put(cellOf(tile), tile);
        }
        return tiles;
    }

    private Tile tile(Cell cell) {
        Tile tile = tilesByCell().get(cell);
        if (tile == null) {
            throw new IllegalArgumentException("No tile at " + cell);
        }
        return tile;
    }

    /** Centre of a cell computed from the map geometry, so it also works for cells without a tile. */
    private static Vector2 cellCenter(Cell cell) {
        return new Vector2(cell.col() * CELL_SIZE + CELL_SIZE / 2f, cell.row() * CELL_SIZE + CELL_SIZE / 2f);
    }

    private static Cell cellOf(Tile tile) {
        int col = Math.round(tile.getX() / CELL_SIZE);
        int row = Math.round(tile.getY() / CELL_SIZE);
        return new Cell(col, row);
    }

    private Board board() {
        return screenField("board");
    }

    private BoardService boardService() {
        GameEventService events = screenField("gameEventService");
        return new BoardService(events, board().getTiles());
    }

    private Stage gameStage() {
        return screenField("gameStage");
    }

    private Stage menuStage() {
        return screenField("menuStage");
    }

    private Stage uiStage() {
        return screenField("uiStage");
    }

    /** Every actor on the UI stage, depth first. */
    private List<Actor> uiActors() {
        List<Actor> actors = new ArrayList<>();
        collectActors(uiStage().getRoot(), actors);
        return actors;
    }

    private static void collectActors(Actor actor, List<Actor> actors) {
        actors.add(actor);
        if (actor instanceof Group group) {
            for (Actor child : group.getChildren()) {
                collectActors(child, actors);
            }
        }
    }

    /** How many screen pixels wide a cell is drawn right now. */
    public float cellSizeOnScreen(Cell cell) {
        Vector2 left = gameStage()
                .stageToScreenCoordinates(new Vector2(cell.col() * CELL_SIZE, cell.row() * CELL_SIZE));
        Vector2 right = gameStage()
                .stageToScreenCoordinates(new Vector2((cell.col() + 1) * CELL_SIZE, cell.row() * CELL_SIZE));
        return right.x - left.x;
    }

    /** Where a button is on the screen and how big: "x,y wxh" in screen pixels (bottom-left corner, y down). */
    public String buttonOnScreen(String text) {
        for (Actor actor : uiActors()) {
            if (isButton(actor, text)) {
                Vector2 corner = uiStage().stageToScreenCoordinates(actor.localToStageCoordinates(new Vector2()));
                Vector2 opposite = uiStage().stageToScreenCoordinates(
                        actor.localToStageCoordinates(new Vector2(actor.getWidth(), actor.getHeight())));
                return Snapshot.number(corner.x) + "," + Snapshot.number(corner.y) + " "
                        + Snapshot.number(opposite.x - corner.x) + "x" + Snapshot.number(corner.y - opposite.y);
            }
        }
        throw new IllegalStateException("No \"" + text + "\" button on screen");
    }

    private Vector2 uiScreenPosition(Actor actor) {
        return screenPosition(uiStage(),
                actor.localToStageCoordinates(new Vector2(actor.getWidth() / 2f, actor.getHeight() / 2f)), true);
    }

    private Stage notificationStage() {
        return screenField("notificationStage");
    }

    private Array<Light> lights() {
        RayHandler rayHandler = screenField("rayHandler");
        return field(rayHandler, RayHandler.class, "lightList");
    }

    private List<Group> menuButtons() {
        List<Group> buttons = new ArrayList<>();
        for (Actor actor : menuStage().getActors()) {
            buttons.add((Group) actor);
        }
        return buttons;
    }

    private InputProcessor inputProcessor() {
        return game.inputProcessor();
    }

    private Vector2 gameScreenPosition(Vector2 world) {
        return gameScreenPosition(world, true);
    }

    /** Converts a world position to a screen position; a player can only click what is on screen. */
    private Vector2 gameScreenPosition(Vector2 world, boolean mustBeVisible) {
        return screenPosition(gameStage(), world, mustBeVisible);
    }

    private Vector2 menuScreenPosition(Group button) {
        return screenPosition(menuStage(), button.localToStageCoordinates(new Vector2()), true);
    }

    private Vector2 screenPosition(Stage stage, Vector2 stagePosition, boolean mustBeVisible) {
        Vector2 screen = stage.stageToScreenCoordinates(stagePosition.cpy());
        boolean visible = screen.x >= 0 && screen.y >= 0 && screen.x < game.screenWidth()
                && screen.y < game.screenHeight();
        if (!visible) {
            if (mustBeVisible) {
                throw new IllegalStateException("Target " + Snapshot.point(stagePosition) + " is not on screen");
            }
            return null;
        }
        return screen;
    }

    private <T> T screenField(String name) {
        Object screen = ((Game) game.game()).getScreen();
        return field(screen, screen.getClass(), name);
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object owner, Class<?> declaringClass, String name) {
        try {
            Field field = declaringClass.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(owner);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "Game internals changed: update GameDriver (" + declaringClass.getSimpleName() + "." + name + ")",
                    e);
        }
    }
}
