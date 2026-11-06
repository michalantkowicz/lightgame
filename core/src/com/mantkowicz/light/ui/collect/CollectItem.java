package com.mantkowicz.light.ui.collect;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.resources.ResourcesService;

import static com.mantkowicz.light.ui.collect.CollectWindowItemState.UNCHOSEN;

public class CollectItem extends Button {
    private final Drawable unchosenDownDrawable;
    private final Drawable unchosenUpDrawable;
    private final Drawable chosenUpDrawable;
    private final Drawable ableToPutDrawable;

    private final CollectItemType collectItemType;
    private CollectWindowItemState state;

    private final ResourcesService resourcesService;
    private Item item;
    private Image itemImage;

    public CollectItem(ResourcesService resourcesService, CollectItemType collectItemType, Item item) {
        super(getButtonStyle(resourcesService));
        this.resourcesService = resourcesService;
        this.item = item;
        this.collectItemType = collectItemType;
        this.state = UNCHOSEN;

        tryAddItemImage(item);

        unchosenUpDrawable = resourcesService.getDrawable("item_background.png");
        unchosenDownDrawable = resourcesService.getDrawable("item_background_pressed.png");
        chosenUpDrawable = resourcesService.getDrawable("item_background_chosen.png");
        ableToPutDrawable = resourcesService.getDrawable("item_background_able_to_put.png");
    }

    public CollectWindowItemState getState() {
        return state;
    }

    public CollectItem setState(CollectWindowItemState state) {
        this.state = state;
        switch (state) {
            case UNCHOSEN:
                getStyle().up = unchosenUpDrawable;
                break;
            case CHOSEN:
                getStyle().up = chosenUpDrawable;
                break;
            case ABLE_TO_PUT:
                getStyle().up = ableToPutDrawable;
                break;
        }
        return this;
    }

    private static ButtonStyle getButtonStyle(ResourcesService resourcesService) {
        ButtonStyle buttonStyle = new ButtonStyle();

        buttonStyle.up = resourcesService.getDrawable("item_background.png");
        buttonStyle.down = resourcesService.getDrawable("item_background_pressed.png");

        return buttonStyle;
    }

    public boolean isEmpty() {
        return item == null;
    }

    public Item getItem() {
        return item;
    }

    public Item removeItem() {
        removeActor(itemImage);
        return item;
    }

    public CollectItem setItem(Item item) {
        this.item = item;
        tryAddItemImage(item);
        return this;
    }

    private void tryAddItemImage(Item item) {
        if (item != null) {
            TextureRegion thumbnail = resourcesService.getThumbnail(item.getThumbnailType());
            itemImage = new Image(thumbnail);
            add(itemImage);
        } else {
            itemImage = null;
        }
    }

    public CollectItemType getCollectItemType() {
        return collectItemType;
    }
}
