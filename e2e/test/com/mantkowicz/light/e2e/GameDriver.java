package com.mantkowicz.light.e2e;

import box2dLight.Light;
import box2dLight.RayHandler;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Array;
import com.mantkowicz.light.actor.GameBoardActor;
import com.mantkowicz.light.actor.implementation.animal.Dog;
import com.mantkowicz.light.actor.implementation.collectible.Chest;
import com.mantkowicz.light.actor.implementation.player.Player;
import com.mantkowicz.light.board.Board;
import com.mantkowicz.light.board.object.TileObject;
import com.mantkowicz.light.board.object.implementation.chest.TileChest;
import com.mantkowicz.light.board.object.implementation.dog.TileDog;
import com.mantkowicz.light.board.object.implementation.light.TileLight;
import com.mantkowicz.light.board.object.implementation.player.TilePlayer;
import com.mantkowicz.light.board.object.implementation.sprite.TileSprite;
import com.mantkowicz.light.board.service.BoardService;
import com.mantkowicz.light.board.tile.Tile;
import com.mantkowicz.light.e2e.headless.HeadlessGame;
import com.mantkowicz.light.service.event.GameEventService;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Plays the game like a player would: clicks tiles and actors, holds, drags, presses keys, waits.
 * Observes only what a player could see. This is the ONLY test class that knows the game's classes;
 * when the game is refactored, only this class may need (mechanical) changes - scenarios and goldens must not.
 */
public final class GameDriver {
    /** Hex geometry of map.tmx: 128 px tall cells, columns 111 px apart (staggered odd columns). */
    private static final float CELL_HEIGHT = 128;
    private static final float COLUMN_WIDTH = 111;
    private static final float TILE_WIDTH = 148;
    private static final int START_FRAMES = 30;
    private static final int FRAMES_PER_SECOND = 60;

    public enum Kind {PLAYER, DOG, CHEST}

    private final HeadlessGame game = new HeadlessGame();
    private final StringBuilder timeline = new StringBuilder();
    private String crash;
    private Vector2 pointer = new Vector2();

    private GameDriver() {
    }

    /** Starts the game and lets it settle for half a second. */
    public static GameDriver start() {
        GameDriver driver = new GameDriver();
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

    public boolean crashed() {
        return crash != null;
    }

    public Cell playerCell() {
        return cellOf(actor(Kind.PLAYER).getTile());
    }

    public Cell cellOf(Kind kind) {
        return cellOf(actor(kind).getTile());
    }

    public String playerPosition() {
        Actor player = actor(Kind.PLAYER);
        return Snapshot.point(player.getX(), player.getY());
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

    public List<Cell> cells() {
        return new ArrayList<>(tilesByCell().keySet());
    }

    public boolean isAccessible(Cell cell) {
        return tile(cell).isAccessible();
    }

    private void describeScene(Snapshot snapshot) {
        Player player = (Player) actor(Kind.PLAYER);
        snapshot.line("player: cell=%s at=%s status=%s", cellOf(player.getTile()), playerPosition(), player.getStatus());
        snapshot.line("dog: cell=%s", cellOf(Kind.DOG));
        snapshot.line("chest: cell=%s", chestCell());
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

    private GameBoardActor actor(Kind kind) {
        Class<? extends GameBoardActor> type = switch (kind) {
            case PLAYER -> Player.class;
            case DOG -> Dog.class;
            case CHEST -> Chest.class;
        };
        for (Actor actor : gameStage().getActors()) {
            if (type.isInstance(actor)) {
                return type.cast(actor);
            }
        }
        throw new IllegalStateException("No " + kind + " on the game stage");
    }

    private static String kindOf(TileObject object) {
        if (object instanceof TileSprite) return "SPRITE";
        if (object instanceof TileChest) return "CHEST";
        if (object instanceof TileDog) return "DOG";
        if (object instanceof TilePlayer) return "PLAYER";
        if (object instanceof TileLight) return "LIGHT";
        throw new IllegalStateException("Unknown tile object " + object.getClass());
    }

    private static String backgroundName(Tile tile) {
        TextureRegion region = field(tile, Tile.class, "backgroundTexture");
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
        float x = cell.col() * COLUMN_WIDTH;
        float y = (cell.row() - (cell.col() % 2) / 2f) * CELL_HEIGHT;
        return new Vector2(x + TILE_WIDTH / 2f, y + CELL_HEIGHT / 2f);
    }

    private static Cell cellOf(Tile tile) {
        int col = Math.round(tile.getX() / COLUMN_WIDTH);
        int row = Math.round(tile.getY() / CELL_HEIGHT + (col % 2) / 2f);
        return new Cell(col, row);
    }

    private Board board() {
        return screenField("board");
    }

    private BoardService boardService() {
        GameEventService events = screenField("gameEventService");
        return new BoardService(events, board());
    }

    private Stage gameStage() {
        return screenField("gameStage");
    }

    private Stage menuStage() {
        return screenField("menuStage");
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
        boolean visible = screen.x >= 0 && screen.y >= 0 && screen.x < game.screenWidth() && screen.y < game.screenHeight();
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
            throw new IllegalStateException("Game internals changed: update GameDriver (" + declaringClass.getSimpleName() + "." + name + ")", e);
        }
    }
}
