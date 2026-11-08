package com.mantkowicz.light.board;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.map.TileObject;
import com.mantkowicz.light.resources.ResourcesService;

import java.util.ArrayList;
import java.util.List;

import static com.mantkowicz.light.board.TilePoint.LEFT_BOTTOM_CORNER;
import static com.mantkowicz.light.board.TilePoint.LEFT_TOP_CORNER;
import static com.mantkowicz.light.board.TilePoint.RIGHT_BOTTOM_CORNER;
import static com.mantkowicz.light.board.TilePoint.RIGHT_TOP_CORNER;

public abstract class Tile {
    private static final int NOTIFICATION_OFFSET = 5;
    /** One quad for {@link #drawGraphic}: 4 corners of x, y, colour, u, v. Drawing is single-threaded. */
    private static final int VERTEX_SIZE = 5, U = 3, V = 4;
    private static final float[] VERTICES = new float[4 * VERTEX_SIZE];

    private static int ID_SEQUENCE = 1;

    private final int id;
    private final List<Tile> neighbours;

    private MapProperties attributes;
    private Array<Vector2> polygon;

    private float width, height;

    private float x, y;

    public void setSize(float width, float height) {
        this.width = width;
        this.height = height;
    }

    private boolean marked = false;

    protected List<TileObject> objects = new ArrayList<>();
    private TextureRegion backgroundTexture;
    /** Map tiles drawn instead of the background, bottom layer first (maps made of several tile layers). */
    private List<TileGraphic> layers;

    protected Tile(ResourcesService resourcesService, String backgroundTextureName) {
        this.id = ID_SEQUENCE++;
        this.neighbours = new ArrayList<>();
        setBackground(resourcesService, backgroundTextureName);
    }

    /**
     * Replaces the class default background. Call it before {@link #setPosition}, because the size comes from the texture.
     */
    public void setBackground(ResourcesService resourcesService, String backgroundTextureName) {
        backgroundTexture = resourcesService.getTexture(backgroundTextureName);
        setSize(backgroundTexture.getRegionWidth(), backgroundTexture.getRegionHeight()); //TODO: move setSize to tile creation place
    }

    /**
     * Draws these map tiles (bottom layer first) instead of the background, stretched to a square of the given size.
     * Animated tiles animate by themselves. Call it before {@link #setPosition}.
     */
    public void setLayers(List<TiledMapTile> layers, float size) {
        List<TileGraphic> graphics = new ArrayList<>();
        for (TiledMapTile layer : layers) {
            graphics.add(TileGraphic.of(layer));
        }
        setGraphics(graphics, size);
    }

    /**
     * Like {@link #setLayers}, keeping how each map tile was placed in Tiled (mirrored, rotated) and its layer's opacity
     * and offset.
     */
    public void setGraphics(List<TileGraphic> graphics, float size) {
        this.layers = new ArrayList<>(graphics);
        setSize(size, size);
    }

    public void prepare(GameContext context) {
        for (TileObject object : objects) {
            object.prepare(this, context);
        }
    }

    protected Vector2[] getPolygonVertices() {
        return polygon.toArray(Vector2[]::new);
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
        initTilePolygon();
    }

    private void initTilePolygon() {
        polygon = new Array<>();
        polygon.add(getTilePointPosition(LEFT_BOTTOM_CORNER));
        polygon.add(getTilePointPosition(RIGHT_BOTTOM_CORNER));
        polygon.add(getTilePointPosition(RIGHT_TOP_CORNER));
        polygon.add(getTilePointPosition(LEFT_TOP_CORNER));
    }

    public boolean doesContainPoint(Vector2 point) {
        return Intersector.isPointInPolygon(this.polygon, point);
    }

    public void mark() {
        marked = true;
    }

    public void unmark() {
        marked = false;
    }

    public boolean isMarked() {
        return marked;
    }

    public abstract boolean isAccessible();

    public List<Tile> getNeighbours() {
        return neighbours;
    }

    public void addNeighbour(Tile neighbour) {
        this.neighbours.add(neighbour);
    }

    public Integer getId() {
        return id;
    }

    public Vector2 getCenter() {
        return new Vector2(x + width / 2f, y + height / 2f);
    }

    protected Vector2 getLeftBottom() {
        return new Vector2(x, y);
    }

    public MapProperties getAttributes() {
        return attributes;
    }

    public void setAttributes(MapProperties attributes) {
        this.attributes = attributes;
    }

    /**
     * Calculates position that provided actor must have to be located at the point of the tile
     *
     * @param actor the actor that must be located at the point of the tile
     * @return position of positioned actor
     */
    public Vector2 calculatePositionForActorAt(Actor actor, TilePoint tilePoint) {
        return getTilePointPosition(tilePoint).sub(actor.getWidth() / 2f, actor.getHeight() / 2f);
    }

    /**
     * @return center position for notification for this tile
     */
    public Vector2 getNotificationCenterPosition() {
        return getCenter().cpy().add(0, height / 2f + NOTIFICATION_OFFSET);
    }

    private Vector2 getTilePointPosition(TilePoint tilePoint) {
        switch (tilePoint) {
            case RIGHT_TOP_CORNER:
                return new Vector2(x + width, y + height);
            case RIGHT_BOTTOM_CORNER:
                return new Vector2(x + width, y);
            case LEFT_BOTTOM_CORNER:
                return new Vector2(x, y);
            case LEFT_TOP_CORNER:
                return new Vector2(x, y + height);
            case CENTER:
            default:
                return getCenter();
        }
    }

    public void addTileObject(TileObject object) {
        objects.add(object);
    }

    public void draw(Batch batch, float parentAlpha) {
        if (layers == null) {
            batch.draw(backgroundTexture, x, y);
            return;
        }
        for (TileGraphic layer : layers) {
            drawGraphic(batch, layer);
        }
    }

    /**
     * Draws a map tile stretched over the tile, mirrored and rotated the way libGDX's OrthogonalTiledMapRenderer does it
     * (mirror first, then turn the picture counter-clockwise), with the layer's opacity.
     */
    private void drawGraphic(Batch batch, TileGraphic graphic) {
        TextureRegion region = graphic.getTile().getTextureRegion();
        Color tint = batch.getColor();
        float color = Color.toFloatBits(tint.r, tint.g, tint.b, tint.a * graphic.getOpacity());
        float x1 = x + graphic.getOffsetX();
        float y1 = y + graphic.getOffsetY();
        float x2 = x1 + width;
        float y2 = y1 + height;
        // corners 0 bottom-left, 1 top-left, 2 top-right, 3 bottom-right; u across, v down the texture
        setCorner(0, x1, y1, color, region.getU(), region.getV2());
        setCorner(1, x1, y2, color, region.getU(), region.getV());
        setCorner(2, x2, y2, color, region.getU2(), region.getV());
        setCorner(3, x2, y1, color, region.getU2(), region.getV2());
        if (graphic.isFlipHorizontally()) {
            swapCoordinate(U, 0, 2);
            swapCoordinate(U, 1, 3);
        }
        if (graphic.isFlipVertically()) {
            swapCoordinate(V, 0, 2);
            swapCoordinate(V, 1, 3);
        }
        int turns = graphic.getRotation();
        for (int turn = 0; turn < turns && turn < 3; turn++) {
            // each corner takes what the next one (0→1→2→3, counter-clockwise) showed: the picture turns left
            rotateCoordinate(U);
            rotateCoordinate(V);
        }
        batch.draw(region.getTexture(), VERTICES, 0, VERTICES.length);
    }

    private static void setCorner(int corner, float x, float y, float color, float u, float v) {
        int i = corner * VERTEX_SIZE;
        VERTICES[i] = x;
        VERTICES[i + 1] = y;
        VERTICES[i + 2] = color;
        VERTICES[i + U] = u;
        VERTICES[i + V] = v;
    }

    private static void swapCoordinate(int coordinate, int cornerA, int cornerB) {
        int a = cornerA * VERTEX_SIZE + coordinate;
        int b = cornerB * VERTEX_SIZE + coordinate;
        float temp = VERTICES[a];
        VERTICES[a] = VERTICES[b];
        VERTICES[b] = temp;
    }

    private static void rotateCoordinate(int coordinate) {
        float first = VERTICES[coordinate];
        for (int corner = 0; corner < 3; corner++) {
            VERTICES[corner * VERTEX_SIZE + coordinate] = VERTICES[(corner + 1) * VERTEX_SIZE + coordinate];
        }
        VERTICES[3 * VERTEX_SIZE + coordinate] = first;
    }

    /**
     * Draws the tile's objects (furniture). The board calls it after every background is drawn, so neighbours never cover them.
     */
    public void drawObjects(Batch batch) {
        for (TileObject object : objects) {
            object.draw(this, batch);
        }
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }
}
