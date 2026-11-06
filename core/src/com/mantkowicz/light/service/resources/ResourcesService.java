package com.mantkowicz.light.service.resources;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.TextureLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.Array;

import java.util.Arrays;
import java.util.List;

import static com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion;

public class ResourcesService {
    private static final String FONT = "font.fnt";

    private final Skin skin;
    private final AssetManager assetManager;
    private final Array<TextureAtlas> atlases = new Array<>();

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
    }

    public TextureRegion getTexture(String textureName) {
        for (TextureAtlas atlas : atlases) {
            AtlasRegion foundRegion = atlas.findRegion(textureName);
            if (foundRegion != null) {
                return foundRegion;
            }
        }
        try {
            Texture texture = assetManager.get(textureName, Texture.class);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            return new TextureRegion(texture);
        } catch (Throwable t) {
            throw new IllegalArgumentException("Couldn't find texture: " + textureName);
        }
    }

    public NinePatchDrawable getNinePatch(String textureName) {
        for (TextureAtlas atlas : atlases) {
            AtlasRegion foundRegion = atlas.findRegion(textureName);
            if (foundRegion != null) {
                return new NinePatchDrawable(atlas.createPatch(textureName));
            }
        }
        try {
            Texture texture = assetManager.get(textureName, Texture.class);
            return new NinePatchDrawable(new NinePatch(texture));
        } catch (Throwable t) {
            throw new IllegalArgumentException("Couldn't find texture: " + textureName);
        }
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
