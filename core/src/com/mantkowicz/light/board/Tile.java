package com.mantkowicz.light.board;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.mantkowicz.light.resources.ResourcesService;

import java.util.ArrayList;
import java.util.List;

import static com.mantkowicz.light.board.TilePoint.LEFT_BOTTOM_CORNER;
import static com.mantkowicz.light.board.TilePoint.LEFT_TOP_CORNER;
import static com.mantkowicz.light.board.TilePoint.RIGHT_BOTTOM_CORNER;
import static com.mantkowicz.light.board.TilePoint.RIGHT_TOP_CORNER;

// baseline (docs/code-quality-plan.md): over the PMD limit before the check came, to be refactored

@SuppressWarnings("PMD.GodClass")
public abstract class Tile {
    private static final int NOTIFICATION_OFFSET = 5;

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

    protected List<TileContent> objects = new ArrayList<>();
    private TextureRegion backgroundTexture;
    /** Map tiles drawn instead of the background, bottom layer first (maps made of several tile layers). */
    private List<TileGraphic> layers;

    protected Tile(ResourcesService resourcesService, String backgroundTextureName) {
        this();
        setBackground(resourcesService, backgroundTextureName);
    }

    /**
     * A tile without a background picture: on maps made with tools/assets the picture comes from the map
     * ({@link #setGraphics}), which also gives the tile its size. Until then it draws nothing.
     */
    protected Tile() {
        this.id = ID_SEQUENCE++;
        this.neighbours = new ArrayList<>();
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

    /**
     * Adds what the tile itself needs in the physics world (nothing for most tiles; a wall's body). The objects on it
     * are prepared by the game screen ({@link #getTileObjects()}).
     */
    public void prepare(World world) {
    }

    /** What the map put on this tile (furniture, characters, lights...), in the order it was added. */
    public List<TileContent> getTileObjects() {
        return objects;
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

    /**
     * The neighbours sharing a side with this tile (up, down, left, right), without the diagonal ones. Walking uses all
     * {@link #getNeighbours()}; a creature steps only onto these, because the characters face only these four
     * directions.
     */
    public List<Tile> getSideNeighbours() {
        List<Tile> sides = new ArrayList<>();
        Vector2 center = getCenter();
        for (Tile neighbour : neighbours) {
            Vector2 other = neighbour.getCenter();
            if (Math.abs(other.x - center.x) < 0.5f || Math.abs(other.y - center.y) < 0.5f) {
                sides.add(neighbour);
            }
        }
        return sides;
    }

    /**
     * The tiles something on this tile (a chest, a creature) is used from: the neighbours left of it, right of it and
     * below it. Not the diagonal ones, because the characters face only up, down, left and right, and not the one above,
     * where the action button drawn over the thing would cover the player.
     */
    public List<Tile> getUsedFromNeighbours() {
        List<Tile> tiles = new ArrayList<>();
        float centerY = getCenter().y;
        for (Tile side : getSideNeighbours()) {
            if (side.getCenter().y < centerY + 0.5f) {
                tiles.add(side);
            }
        }
        return tiles;
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
            case RIGHT_TOP_CORNER :
                return new Vector2(x + width, y + height);
            case RIGHT_BOTTOM_CORNER :
                return new Vector2(x + width, y);
            case LEFT_BOTTOM_CORNER :
                return new Vector2(x, y);
            case LEFT_TOP_CORNER :
                return new Vector2(x, y + height);
            case CENTER :
            default :
                return getCenter();
        }
    }

    public void addTileObject(TileContent object) {
        objects.add(object);
    }

    public void draw(Batch batch, float parentAlpha) {
        if (layers == null) {
            if (backgroundTexture != null) {
                batch.draw(backgroundTexture, x, y);
            }
            return;
        }
        for (TileGraphic layer : layers) {
            drawGraphic(batch, layer);
        }
    }

    /**
     * Draws a map tile stretched over the tile, with the layer's opacity. A mirrored tile is drawn with a scale of -1 and
     * a rotated one turned around the tile's centre: mirror first, then turn counter-clockwise, like libGDX's map
     * renderer.
     */
    private void drawGraphic(Batch batch, TileGraphic graphic) {
        TextureRegion region = graphic.tile().getTextureRegion();
        float drawX = x + graphic.offsetX();
        float drawY = y + graphic.offsetY();
        float tint = batch.getPackedColor();
        if (graphic.opacity() != 1f) {
            Color color = batch.getColor();
            batch.setColor(color.r, color.g, color.b, color.a * graphic.opacity());
        }
        if (!graphic.flipHorizontally() && !graphic.flipVertically() && graphic.rotation() == Cell.ROTATE_0) {
            batch.draw(region, drawX, drawY, width, height);
        } else {
            batch.draw(region, drawX, drawY, width / 2f, height / 2f, width, height,
                    graphic.flipHorizontally() ? -1 : 1, graphic.flipVertically() ? -1 : 1, graphic.rotation() * 90f);
        }
        batch.setPackedColor(tint); // the next drawings keep the batch's own colour
    }

    /**
     * Draws the tile's objects (furniture). The board calls it after every background is drawn, so neighbours never cover them.
     */
    public void drawObjects(Batch batch) {
        for (TileContent object : objects) {
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
