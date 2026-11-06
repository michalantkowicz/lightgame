package com.mantkowicz.light.ui.window;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.actor.Collecting;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.actor.Inventory;
import com.mantkowicz.light.actor.Item;
import com.mantkowicz.light.debug.Debug;
import com.mantkowicz.light.game.GameContext;
import com.mantkowicz.light.plugin.Plugin;
import com.mantkowicz.light.plugin.implementation.CenterActorByCameraPlugin;
import com.mantkowicz.light.service.resources.ResourcesService;

import java.util.ArrayList;
import java.util.List;

import static com.mantkowicz.light.actor.GameActorType.COLLECT_WINDOW;
import static com.mantkowicz.light.ui.window.CollectItemType.COLLECTIBLE_ITEM;
import static com.mantkowicz.light.ui.window.CollectItemType.COLLECTING_ITEM;
import static com.mantkowicz.light.ui.window.CollectWindowItemState.ABLE_TO_PUT;
import static com.mantkowicz.light.ui.window.CollectWindowItemState.CHOSEN;
import static com.mantkowicz.light.ui.window.CollectWindowItemState.UNCHOSEN;
import static com.mantkowicz.light.debug.Debug.Tool.COLLECT_WINDOW_LINES;

public class CollectWindow extends GameActor {
    private static final int WIDTH = 600;
    private static final int HEIGHT = 600;
    private static final int PADDING = 20;
    private static final int PADDING_ITEM = 20;
    private static final CollectItem NO_ITEM = null;
    private final Collectible collectible;
    private final Collecting collecting;
    private final Skin skin;
    private final ResourcesService resourcesService;
    private final List<CollectItem> collectItems;
    private CollectItem chosenItem;
    private final Table mainTable;

    public CollectWindow(GameContext context, Collecting collecting, Collectible collectible) {
        super(COLLECT_WINDOW);

        this.collectible = collectible;
        this.collecting = collecting;
        this.skin = context.getResourcesService().getSkin();
        this.resourcesService = context.getResourcesService();
        this.collectItems = new ArrayList<>();

        Table backgroundTable = new Table();
        backgroundTable.setSize(context.getMenuStage().getWidth() * 2, context.getMenuStage().getHeight() * 2);
        backgroundTable.setTouchable(Touchable.enabled);
        backgroundTable.addListener(stopClickThrough());
        TextureRegionDrawable backgroundDrawable = new TextureRegionDrawable(new TextureRegion(resourcesService.getTexture("black.png")));
        backgroundTable.setBackground(backgroundDrawable);
        backgroundTable.setPosition(-backgroundTable.getWidth() / 2f, -backgroundTable.getHeight() / 2f);
        if (Debug.isOn(COLLECT_WINDOW_LINES)) {
            backgroundTable.debug();
        }
        addActor(backgroundTable);

        mainTable = new Table();
        mainTable.setTouchable(Touchable.enabled);
        mainTable.setSize(WIDTH, HEIGHT);

        mainTable.addListener(stopClickThrough());

        TextureRegionDrawable textureRegionDrawable = new TextureRegionDrawable(new TextureRegion(resourcesService.getTexture("table_background.png")));
        mainTable.setBackground(textureRegionDrawable);

        mainTable.add(getContent(HEIGHT - 2 * PADDING)).pad(PADDING).expand().center();

        addActor(mainTable);

        setSize(mainTable.getWidth(), mainTable.getHeight());

        Plugin plugin = new CenterActorByCameraPlugin(this, context, new Vector2());
        addPlugin(plugin);
    }

    public void onItemChosen() {
        for (CollectItem item : collectItems) {
            if (UNCHOSEN.equals(item.getState()) && item.isEmpty()) {
                item.setState(ABLE_TO_PUT);
            }
        }
    }

    public void onItemUnchosen() {
        for (CollectItem item : collectItems) {
            item.setState(UNCHOSEN);
        }
    }

    private Table getContent(int contentHeight) {
        Table table = new Table();

        Table collectibleTable = createItemsTable(collectible.getInventory(), COLLECTIBLE_ITEM);
        Table inventoryTable = createItemsTable(collecting.getInventory(), COLLECTING_ITEM);
        Table buttonsTable = getButtonsTable();

        table.add(collectibleTable).height(contentHeight * 0.3f).row();
        table.add(inventoryTable).height(contentHeight * 0.3f).row();
        table.add(buttonsTable).height(contentHeight * 0.4f).row();

        return table;
    }

    private Table getButtonsTable() {
        Table buttonsTable = new Table();
        TextButton cancelButton = new TextButton("CANCEL", skin, "no");
        TextButton okButton = new TextButton("OK", skin, "ok");

        cancelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                remove();
            }
        });
        okButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                saveState();
                remove();
            }
        });

        Cell<TextButton> cancelButtonCell = buttonsTable.add(cancelButton);
        setItemPadding(cancelButtonCell);

        Cell<TextButton> okButtonCell = buttonsTable.add(okButton);
        setItemPadding(okButtonCell);

        return buttonsTable;
    }

    private Table createItemsTable(Inventory inventory, CollectItemType collectItemType) {
        Table itemsTable = new Table();
        for (Item inventoryItem : inventory.getItems()) {
            CollectItem item = new CollectItem(resourcesService, collectItemType, inventoryItem);
            item.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onItemClicked(item);
                }
            });
            collectItems.add(item);

            Cell<CollectItem> cell = itemsTable.add(item);
            setItemPadding(cell);
        }
        return itemsTable;
    }

    /**
     * First click chooses an item, a click on a free slot swaps the chosen item there, a click on the chosen item unchooses it.
     */
    private void onItemClicked(CollectItem clickedItem) {
        CollectWindowItemState itemState = clickedItem.getState();
        if (UNCHOSEN.equals(itemState) && !isAnyItemChosen() && !clickedItem.isEmpty()) {
            clickedItem.setState(CHOSEN);
            setChosenItem(clickedItem);
            onItemChosen();
        } else if (ABLE_TO_PUT.equals(itemState) && isAnyItemChosen()) {
            Item movedItem = chosenItem.removeItem();
            Item replacedItem = clickedItem.removeItem();

            clickedItem.setItem(movedItem);
            chosenItem.setItem(replacedItem);

            setChosenItem(NO_ITEM);
            onItemUnchosen();
        } else if (CHOSEN.equals(itemState)) {
            clickedItem.setState(UNCHOSEN);
            setChosenItem(NO_ITEM);
            onItemUnchosen();
        }
    }

    /**
     * Swallows touches so they don't reach the board under the window.
     */
    private static ClickListener stopClickThrough() {
        return new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }
        };
    }

    private void setItemPadding(Cell cell) {
        cell.padRight(PADDING_ITEM / 2f).padLeft(PADDING_ITEM / 2f).padTop(PADDING_ITEM).padBottom(PADDING_ITEM);
    }

    public boolean isAnyItemChosen() {
        return chosenItem != null;
    }

    public CollectItem getChosenItem() {
        return chosenItem;
    }

    public CollectWindow setChosenItem(CollectItem chosenItem) {
        this.chosenItem = chosenItem;
        return this;
    }

    public void saveState() {
        List<Item> collectingItemsList = getItemsByType(COLLECTING_ITEM);
        List<Item> collectibleItemsList = getItemsByType(COLLECTIBLE_ITEM);

        collecting.getInventory().setItemsAndCompleteWithNulls(collectingItemsList);
        collectible.getInventory().setItemsAndCompleteWithNulls(collectibleItemsList);
    }

    private List<Item> getItemsByType(CollectItemType collectItemType) {
        List<Item> result = new ArrayList<>();
        for (CollectItem collectItem : collectItems) {
            if (collectItemType.equals(collectItem.getCollectItemType())) {
                result.add(collectItem.getItem());
            }
        }
        return result;
    }
}
