package com.mantkowicz.light.ui.window;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.mantkowicz.light.actor.Collectible;
import com.mantkowicz.light.actor.Collecting;
import com.mantkowicz.light.actor.GameActor;
import com.mantkowicz.light.actor.Item;
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
import static com.mantkowicz.light.ui.window.CollectWindowItemState.UNCHOSEN;

public class CollectWindow extends GameActor {
    private final int WIDTH = 600;
    private final int HEIGHT = 600;
    private final int PADDING = 20;
    private final int PADDING_ITEM = 20;
    private final Collectible collectible;
    private final Collecting collecting;
    private final Skin skin;
    private final ResourcesService resourcesService;
    private final List<CollectItem> collectItems;
    private CollectItem chosenItem;
    private Table mainTable;

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
        backgroundTable.addListener(new StopClickThroughClickListener());
        TextureRegionDrawable backgroundDrawable = new TextureRegionDrawable(new TextureRegion(resourcesService.getTexture("black.png")));
        backgroundTable.setBackground(backgroundDrawable);
        backgroundTable.setPosition(-backgroundTable.getWidth() / 2f, -backgroundTable.getHeight() / 2f);
        backgroundTable.debug();
        addActor(backgroundTable);

        mainTable = new Table();
        mainTable.setTouchable(Touchable.enabled);
        mainTable.setSize(WIDTH, HEIGHT);

        mainTable.addListener(new StopClickThroughClickListener());

        TextureRegionDrawable textureRegionDrawable = new TextureRegionDrawable(new TextureRegion(resourcesService.getTexture("table_background.png")));
        mainTable.setBackground(textureRegionDrawable);

        mainTable.add(getContent(HEIGHT - 2 * PADDING)).pad(PADDING).expand().center();

        addActor(mainTable);

        setSize(mainTable.getWidth(), mainTable.getHeight());

        Plugin plugin = new CenterActorByCameraPlugin(this, context.getCamera(), new Vector2());
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

        Table collectibleTable = getCollectibleTable();
        Table inventoryTable = getInventoryTable();
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

        cancelButton.addListener(new CollectWindowCancelListener(this));
        okButton.addListener(new CollectWindowOkListener(this));

        Cell<TextButton> cancelButtonCell = buttonsTable.add(cancelButton);
        setItemPadding(cancelButtonCell);

        Cell<TextButton> okButtonCell = buttonsTable.add(okButton);
        setItemPadding(okButtonCell);

        return buttonsTable;
    }

    private Table getInventoryTable() {
        Table inventoryTable = new Table();
        for (Item collectingItem : collecting.getInventory().getItems()) {
            CollectItem item = new CollectItem(resourcesService, COLLECTING_ITEM, collectingItem);
            item.addListener(new CollectWindowItemListener(this, item));
            collectItems.add(item);

            Cell<CollectItem> cell = inventoryTable.add(item);
            setItemPadding(cell);
        }
        return inventoryTable;
    }

    private Table getCollectibleTable() {
        Table collectibleTable = new Table();
        for (Item collectibleItem : collectible.getInventory().getItems()) {
            CollectItem item = new CollectItem(resourcesService, COLLECTIBLE_ITEM, collectibleItem);
            item.addListener(new CollectWindowItemListener(this, item));
            collectItems.add(item);

            Cell<CollectItem> cell = collectibleTable.add(item);
            setItemPadding(cell);
        }
        return collectibleTable;
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
