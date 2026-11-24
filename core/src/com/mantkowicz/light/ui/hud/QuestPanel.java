package com.mantkowicz.light.ui.hud;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.quest.QuestJournal;
import com.mantkowicz.light.quest.QuestRun;
import com.mantkowicz.light.quest.Thing;
import com.mantkowicz.light.resources.ResourcesService;
import com.mantkowicz.light.resources.ShaderType;
import com.mantkowicz.light.resources.UiImage;
import com.mantkowicz.light.ui.UiStyle;

import java.util.function.BooleanSupplier;

/**
 * The quest going on, in a row just over the backpack slots ({@link Hud} places it), pictures only: over the first two
 * slots the pack's brown rectangle button with who asks (the animal's face), an arrow and what it asks for, and under
 * them a line with the time left, shrinking from the right: green, yellow from {@link #YELLOW} of the time left, red
 * from {@link #RED}. On its top left corner a small round button with a red X: tapped, the quest is cancelled. Over the
 * third slot the quest's slot, golden: empty it shows the dark brown shape of what is wanted, else what was picked up
 * for the quest (tapped, it jumps out). Hidden while no quest goes on.
 */
public class QuestPanel extends Group {
    /** The time line turns yellow, then red, when this much of the time is left. */
    static final float YELLOW = 0.3f, RED = 0.1f;
    /** The quest's slot: the backpack's slot, tinted golden. */
    private static final Color GOLD = new Color(1f, 0.93f, 0.45f, 1);
    /** The shape of what is wanted in the empty slot: the dark brown of a chosen swap. */
    private static final Color SHAPE = new Color(0.42f, 0.26f, 0.16f, 1);
    private static final Color TRACK = new Color(0.35f, 0.24f, 0.17f, 1), GREEN = new Color(0.45f, 0.78f, 0.36f, 1),
            AMBER = new Color(0.95f, 0.8f, 0.3f, 1), ALARM = new Color(0.9f, 0.32f, 0.27f, 1);
    /** Screen pixels per pack pixel of the button, the X's button, the arrow, the pictures. */
    private static final float BUTTON_SCALE = 3, X_SCALE = 2, ARROW_SCALE = 2, PICTURE_SCALE = 3, FACE_SCALE = 1.5f;
    /**
     * The X's round button sticks out of the brown button's top left corner this far (both ways); the pictures start
     * right of it. The line is this far from the button's sides and bottom, this thick.
     */
    private static final float X_OUT = 14, PICTURES_LEFT = 24, LINE_SIDE = 15, LINE_BOTTOM = 20, LINE_HEIGHT = 8;
    /** An animal's face (an emoji) is this many pack pixels wide. */
    private static final float FACE = 28;
    /** The pictures' middles this far down from the button's top (over the line). */
    private static final float PICTURES_MIDDLE = 36.5f;

    private final ResourcesService resources;
    private final QuestJournal journal;
    private final Image face = new Image(), wanted = new Image(), slotItem = new Image(), line;
    private final Silhouette shape;
    private final Image slot;
    private final float lineWidth;
    private String shownThing;
    /** Items flying into the quest's slot: it shows what is in it once they have landed. */
    private int arriving;
    /** Shown this much longer after the quest has ended (what was given from its slot is still flying). */
    private float holding;

    /**
     * @param width        as wide as the first two backpack slots with the gap between them
     * @param gap          between two backpack slots: between the button and the quest's slot
     * @param slotWidth    the backpack slot's size: the quest's slot's, over the third one
     * @param throwOutSlot throws what is in the quest's slot out next to the player; false when it has nowhere to land
     *                     (the slot shakes)
     */
    QuestPanel(ResourcesService resources, QuestJournal journal, float width, float gap, float slotWidth,
            float slotHeight, BooleanSupplier throwOutSlot) {
        this.resources = resources;
        this.journal = journal;
        setSize(width + gap + slotWidth, slotHeight);
        setTouchable(Touchable.childrenOnly);

        NinePatch patch = new NinePatch(resources.getUiNinePatch(UiImage.QUEST_BUTTON));
        patch.scale(BUTTON_SCALE, BUTTON_SCALE);
        Image button = new Image(new NinePatchDrawable(patch));
        button.setBounds(0, 0, width, slotHeight);
        button.setName("QUEST ROW");
        button.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true; // a tap on the row stays here: it doesn't walk the player to the tile under it
            }
        });
        addActor(button);

        TextureRegion white = whitePixel();
        lineWidth = width - 2 * LINE_SIDE;
        addActor(box(white, TRACK, LINE_SIDE, LINE_BOTTOM, lineWidth, LINE_HEIGHT));
        line = box(white, GREEN, LINE_SIDE, LINE_BOTTOM, lineWidth, LINE_HEIGHT);
        addActor(line);

        addPictures(width, slotHeight);
        addCancelButton(slotHeight);

        slot = new Image(UiStyle.stretching(resources, UiImage.SLOT));
        slot.setBounds(width + gap, 0, slotWidth, slotHeight);
        slot.setColor(GOLD);
        slot.setName("QUEST SLOT");
        slot.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (!throwOutSlot.getAsBoolean()) {
                    slot.addAction(Actions.sequence(Actions.moveBy(0, 6, 0.04f), Actions.moveBy(0, -12, 0.08f),
                            Actions.moveBy(0, 6, 0.04f))); // nowhere to land: it shakes
                }
            }
        });
        addActor(slot);
        shape = new Silhouette(new ShaderProgram(resources.getShaderVertex(ShaderType.SILHOUETTE),
                resources.getShaderFragment(ShaderType.SILHOUETTE)));
        shape.setColor(SHAPE);
        float shapeSize = 16 * PICTURE_SCALE;
        place(shape, slot.getX() + (slotWidth - shapeSize) / 2f, (slotHeight - shapeSize) / 2f + 3, shapeSize);
        float itemSize = BackpackSlots.ITEM_SIZE; // as in a backpack slot
        place(slotItem, slot.getX() + (slotWidth - itemSize) / 2f, (slotHeight - itemSize) / 2f + 4, itemSize);
        setVisible(false);
    }

    /** Who asks, an arrow, what for: spread evenly right of the X's button, their middles over the line. */
    private void addPictures(float width, float height) {
        TextureRegion arrowPicture = resources.getUiImage(UiImage.QUEST_ARROW);
        float faceSize = FACE * FACE_SCALE, pictureSize = 16 * PICTURE_SCALE;
        float arrowWidth = arrowPicture.getRegionWidth() * ARROW_SCALE;
        float arrowHeight = arrowPicture.getRegionHeight() * ARROW_SCALE;
        float gap = (width - PICTURES_LEFT - faceSize - arrowWidth - pictureSize) / 4f;
        float middle = height - PICTURES_MIDDLE;
        float x = PICTURES_LEFT + gap;
        place(face, x, middle - faceSize / 2f, faceSize);
        x += faceSize + gap;
        Image arrow = new Image(arrowPicture);
        arrow.setBounds(x, middle - arrowHeight / 2f, arrowWidth, arrowHeight);
        arrow.setTouchable(Touchable.disabled);
        addActor(arrow);
        x += arrowWidth + gap;
        place(wanted, x, middle - pictureSize / 2f, pictureSize);
    }

    /** The small beige round button with the red X over the brown button's top left corner: it cancels the quest. */
    private void addCancelButton(float height) {
        TextureRegion round = resources.getUiImage(UiImage.SMALL_BEIGE_BUTTON);
        TextureRegion cross = resources.getUiImage(UiImage.RED_CLOSE_ICON);
        Group cancel = new Group();
        float size = 16 * BUTTON_SCALE;
        cancel.setBounds(-X_OUT, height + X_OUT - size, size, size);
        Image button = new Image(round);
        button.setScaling(Scaling.fit);
        button.setSize(size, size);
        cancel.addActor(button);
        Image x = new Image(cross);
        float crossWidth = cross.getRegionWidth() * X_SCALE, crossHeight = cross.getRegionHeight() * X_SCALE;
        x.setBounds((size - crossWidth) / 2f, (size - crossHeight) / 2f + 1, crossWidth, crossHeight);
        x.setTouchable(Touchable.disabled);
        cancel.addActor(x);
        cancel.setName("CANCEL QUEST");
        cancel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                journal.cancel();
            }
        });
        addActor(cancel);
    }

    private static TextureRegion whitePixel() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        TextureRegion region = new TextureRegion(new Texture(pixmap));
        pixmap.dispose();
        return region;
    }

    private Image box(TextureRegion white, Color color, float x, float y, float width, float height) {
        Image box = new Image(white);
        box.setColor(color);
        box.setBounds(x, y, width, height);
        box.setTouchable(Touchable.disabled);
        return box;
    }

    /** In a square this big, its picture fitted in, keeping its shape. */
    private void place(Image image, float x, float y, float size) {
        image.setScaling(Scaling.fit);
        image.setBounds(x, y, size, size);
        image.setTouchable(Touchable.disabled);
        addActor(image);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        QuestRun run = journal.getActive();
        holding = run == null ? Math.max(0, holding - delta) : holding;
        setVisible(run != null || holding > 0);
        if (run == null) { // held: as it was, but what was in the slot is flying away
            slotItem.setDrawable(null);
            shape.setVisible(true);
            return;
        }
        face.setDrawable(new TextureRegionDrawable(resources.getUiImage(run.getOwner().getIcon())));
        Thing thing = run.getWaitingFor();
        if (thing != null && !thing.getName().equals(shownThing)) {
            shownThing = thing.getName();
            TextureRegionDrawable picture = new TextureRegionDrawable(thing.create(resources).getPicture());
            wanted.setDrawable(picture);
            shape.setDrawable(picture);
        }
        Item inSlot = journal.getSlot();
        slotItem.setDrawable(inSlot == null ? null : new TextureRegionDrawable(inSlot.getPicture()));
        slotItem.setVisible(arriving == 0);
        shape.setVisible(inSlot == null || arriving > 0);
        float left = run.getTimeFraction();
        line.setWidth(lineWidth * left);
        line.setColor(left <= RED ? ALARM : left <= YELLOW ? AMBER : GREEN);
    }

    /** The quest's slot (where items fly to and from). */
    Image getSlot() {
        return slot;
    }

    /** It stays shown this long even if the quest ends meanwhile (what was given from its slot is flying). */
    void holdFor(float seconds) {
        holding = Math.max(holding, seconds);
    }

    /** An item is flying into the slot: it shows its shape until it lands ({@link #arrived}). */
    void expectArrival() {
        arriving++;
    }

    void arrived() {
        arriving = Math.max(0, arriving - 1);
    }

    /** How much of the time line is left (0..1), and its colour: for tests. */
    public String describeLine() {
        return Math.round(line.getWidth() / lineWidth * 100) + "% " + line.getColor();
    }

    /** A picture's shape, flat in its colour (the silhouette shader): what the empty quest slot waits for. */
    private static final class Silhouette extends Image {
        private final ShaderProgram shader;

        Silhouette(ShaderProgram shader) {
            this.shader = shader;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            ShaderProgram before = batch.getShader();
            batch.setShader(shader);
            super.draw(batch, parentAlpha);
            batch.setShader(before);
        }
    }
}
