package com.mantkowicz.light.resources;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;

public class ResourcesService {
    private static final String FONT = "font.fnt";
    /** Animations made by tools/assets: names, frame durations and the atlas with the frames. */
    private static final String ANIMATIONS = "sprites/animations.json";
    private static final String RUN_TOOL = "run python3 tools/assets/build_assets.py";

    private final Skin skin;
    private final AssetManager assetManager;
    private final Array<TextureAtlas> atlases = new Array<>();
    private final Map<String, Animation<TextureRegion>> animations = new HashMap<>();
    private final Map<String, SpriteSheet> spriteSheets = new HashMap<>();

    public ResourcesService() {
        assetManager = new AssetManager();

        TextureLoader.TextureParameter textureParameter = new TextureLoader.TextureParameter();
        textureParameter.minFilter = Texture.TextureFilter.Linear;
        textureParameter.magFilter = Texture.TextureFilter.Linear;

        List<String> textureNames = Arrays.asList("floor-tile.png", "wall-tile.png", "player.png", "chest.png",
                "lighter.png", "item_background.png", "table_background.png", "item_background_pressed.png", "item_background_able_to_put.png",
                "item_background_chosen.png", "dog.png", "black.png");

        for (String textureName : textureNames) {
            assetManager.load(textureName, Texture.class, textureParameter);
        }

        assetManager.load(FONT, BitmapFont.class);
        assetManager.load("skin/skin.atlas", TextureAtlas.class);
        assetManager.finishLoading();

        assetManager.getAll(TextureAtlas.class, atlases);
        skin = new Skin(Gdx.files.internal("skin/skin.json"), assetManager.get("skin/skin.atlas", TextureAtlas.class));
        loadAnimations();
    }

    private void loadAnimations() {
        FileHandle file = Gdx.files.internal(ANIMATIONS);
        JsonValue root = new JsonReader().parse(file);
        String atlasName = file.parent().child(root.getString("atlas")).path();
        assetManager.load(atlasName, TextureAtlas.class);
        assetManager.finishLoadingAsset(atlasName);
        TextureAtlas atlas = assetManager.get(atlasName, TextureAtlas.class);
        if (!atlases.contains(atlas, true)) {
            atlases.add(atlas); // getTexture finds single sprite frames too, e.g. "rabbit/idle_down"
        }

        for (JsonValue sheet = root.get("sheets").child; sheet != null; sheet = sheet.next) {
            spriteSheets.put(sheet.name, new SpriteSheet(sheet.getInt("frameWidth"), sheet.getInt("frameHeight"),
                    sheet.getInt("trimX"), sheet.getInt("trimY")));
        }
        for (JsonValue animation = root.get("animations").child; animation != null; animation = animation.next) {
            Array<AtlasRegion> frames = atlas.findRegions(animation.name);
            if (frames.size != animation.getInt("frames")) {
                throw new IllegalStateException("Animation '" + animation.name + "' has " + frames.size + " frames in "
                        + atlasName + " instead of " + animation.getInt("frames") + ": " + RUN_TOOL);
            }
            animations.put(animation.name, new Animation<TextureRegion>(animation.getFloat("frameDuration"), frames,
                    Animation.PlayMode.valueOf(animation.getString("playMode"))));
        }
        // fail at start, not when an actor first needs its sheet
        for (Sprite sprite : Sprite.values()) {
            if (!spriteSheets.containsKey(sprite.getSheet())) {
                throw new IllegalStateException("Sprite." + sprite + " (sheet '" + sprite.getSheet() + "') isn't in "
                        + ANIMATIONS + ": " + RUN_TOOL);
            }
        }
    }

    /**
     * The animation of an action in a direction: the sheet's row "action_direction", e.g. (RABBIT, WALK, LEFT) is
     * "rabbit/walk_left".
     */
    public Animation<TextureRegion> getAnimation(Sprite sprite, Action action, Direction direction) {
        return getAnimation(animationName(sprite, action, direction));
    }

    /**
     * The animation of an action without a direction: the sheet's row "action", e.g. "open_front" of the chest sheet.
     */
    public Animation<TextureRegion> getAnimation(Sprite sprite, Action action) {
        return getAnimation(animationName(sprite, action, null));
    }

    public boolean hasAnimation(Sprite sprite, Action action, Direction direction) {
        return hasAnimation(animationName(sprite, action, direction));
    }

    public boolean hasAnimation(Sprite sprite, Action action) {
        return hasAnimation(animationName(sprite, action, null));
    }

    public SpriteSheet getSpriteSheet(Sprite sprite) {
        return getSpriteSheet(sprite.getSheet());
    }

    /**
     * "sheet/row" as in sprites/animations.json, e.g. "rabbit/walk_left"; {@code direction} may be null.
     */
    public static String animationName(Sprite sprite, Action action, Direction direction) {
        return sprite.getSheet() + "/" + action.getRow() + (direction == null ? "" : "_" + direction.getSuffix());
    }

    private Animation<TextureRegion> getAnimation(String name) {
        Animation<TextureRegion> animation = animations.get(name);
        if (animation == null) {
            throw new IllegalArgumentException("Sheet '" + name.substring(0, name.indexOf('/')) + "' has no animation '"
                    + name.substring(name.indexOf('/') + 1) + "' (see build/assets-preview/index.html for its rows)");
        }
        return animation;
    }

    private boolean hasAnimation(String name) {
        return animations.containsKey(name);
    }

    private SpriteSheet getSpriteSheet(String name) {
        SpriteSheet sheet = spriteSheets.get(name);
        if (sheet == null) {
            throw new IllegalArgumentException("Couldn't find sprite sheet: " + name);
        }
        return sheet;
    }

    public TextureRegion getTexture(String textureName) {
        for (TextureAtlas atlas : atlases) {
            AtlasRegion foundRegion = atlas.findRegion(textureName);
            if (foundRegion != null) {
                return foundRegion;
            }
        }
        if (!assetManager.isLoaded(textureName, Texture.class)) {
            throw new IllegalArgumentException("Couldn't find texture: " + textureName);
        }
        Texture texture = assetManager.get(textureName, Texture.class);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return new TextureRegion(texture);
    }

    /**
     * {@link #getTexture(String)} wrapped for scene2d widgets.
     */
    public TextureRegionDrawable getDrawable(String textureName) {
        return new TextureRegionDrawable(getTexture(textureName));
    }

    public NinePatchDrawable getNinePatch(String textureName) {
        for (TextureAtlas atlas : atlases) {
            AtlasRegion foundRegion = atlas.findRegion(textureName);
            if (foundRegion != null) {
                return new NinePatchDrawable(atlas.createPatch(textureName));
            }
        }
        if (!assetManager.isLoaded(textureName, Texture.class)) {
            throw new IllegalArgumentException("Couldn't find texture: " + textureName);
        }
        return new NinePatchDrawable(new NinePatch(assetManager.get(textureName, Texture.class)));
    }

    public TextureRegion getThumbnail(ThumbnailType thumbnailType) {
        return getTexture(thumbnailType.getRegionName());
    }

    /**
     * The one game font, loaded once and disposed with the asset manager.
     */
    public BitmapFont getFont() {
        return assetManager.get(FONT, BitmapFont.class);
    }

    public Skin getSkin() {
        return skin;
    }

    public AssetManager getAssetManager() {
        return assetManager;
    }

    public void dispose() {
        assetManager.dispose();
        skin.dispose();
    }

    public FileHandle getShaderVertex(ShaderType shader) {
        return Gdx.files.internal(shader.getVertexPath());
    }

    public FileHandle getShaderFragment(ShaderType shader) {
        return Gdx.files.internal(shader.getFragmentPath());
    }
}
