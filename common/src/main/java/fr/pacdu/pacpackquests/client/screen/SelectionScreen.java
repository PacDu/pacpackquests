package fr.pacdu.pacpackquests.client.screen;

import fr.pacdu.pacpackquests.client.ClientQuestData;
import fr.pacdu.pacpackquests.client.gui.ScrollbarWidget;
import fr.pacdu.pacpackquests.client.util.DrawUtils;
import fr.pacdu.pacpackquests.client.util.IconUtils;
import fr.pacdu.pacpackquests.client.util.TranslationUtils;
import fr.pacdu.pacpackquests.network.RequestRegistryPayload;
import fr.pacdu.pacpackquests.platform.Services;
import fr.pacdu.pacpackquests.util.ItemUtils;
import fr.pacdu.pacpackquests.util.RegistryType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Consumer;

/**
 * Screen for selecting elements across various Minecraft registries (items, blocks, entities, biomes, etc.).
 * For items, it replicates JEI's creative tab ordering, multi-line tooltips (mod name, components, IDs),
 * and advanced search (@mod, #tags, names, and tooltip contents).
 */
public class SelectionScreen extends Screen {

    /**
     * Cached entry data for efficient search and rendering.
     */
    private record SelectionEntry(String id, ItemStack stack, String displayName, String namespace, String modName,
                                  String path, String searchableTooltip, boolean isTag) {}

    private final Screen parent;
    private final RegistryType contextType;
    private final Consumer<String> onSelected;
    private final String previouslySelectedEntry;

    private EditBox searchBox;
    private final List<SelectionEntry> allEntries = new ArrayList<>();
    private final List<SelectionEntry> filteredEntries = new ArrayList<>();

    private ScrollbarWidget gridScrollbar = new ScrollbarWidget(0, 0, 6, 0, 1.0, false);

    private int columns;
    private int rows;
    private int maxVisible;
    private boolean withTags = true;
    private boolean isReward = false;

    public SelectionScreen(Screen parent, RegistryType contextType, Consumer<String> onSelected, String previouslySelectedEntry) {
        super(Component.translatable("gui.pacpackquests.select_element"));
        this.parent = parent;
        this.contextType = contextType;
        this.onSelected = onSelected;
        this.previouslySelectedEntry = previouslySelectedEntry;
    }

    public SelectionScreen(Screen parent, RegistryType contextType, Consumer<String> onSelected, String previouslySelectedEntry, boolean withTags) {
        this(parent, contextType, onSelected, previouslySelectedEntry);
        this.withTags = withTags;
    }

    public SelectionScreen(Screen parent, RegistryType contextType, Consumer<String> onSelected, String previouslySelectedEntry, boolean withTags, boolean isReward) {
        this(parent, contextType, onSelected, previouslySelectedEntry, withTags);
        this.isReward = isReward;
    }

    @Override
    protected void init() {
        super.init();

        this.columns = Math.clamp(this.width / 18 - 5, 9, 64);
        this.rows = this.height / 18 - 5;
        this.maxVisible = this.columns * this.rows;

        // 1. Search Box
        int searchWidth = 160;
        this.searchBox = new EditBox(this.font, (this.width - searchWidth) / 2, 20, searchWidth, 20, Component.empty());
        this.searchBox.setMaxLength(128);
        this.searchBox.setHint(Component.translatable("gui.pacpackquests.search").withStyle(ChatFormatting.DARK_GRAY));
        this.searchBox.setResponder(this::updateSearch);
        this.addRenderableWidget(this.searchBox);
        this.setFocused(this.searchBox);

        // 2. Cancel Button
        this.addRenderableWidget(Button.builder(Component.translatable("gui.pacpackquests.cancel"), button -> this.minecraft.setScreen(this.parent))
                .bounds((this.width - 100) / 2, this.height - 30, 100, 20).build());

        // 3. Populate initial data based on context
        this.populateEntries();
        this.updateSearch("");
    }

    /**
     * Populates all available entries based on current registry context.
     */
    private void populateEntries() {
        allEntries.clear();
        var world = this.minecraft.level;
        HolderLookup.Provider registries = world != null ? world.registryAccess() : RegistryAccess.EMPTY;
        boolean addPreviousEntry = true;

        switch (contextType) {
            case ITEM -> {
                // 1. Ensure creative search tab stacks are initialized
                Collection<ItemStack> searchStacks = CreativeModeTabs.searchTab().getSearchTabDisplayItems();
                if (searchStacks.isEmpty() && world != null) {
                    var enabledFeatures = world.enabledFeatures();
                    boolean operator = this.minecraft.player != null && this.minecraft.player.canUseGameMasterBlocks();
                    CreativeModeTabs.tryRebuildTabContents(enabledFeatures, operator, registries);
                    searchStacks = CreativeModeTabs.searchTab().getSearchTabDisplayItems();
                }

                if (searchStacks.isEmpty()) {
                    searchStacks = BuiltInRegistries.ITEM.stream().map(Item::getDefaultInstance).toList();
                }

                // 2. Populate stacks in creative tab order
                for (ItemStack stack : searchStacks) {
                    if (stack.isEmpty()) continue;
                    addEntry(ItemUtils.toCommandString(stack, registries));
                }

                // 3. Add tags last if enabled
                if (this.withTags) {
                    BuiltInRegistries.ITEM.getTags().forEach(tag -> {
                        Identifier tagId = tag.key().location();
                        String tagString = "#" + tagId;
                        addEntry(tagString);
                    });
                }
            }
            case BLOCK -> {
                BuiltInRegistries.BLOCK.keySet().forEach(id -> addEntry(id.toString()));
                if (this.withTags) {
                    BuiltInRegistries.BLOCK.getTags().forEach(tag -> addEntry("#" + tag.key().location().toString()));
                }
                this.allEntries.sort(this::compareEntries);
            }
            case MOB -> {
                java.util.Set<Identifier> validMobs = new java.util.HashSet<>();

                // Filter the raw entity IDs to keep only real mobs
                BuiltInRegistries.ENTITY_TYPE.keySet().forEach(id -> {
                    var type = BuiltInRegistries.ENTITY_TYPE.getValue(id);
                    var group = type.getCategory();

                    // Check if a spawn egg exists for this entity (covers MISC mobs like Wither, Villagers, Golems)
                    Identifier eggId = Identifier.fromNamespaceAndPath(id.getNamespace(), id.getPath() + "_spawn_egg");
                    boolean hasSpawnEgg = BuiltInRegistries.ITEM.containsKey(eggId);

                    // A valid mob is either in a natural spawn group, has a spawn egg, or is the player explicitly
                    if (group != MobCategory.MISC || hasSpawnEgg || id.getPath().equals("player")) {
                        validMobs.add(id);
                        addEntry(id.toString());
                    }
                });

                // Add tag only if at least ONE of its entities is a valid mob
                BuiltInRegistries.ENTITY_TYPE.getTags().forEach(tagList -> {
                    boolean hasValidMob = tagList.stream().anyMatch(entry -> validMobs.contains(BuiltInRegistries.ENTITY_TYPE.getKey(entry.value())));
                    if (hasValidMob) {
                        addEntry("#" + tagList.key().location().toString());
                    }
                });
                this.allEntries.sort(this::compareEntries);
            }
            case BIOME -> {
                if (world != null) {
                    var registryOpt = world.registryAccess().lookup(Registries.BIOME);
                    if (registryOpt.isPresent()) {
                        var registry = registryOpt.get();
                        registry.listElementIds().forEach(id -> addEntry(id.identifier().toString()));
                        registry.listTags().forEach(tag -> addEntry("#" + tag.key().location().toString()));
                        this.allEntries.sort(this::compareEntries);
                    }
                }
            }
            case STRUCTURE -> {
                var server = Minecraft.getInstance().getSingleplayerServer();
                if (server != null) {
                    var registryOpt = server.registryAccess().lookup(Registries.STRUCTURE);
                    if (registryOpt.isPresent()) {
                        var registry = registryOpt.get();
                        registry.listElementIds().forEach(id -> addEntry(id.identifier().toString()));
                        registry.listTags().forEach(tag -> addEntry("#" + tag.key().location().toString()));
                        this.allEntries.sort(this::compareEntries);
                    }
                } else {
                    addPreviousEntry = false;
                    refreshEntries(ClientQuestData.SERVER_STRUCTURES);
                    Services.NETWORK.sendToServer(new RequestRegistryPayload(RegistryType.STRUCTURE));
                }
            }
            case DIMENSION -> {
                var handler = Minecraft.getInstance().getConnection();
                if (handler != null) {
                    handler.levels().forEach(key -> addEntry(key.identifier().toString()));
                    this.allEntries.sort(this::compareEntries);
                }
            }
            case LOOT_TABLE -> {
                addEntry("#pacpackquests:loot_chest");
                var server = Minecraft.getInstance().getSingleplayerServer();
                if (server != null) {
                    var opt = server.reloadableRegistries().lookup().lookup(Registries.LOOT_TABLE);
                    opt.ifPresent(lootTableImpl -> lootTableImpl.listElementIds().forEach(key -> {
                        Identifier id = key.identifier();
                        if (id.getPath().startsWith("chests/")) {
                            addEntry(id.toString());
                        }
                    }));
                    this.allEntries.sort(this::compareEntries);
                } else {
                    addPreviousEntry = false;
                    refreshEntries(ClientQuestData.SERVER_LOOT_TABLES);
                    Services.NETWORK.sendToServer(new RequestRegistryPayload(RegistryType.LOOT_TABLE));
                }
            }
            case COMMAND -> {}
        }

        if (addPreviousEntry) {
            removeEntry(this.previouslySelectedEntry);
            addEntry(this.previouslySelectedEntry, true);
        }
    }

    private void addEntry(String idStr) {
        addEntry(idStr, false);
    }

    private void addEntry(String idStr, boolean atFirst) {
        if (idStr == null || idStr.isEmpty()) return;

        boolean isTag = idStr.startsWith("#");
        String raw = isTag ? idStr.substring(1) : idStr;
        if (raw.contains("[")) {
            raw = raw.substring(0, raw.indexOf('['));
        }

        Identifier id = Identifier.tryParse(raw);
        String namespace = id != null ? id.getNamespace() : "minecraft";
        String path = id != null ? id.getPath() : raw;
        String modName = TranslationUtils.getModName(namespace);
        ItemStack stack = IconUtils.getRepresentativeItem(idStr, this.contextType);
        String displayName = TranslationUtils.getTranslatedName(idStr, this.contextType, this.isReward);
        String tooltip = getTooltipText(stack);

        SelectionEntry entry = new SelectionEntry(idStr, stack, displayName, namespace, modName, path, tooltip, isTag);
        if (atFirst) {
            allEntries.addFirst(entry);
        } else {
            allEntries.add(entry);
        }
    }

    private void removeEntry(String idStr) {
        allEntries.removeIf(entry -> entry.id.equals(idStr));
    }

    // Precompute searchable tooltip text (for enchantments, potion effects, lore)
    private String getTooltipText(ItemStack stack) {
        StringBuilder tooltipText = new StringBuilder();
        try {
            List<Component> tooltipLines = Screen.getTooltipFromItem(this.minecraft, stack);
            for (Component line : tooltipLines) {
                tooltipText.append(line.getString().toLowerCase(Locale.ROOT)).append(" ");
            }
        } catch (Exception ignored) {}
        return tooltipText.toString();
    }

    public RegistryType getRegistryType() {
        return this.contextType;
    }

    public void refreshEntries(List<String> newEntries) {
        this.allEntries.clear();
        if (this.contextType == RegistryType.LOOT_TABLE) {
            addEntry("#pacpackquests:loot_chest");
        }
        for (String id : newEntries) {
            addEntry(id);
        }
        this.allEntries.sort(this::compareEntries);
        removeEntry(this.previouslySelectedEntry);
        addEntry(this.previouslySelectedEntry, true);
        this.updateSearch(this.searchBox != null ? this.searchBox.getValue() : "");
    }

    /**
     * Filters entries according to user input.
     * Supports:
     * - "@mod" prefix: searches mod namespace or mod title
     * - "#tag" prefix: searches tags
     * - Normal search: matches item name, translation, path, or any text in the tooltip (enchantment name, potion effect)
     */
    private void updateSearch(String query) {
        filteredEntries.clear();
        String trimmed = query.trim();
        String lowerQuery = trimmed.toLowerCase(Locale.ROOT);

        for (SelectionEntry entry : allEntries) {
            if (lowerQuery.isEmpty()) {
                filteredEntries.add(entry);
            } else if (lowerQuery.startsWith("@")) {
                String modQuery = lowerQuery.substring(1).trim();
                if (entry.namespace.toLowerCase(Locale.ROOT).contains(modQuery)
                        || entry.modName.toLowerCase(Locale.ROOT).contains(modQuery)) {
                    filteredEntries.add(entry);
                }
            } else if (lowerQuery.startsWith("#")) {
                String tagQuery = lowerQuery.substring(1).trim();
                if (entry.isTag && (entry.path.toLowerCase(Locale.ROOT).contains(tagQuery)
                        || entry.id.toLowerCase(Locale.ROOT).contains(tagQuery)
                        || entry.displayName.toLowerCase(Locale.ROOT).contains(tagQuery))) {
                    filteredEntries.add(entry);
                }
            } else {
                if (entry.displayName.toLowerCase(Locale.ROOT).contains(lowerQuery)
                        || entry.path.toLowerCase(Locale.ROOT).contains(lowerQuery)
                        || entry.searchableTooltip.contains(lowerQuery)) {
                    filteredEntries.add(entry);
                }
            }
        }

        // Reset scroll when searching
        gridScrollbar = new ScrollbarWidget(0, 0, 6, 0, 1.0, false);

        // Reduce columns number when there are fewer items displayed
        this.columns = Math.clamp(this.filteredEntries.size(), 9, Math.clamp(this.width / 18 - 5, 9, 64));
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredString(this.font, this.title, this.width / 2, 5, 0xFFFFFFFF);

        int startX = (this.width - (columns * 18)) / 2;
        int startY = 50;

        SelectionEntry hoveredEntry = null;

        // Render the Grid
        for (int i = 0; i < maxVisible; i++) {
            int dataIndex = (gridScrollbar.getStepOffset() * columns) + i;
            if (dataIndex >= filteredEntries.size()) break;

            SelectionEntry entry = filteredEntries.get(dataIndex);
            int col = i % columns;
            int row = i / columns;
            int cellX = startX + (col * 18);
            int cellY = startY + (row * 18);

            // Draw Item Icon
            context.renderItem(entry.stack, cellX + 1, cellY + 1);

            // If it's a tag, draw a small yellow '#' overlay
            if (entry.isTag) {
                context.drawString(this.font, "#", cellX + 2, cellY + 2, 0xFFFFFF00, true);
            }

            if (this.previouslySelectedEntry.equals(entry.id)) {
                DrawUtils.drawCheckmark(context, cellX + 11, cellY - 2, 7);
            }

            // Hover logic
            if (mouseX >= cellX && mouseX < cellX + 18 && mouseY >= cellY && mouseY < cellY + 18) {
                context.fill(cellX, cellY, cellX + 18, cellY + 18, 0x88FFFFFF); // Highlight box
                hoveredEntry = entry;
            }
        }

        // Draw JEI-styled Tooltip on top of everything
        if (hoveredEntry != null) {
            List<Component> tooltip = new ArrayList<>();

            if (hoveredEntry.isTag) {
                Identifier tagId = Identifier.tryParse(hoveredEntry.id.substring(1));
                int count = TranslationUtils.getTagSize(tagId, this.contextType);
                String countStr = count >= 0 ? Integer.toString(count) : "?";

                tooltip.add(Component.literal(hoveredEntry.displayName).withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.translatable("gui.pacpackquests.tag")
                        .append(Component.literal(" (" + countStr + " "))
                        .append(Component.translatable("registry.pacpackquests." + contextType.name().toLowerCase(Locale.ROOT)))
                        .append(Component.literal(")"))
                        .withStyle(ChatFormatting.GRAY));
            } else if (!hoveredEntry.stack.isEmpty() && contextType == RegistryType.ITEM) {
                // Vanilla item tooltip (handles item name color, enchantments, potion effects)
                tooltip.addAll(Screen.getTooltipFromItem(this.minecraft, hoveredEntry.stack));
            } else {
                tooltip.add(Component.literal(hoveredEntry.displayName).withStyle(ChatFormatting.GOLD));
            }

            // JEI mod name line in Blue Italic
            tooltip.add(Component.literal(hoveredEntry.modName).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));

            context.setTooltipForNextFrame(this.font, tooltip, Optional.empty(), mouseX, mouseY);
        }

        // Scrollbar Render
        int maxScroll = Math.max(0, (filteredEntries.size() - maxVisible + columns - 1) / columns);
        int scrollX = startX + (columns * 18) + 5;
        gridScrollbar.setBounds(scrollX, startY, 6, rows * 18);
        gridScrollbar.setMaxScroll(maxScroll, rows);
        gridScrollbar.render(context, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (click.button() == 0) { // Left click
            int startX = (this.width - (columns * 18)) / 2;
            int startY = 50;

            if (gridScrollbar.mouseClicked(click.x(), click.y(), 0)) {
                return true;
            }

            // Normal Grid Click Logic
            for (int i = 0; i < maxVisible; i++) {
                int dataIndex = (gridScrollbar.getStepOffset() * columns) + i;
                if (dataIndex >= filteredEntries.size()) break;

                int col = i % columns;
                int row = i / columns;
                int cellX = startX + (col * 18);
                int cellY = startY + (row * 18);

                // If user clicked this cell, trigger callback and close
                if (click.x() >= cellX && click.x() < cellX + 18 && click.y() >= cellY && click.y() < cellY + 18) {
                    this.onSelected.accept(filteredEntries.get(dataIndex).id);
                    this.minecraft.setScreen(this.parent);
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (gridScrollbar.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if (gridScrollbar.mouseDragged(click.x(), click.y(), click.button(), offsetX, offsetY)) return true;
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if (gridScrollbar.mouseReleased(click.x(), click.y(), click.button())) return true;
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.setScreen(this.parent);
            return true;
        } else if (input.key() == GLFW.GLFW_KEY_ENTER) {
            this.onSelected.accept(this.searchBox.getValue());
            this.minecraft.setScreen(this.parent);
            return true;
        }
        return super.keyPressed(input);
    }

    /**
     * Compares entries for non-item contexts.
     * Tags are placed at the very end, preceded by raw numeric ID order or alphabetical order.
     */
    private int compareEntries(SelectionEntry a, SelectionEntry b) {
        if (a.isTag && !b.isTag) return 1;
        if (!a.isTag && b.isTag) return -1;
        if (a.isTag && b.isTag) return a.id.compareTo(b.id);

        int idA = getRawId(a.id);
        int idB = getRawId(b.id);

        if (idA != -1 && idB != -1) {
            return Integer.compare(idA, idB);
        }

        return a.id.compareTo(b.id);
    }

    private int getRawId(String entry) {
        Identifier id = Identifier.tryParse(entry);
        if (id == null) return -1;

        var world = Minecraft.getInstance().level;
        var server = Minecraft.getInstance().getSingleplayerServer();

        return switch (this.contextType) {
            case ITEM -> BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.getId(BuiltInRegistries.ITEM.getValue(id)) : -1;
            case BLOCK -> BuiltInRegistries.BLOCK.containsKey(id) ? BuiltInRegistries.BLOCK.getId(BuiltInRegistries.BLOCK.getValue(id)) : -1;
            case MOB -> BuiltInRegistries.ENTITY_TYPE.containsKey(id) ? BuiltInRegistries.ENTITY_TYPE.getId(BuiltInRegistries.ENTITY_TYPE.getValue(id)) : -1;
            case BIOME -> {
                if (world != null) {
                    var regOpt = world.registryAccess().lookup(Registries.BIOME);
                    if (regOpt.isPresent()) {
                        var reg = regOpt.get();
                        var holderOpt = reg.get(ResourceKey.create(Registries.BIOME, id));
                        yield holderOpt.map(h -> reg.getId(h.value())).orElse(-1);
                    }
                }
                yield -1;
            }
            case STRUCTURE -> {
                if (server != null) {
                    var regOpt = server.registryAccess().lookup(Registries.STRUCTURE);
                    if (regOpt.isPresent()) {
                        var reg = regOpt.get();
                        var holderOpt = reg.get(ResourceKey.create(Registries.STRUCTURE, id));
                        yield holderOpt.map(h -> reg.getId(h.value())).orElse(-1);
                    }
                }
                yield -1;
            }
            case DIMENSION, LOOT_TABLE, COMMAND -> -1;
        };
    }
}
