package com.mantkowicz.light.ui.hud;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.ui.RoundButton;

/**
 * The HUD's big round button right of the face: the tool for where the player stands (e.g. the scythe on grass), or
 * without one the hint (the question mark). Not to be mixed up with the small action buttons over things on the map
 * ({@link ActionButton}).
 * <p>
 * For a swap ({@link #setSwap}) it widens to the left into the pack's rounded button: what goes on its left (a backpack
 * item, pulsing with its slot), the swap arrows in the middle, what comes on its right (the item the player stands
 * on). Round or wide, its right edge and the middle of its face stay where the HUD placed them ({@link #place}).
 */
public class ToolButton extends RoundButton {
    /** Its name for the question mark (no tool). */
    public static final String HINT = "hint";
    /** Its name while it offers a swap ({@link #setSwap}). */
    public static final String SWAP = "swap";
    /** The wide button: as wide as there is room right of the face, the pack's button drawn at the HUD's scale. */
    static final float WIDE_WIDTH = 252;
    /** Its items and the arrows: 16 px pictures at the HUD's scale, this far in from its ends. */
    private static final float WIDE_ITEM_SIZE = 64, WIDE_INSET = 22;

    private final ResourcesService resources;
    private final float roundWidth, roundHeight, roundFaceMiddle;
    /** Where the HUD placed it: its right edge, and the middle of its face. */
    private float right, faceMiddle;
    private final NinePatchDrawable wideUp, wideDown;
    private final Group wide = new Group();
    private final Image wideBackground = new Image(), going = new Image(), arrows, coming = new Image();
    private String toolName = HINT;
    private Runnable toolUse;

    /** @param hint what tapping it does without a tool (the face says the hint) */
    ToolButton(ResourcesService resources, Runnable hint) {
        super(resources, resources.getUiImage(UiImage.HINT_ICON));
        this.resources = resources;
        roundWidth = getWidth();
        roundHeight = getHeight();
        roundFaceMiddle = getFaceMiddle();
        wideUp = wide(resources, UiImage.BUTTON);
        wideDown = wide(resources, UiImage.BUTTON_DOWN);
        arrows = new Image(resources.getUiImage(UiImage.SWAP_ICON));
        for (Image piece : new Image[]{wideBackground, going, arrows, coming}) {
            piece.setTouchable(Touchable.disabled);
            wide.addActor(piece);
        }
        going.setSize(WIDE_ITEM_SIZE, WIDE_ITEM_SIZE);
        coming.setSize(WIDE_ITEM_SIZE, WIDE_ITEM_SIZE);
        arrows.setSize(arrows.getPrefWidth() * Hud.SCALE, arrows.getPrefHeight() * Hud.SCALE);
        wide.setTouchable(Touchable.disabled);
        wide.setVisible(false);
        addActor(wide);
        setName("TOOL");
        addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (toolUse != null) {
                    toolUse.run();
                } else {
                    hint.run();
                }
            }
        });
    }

    private static NinePatchDrawable wide(ResourcesService resources, UiImage image) {
        NinePatch patch = new NinePatch(resources.getUiNinePatch(image));
        patch.scale(Hud.SCALE, Hud.SCALE);
        return new NinePatchDrawable(patch);
    }

    /**
     * It shows this tool and tapping it uses it, until {@link #resetTool()}.
     *
     * @param name what the tool is, e.g. "scythe" (for tests and descriptions)
     */
    public void setTool(String name, TextureRegion picture, Runnable use) {
        showWide(false);
        toolName = name;
        toolUse = use;
        setPicture(picture);
    }

    /**
     * It widens and offers a swap: {@code going} (a backpack item) on its left, the arrows, {@code coming} (the item the
     * player stands on) on its right; tapping it swaps them, until {@link #resetTool()} or another tool.
     */
    public void setSwap(TextureRegion goingPicture, TextureRegion comingPicture, Runnable swap) {
        toolName = SWAP;
        toolUse = swap;
        setPicture(goingPicture, going);
        setPicture(comingPicture, coming);
        showWide(true);
    }

    private static void setPicture(TextureRegion picture, Image image) {
        TextureRegionDrawable now = (TextureRegionDrawable) image.getDrawable();
        if (now == null || now.getRegion() != picture) {
            image.setDrawable(new TextureRegionDrawable(picture));
        }
    }

    /**
     * Tints what would go in the swap on offer, as its backpack slot is tinted (they pulse together: the same item).
     */
    void tintGoing(Color tint) {
        going.getColor().set(tint);
    }

    /** No tool: the question mark again, and tapping it gives the hint. */
    public void resetTool() {
        showWide(false);
        toolName = HINT;
        toolUse = null;
        setPicture(resources.getUiImage(UiImage.HINT_ICON));
    }

    /**
     * Stands with its right edge here and the middle of its face (the part that is pressed, over its side) at this
     * height, round or wide.
     */
    void place(float rightEdge, float faceMiddleY) {
        right = rightEdge;
        faceMiddle = faceMiddleY;
        setPosition(right - getWidth(),
                Math.round(faceMiddle - (wide.isVisible() ? wideFaceMiddle() : roundFaceMiddle)));
    }

    /** How high above the wide button's bottom the middle of its face is (its bottom border is its side). */
    private float wideFaceMiddle() {
        return (wideUp.getMinHeight() + wideUp.getBottomHeight() - wideUp.getTopHeight()) / 2f;
    }

    /** Wide (a swap) or round (a tool or the hint); its right edge and the middle of its face stay where they are. */
    private void showWide(boolean shown) {
        if (shown == wide.isVisible()) {
            return;
        }
        wide.setVisible(shown);
        setRoundShown(!shown);
        setSize(shown ? WIDE_WIDTH : roundWidth, shown ? wideUp.getMinHeight() : roundHeight);
        place(right, faceMiddle);
        showWidePressed();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        showWidePressed();
    }

    /** Pressed, the wide button is lower (the pack's pressed piece) and what is on it goes down with it. */
    private void showWidePressed() {
        if (!wide.isVisible()) {
            return;
        }
        NinePatchDrawable drawable = isHeldDown() ? wideDown : wideUp;
        wideBackground.setDrawable(drawable);
        wideBackground.setSize(getWidth(), drawable.getMinHeight());
        // the middle of the button's face (pressed, the piece is lower)
        float middle = (drawable.getMinHeight() + drawable.getBottomHeight() - drawable.getTopHeight()) / 2f;
        going.setPosition(WIDE_INSET, Math.round(middle - WIDE_ITEM_SIZE / 2f));
        coming.setPosition(getWidth() - WIDE_INSET - WIDE_ITEM_SIZE, going.getY());
        arrows.setPosition(Math.round((getWidth() - arrows.getWidth()) / 2f),
                Math.round(middle - arrows.getHeight() / 2f));
    }

    /** The tool on it, e.g. "scythe", {@link #SWAP} while it is wide, or {@link #HINT} for the question mark. */
    public String getToolName() {
        return toolName;
    }

    /** How what would go in the swap on offer is tinted now: "ffffffff" in its own colours. */
    public String getGoingTint() {
        return going.getColor().toString();
    }

    /** While it offers a swap: what goes (left) and what comes (right); null otherwise. */
    public TextureRegion[] getSwapPictures() {
        if (!wide.isVisible()) {
            return null;
        }
        return new TextureRegion[]{((TextureRegionDrawable) going.getDrawable()).getRegion(),
                ((TextureRegionDrawable) coming.getDrawable()).getRegion()};
    }
}
