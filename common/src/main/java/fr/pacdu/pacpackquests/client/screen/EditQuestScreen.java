package fr.pacdu.pacpackquests.client.screen;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import fr.pacdu.pacpackquests.QuestDefinition;
import fr.pacdu.pacpackquests.QuestTask;
import fr.pacdu.pacpackquests.RewardType;
import fr.pacdu.pacpackquests.TaskType;
import fr.pacdu.pacpackquests.client.ClientQuestData;
import fr.pacdu.pacpackquests.client.gui.ScrollbarWidget;
import fr.pacdu.pacpackquests.client.util.IconUtils;
import fr.pacdu.pacpackquests.network.DeleteQuestPayload;
import fr.pacdu.pacpackquests.network.SaveQuestPayload;
import fr.pacdu.pacpackquests.platform.Services;
import fr.pacdu.pacpackquests.util.ItemUtils;
import fr.pacdu.pacpackquests.util.PathSecurity;
import fr.pacdu.pacpackquests.util.RegistryType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static fr.pacdu.pacpackquests.client.util.TranslationUtils.*;

public class EditQuestScreen extends Screen {

    private final QuestScreen parent;
    private final String questId;
    private final String category;
    private final int gridX, gridY;

    // Standard Text Fields
    private EditBox idField, titleField, reqAmountField, rewardAmountField;

    // Selection Buttons
    private Button targetButton, iconButton, parentsButton, rewardButton, taskTypeButton, rewardTypeButton;

    // Dropdown
    private boolean isTaskDropdownOpen = false;
    private boolean isRewardDropdownOpen = false;
    private final ScrollbarWidget dropdownScrollbar = new ScrollbarWidget(0, 0, 6, 0, 1, true);
    private static final int MAX_DROPDOWN_VISIBLE = 4;

    // State variables holding the currently selected Strings
    private String selectedIcon = "minecraft:stone";
    private String selectedTarget = "minecraft:stone";
    private String selectedReward = "minecraft:diamond";

    private TaskType currentTaskType = TaskType.MINE_BLOCK;
    private RewardType currentRewardType = RewardType.ITEM;

    private String selectedCommandDescription = "";
    private String errorMessage = null;
    private List<String> selectedParents = new ArrayList<>();
    private QuestDefinition duplicatedQuest = null;
    private boolean isInitialized = false;
    private boolean isIdManuallyEdited = false;
    private boolean isIconManuallyEdited = false;

    public EditQuestScreen(QuestScreen parent, String questId, String category, int gridX, int gridY) {
        super(Component.literal(questId == null ? I18n.get("gui.pacpackquests.create_new_quest") : I18n.get("gui.pacpackquests.edit_quest")));
        this.parent = parent;
        this.questId = questId;
        this.category = category;
        this.gridX = gridX;
        this.gridY = gridY;
    }

    public EditQuestScreen(QuestScreen parent, String questId, String category, int gridX, int gridY, QuestDefinition duplicatedQuest) {
        this(parent, questId, category, gridX, gridY);
        this.duplicatedQuest = duplicatedQuest;
    }

    @Override
    protected void init() {
        int col1 = this.width / 2 - 155;
        int col2 = this.width / 2 + 5;
        int fieldWidth = 150;
        int fieldHeight = 20;

        int yStep = 34;
        int totalFormHeight = (yStep * 5) + 25;
        int y = Math.max(35, (this.height - totalFormHeight) / 2);

        // --- PREFILL DATA IF EDITING ---
        String initialTitle = "";
        String initialReqAmt = "1";
        String initialRewAmt = "1";
        String initialParents = "";

        // 1. Load data from memory if we are editing a quest
        if ((questId != null && ClientQuestData.CLIENT_DEFINITIONS.containsKey(questId)) || duplicatedQuest != null) {
            QuestDefinition def = duplicatedQuest != null ? duplicatedQuest : ClientQuestData.CLIENT_DEFINITIONS.get(questId);
            initialTitle = def.title();

            if (!this.isInitialized) {
                this.currentTaskType = def.taskType();
                this.currentRewardType = def.rewardType();

                // Only update these variables if they are empty (to avoid overwriting SelectionScreen callbacks)
                if (this.selectedIcon == null || this.selectedIcon.equals("minecraft:stone")) this.selectedIcon = ItemUtils.toCommandString(def.icon());
                if (this.selectedTarget == null || this.selectedTarget.equals("minecraft:stone")) this.selectedTarget = def.target();
                if (this.selectedReward == null || this.selectedReward.equals("minecraft:diamond")) this.selectedReward = def.reward();
                if (this.selectedCommandDescription.isEmpty()) this.selectedCommandDescription = def.rewardDescription();
            }

            initialReqAmt = String.valueOf(def.requiredAmount());
            initialRewAmt = String.valueOf(def.rewardAmount());
            if (def.parents() != null) { initialParents = String.join(",", def.parents()); if (!this.isInitialized) { this.selectedParents = new java.util.ArrayList<>(def.parents()); } }
        }

        this.isInitialized = true;

        // 2. Override initial values with current field contents (if they exist)
        String currentId = this.idField != null ? this.idField.getValue() : duplicatedQuest != null ? duplicatedQuest.id() : "";
        if (this.titleField != null) initialTitle = this.titleField.getValue();
        if (this.reqAmountField != null) initialReqAmt = this.reqAmountField.getValue();
        if (this.rewardAmountField != null) initialRewAmt = this.rewardAmountField.getValue();
        if (this.selectedParents != null) initialParents = String.join(",", this.selectedParents);

        // ROW 1: Title & Parents
        this.titleField = new EditBox(this.font, col1, y, fieldWidth, fieldHeight, Component.translatable("form.pacpackquests.title"));
        this.titleField.setMaxLength(96);
        this.titleField.setValue(initialTitle);
        this.titleField.setHint(Component.translatable("form.pacpackquests.title_placeholder").withStyle(ChatFormatting.DARK_GRAY));
        this.addRenderableWidget(this.titleField);

        this.parentsButton = Button.builder(Component.literal(initialParents.isEmpty() ? "None" : initialParents.split(",").length + " parents"), button -> {
            this.minecraft.setScreen(new ParentSelectionScreen(this, this.questId != null ? this.questId : "", this.category, this.selectedParents, this.titleField.getValue(), this.gridX, this.gridY, this.selectedIcon));
        }).bounds(col2, y, fieldWidth, fieldHeight).build();
        this.addRenderableWidget(this.parentsButton);

        // ROW 2: Task Type & Reward Type Toggles
        y += yStep - 8;
        this.taskTypeButton = Button.builder(
                Component.translatable("form.pacpackquests.task_type")
                        .append(Component.literal(" *").withStyle(ChatFormatting.RED))
                        .append(Component.literal(": "))
                        .append(Component.translatable("task.pacpackquests." + currentTaskType.name().toLowerCase())),
                button -> {
                    this.isTaskDropdownOpen = !this.isTaskDropdownOpen;
                    if (this.isTaskDropdownOpen) {
                        this.isRewardDropdownOpen = false;
                    }
                    this.dropdownScrollbar.resetScroll();
                }
        ).bounds(col1, y, fieldWidth, fieldHeight).build();
        this.addRenderableWidget(this.taskTypeButton);

        this.rewardTypeButton = Button.builder(
                Component.translatable("form.pacpackquests.reward_type")
                        .append(Component.literal(" *").withStyle(ChatFormatting.RED))
                        .append(Component.literal(": "))
                        .append(Component.translatable("reward.pacpackquests." + currentRewardType.name().toLowerCase())),
                button -> {
                    this.isRewardDropdownOpen = !this.isRewardDropdownOpen;
                    if (this.isRewardDropdownOpen) {
                        this.isTaskDropdownOpen = false;
                    }
                    this.dropdownScrollbar.resetScroll();
                }
        ).bounds(col2, y, fieldWidth, fieldHeight).build();
        this.addRenderableWidget(this.rewardTypeButton);

        // ROW 3: Target Button & Reward Button
        y += yStep;
        this.targetButton = Button.builder(Component.literal(formatDisplayString(getTranslatedName(selectedTarget, getTargetRegistryType()))), button -> {
            RegistryType ctx = getTargetRegistryType();
            this.minecraft.setScreen(new SelectionScreen(this, ctx, result -> {
                this.selectedTarget = result;
                this.targetButton.setMessage(Component.literal(formatDisplayString(getTranslatedName(result, ctx))));
                if (!this.isIconManuallyEdited) {
                    this.selectedIcon = ItemUtils.toCommandString(IconUtils.getRepresentativeItem(result, TaskType.getRegistryType(currentTaskType)));
                    this.iconButton.setMessage(Component.literal(formatDisplayString(getTranslatedName(this.selectedIcon, RegistryType.ITEM))));
                }
            }, this.selectedTarget));
        }).bounds(col1, y, fieldWidth, fieldHeight).build();
        this.addRenderableWidget(this.targetButton);

        this.rewardButton = Button.builder(Component.literal(""), button -> {
            if (currentRewardType == RewardType.COMMAND) {
                this.minecraft.setScreen(new EditCommandScreen(this, this.selectedReward, this.selectedCommandDescription, (cmd, desc) -> {
                    this.selectedReward = cmd;
                    this.selectedCommandDescription = desc;
                    updateRewardButtonMessage();
                }));
            } else {
                this.minecraft.setScreen(new SelectionScreen(this, RegistryType.ITEM, result -> {
                    this.selectedReward = result;
                    updateRewardButtonMessage();
                }, this.selectedReward, true, true));
            }
        }).bounds(col2, y, fieldWidth, fieldHeight).build();
        this.rewardButton.active = (currentRewardType != RewardType.XP && currentRewardType != RewardType.LEVEL);
        updateRewardButtonMessage();
        this.addRenderableWidget(this.rewardButton);

        // ROW 4: Amounts
        y += yStep;
        this.reqAmountField = new EditBox(this.font, col1, y, fieldWidth, fieldHeight, Component.translatable("form.pacpackquests.required_amount"));
        this.reqAmountField.setMaxLength(7);
        this.reqAmountField.setFilter(text -> text.matches("^[0-9]*$"));
        this.reqAmountField.setValue(initialReqAmt);
        this.reqAmountField.setEditable(this.currentTaskType != TaskType.EXPLORE_DIMENSION && this.currentTaskType != TaskType.EXPLORE_BIOME);
        this.reqAmountField.active = this.currentTaskType != TaskType.EXPLORE_DIMENSION && this.currentTaskType != TaskType.EXPLORE_BIOME;
        this.addRenderableWidget(this.reqAmountField);

        this.rewardAmountField = new EditBox(this.font, col2, y, fieldWidth, fieldHeight, Component.translatable("form.pacpackquests.reward_amount"));
        this.rewardAmountField.setMaxLength(7);
        this.rewardAmountField.setFilter(text -> text.matches("^[0-9]*$"));
        this.rewardAmountField.setValue(initialRewAmt);
        this.addRenderableWidget(this.rewardAmountField);

        // ROW 5: ID & Icon Button
        y += yStep;
        this.idField = new EditBox(this.font, col1, y, fieldWidth, fieldHeight, Component.translatable("form.pacpackquests.quest_id")) {
            @Override
            public void insertText(String text) {
                super.insertText(PathSecurity.formatStringToValidPath(text));
            }
        };
        this.idField.setMaxLength(64);
        if (questId != null) {
            this.idField.setValue(questId);
            this.idField.setEditable(false);
            this.idField.active = false;
        } else {
            // Restore the ID typed during the creation of a new quest or the duplication of another one
            this.idField.setValue(currentId);
            this.idField.setHint(Component.translatable("form.pacpackquests.quest_id_placeholder").withStyle(ChatFormatting.DARK_GRAY));
            this.idField.setResponder(newId -> {
                if (!this.isIdManuallyEdited && this.idField.isFocused() && !newId.equals(PathSecurity.formatStringToValidPath(this.titleField.getValue()))) {
                    this.isIdManuallyEdited = true;
                }
            });

            // Add a listener to dynamically change the ID according to the title
            this.titleField.setResponder(newTitle -> {
                if (!isIdManuallyEdited) this.idField.setValue(PathSecurity.formatStringToValidPath(newTitle));
            });
        }
        this.addRenderableWidget(this.idField);

        this.iconButton = Button.builder(Component.literal(formatDisplayString(getTranslatedName(this.selectedIcon, RegistryType.ITEM))), button -> {
            this.minecraft.setScreen(new SelectionScreen(this, RegistryType.ITEM, result -> {
                this.selectedIcon = result;
                this.iconButton.setMessage(Component.literal(formatDisplayString(getTranslatedName(result, RegistryType.ITEM))));
                this.isIconManuallyEdited = true;
            }, this.selectedIcon, false));
        }).bounds(col2, y, fieldWidth, fieldHeight).build();
        this.addRenderableWidget(this.iconButton);

        // --- BOTTOM ACTION BUTTONS ---
        y += yStep + 8;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.pacpackquests.cancel"), button -> this.minecraft.setScreen(parent))
                .bounds(this.width / 2 - 105, y, 60, fieldHeight).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.pacpackquests.save").withStyle(ChatFormatting.GREEN), button -> saveQuest())
                .bounds(this.width / 2 - 35, y, 80, fieldHeight).build());

        if (questId != null) {
            this.addRenderableWidget(Button.builder(Component.translatable("gui.pacpackquests.delete").withStyle(ChatFormatting.RED), button -> {
                Services.NETWORK.sendToServer(new DeleteQuestPayload(questId));
                this.minecraft.setScreen(parent);
            }).bounds(this.width / 2 + 55, y, 60, fieldHeight).build());
        }

        this.duplicatedQuest = null;
    }

    private RegistryType getTargetRegistryType() {
        return switch (currentTaskType) {
            case MINE_BLOCK -> RegistryType.BLOCK;
            case CRAFT_ITEM, OBTAIN_ITEM -> RegistryType.ITEM;
            case KILL_MOB -> RegistryType.MOB;
            case EXPLORE_BIOME -> RegistryType.BIOME;
            case EXPLORE_STRUCTURE -> RegistryType.STRUCTURE;
            case EXPLORE_DIMENSION -> RegistryType.DIMENSION;
            case OPEN_LOOT_CHEST -> RegistryType.LOOT_TABLE;
        };
    }

    private void updateRewardButtonMessage() {
        if (this.rewardButton == null) return;
        RegistryType ctx = switch (currentRewardType) {
            case ITEM -> RegistryType.ITEM;
            case COMMAND -> RegistryType.COMMAND;
            case XP, LEVEL -> null;
        };
        this.rewardButton.setMessage(Component.literal(formatDisplayString(getTranslatedRewardName(selectedReward, ctx))));
    }

    // Validity checks are mostly handled by the selection screen now, but we keep this as a safeguard
    private boolean isValidTarget(String target, TaskType type) {
        if (target.startsWith("#")) {
            Identifier id = Identifier.tryParse(target.substring(1));
            if (id == null) return false;
            return switch (type) {
                case MINE_BLOCK -> BuiltInRegistries.BLOCK.get(TagKey.create(Registries.BLOCK, id)).isPresent();
                case CRAFT_ITEM, OBTAIN_ITEM -> BuiltInRegistries.ITEM.get(TagKey.create(Registries.ITEM, id)).isPresent();
                case KILL_MOB -> BuiltInRegistries.ENTITY_TYPE.get(TagKey.create(Registries.ENTITY_TYPE, id)).isPresent();
                case EXPLORE_BIOME -> {
                    var regOpt = Minecraft.getInstance().level.registryAccess().lookup(Registries.BIOME);
                    yield regOpt.isPresent() && regOpt.get().get(TagKey.create(Registries.BIOME, id)).isPresent();
                }
                case EXPLORE_STRUCTURE -> true;
                case EXPLORE_DIMENSION -> false;
                case OPEN_LOOT_CHEST -> target.equals("#pacpackquests:loot_chest");
            };
        }

        if (target.contains("[")) {
            String baseId = target.substring(0, target.indexOf('['));
            Identifier id = Identifier.tryParse(baseId);
            return id != null && (type == TaskType.CRAFT_ITEM || type == TaskType.OBTAIN_ITEM) && BuiltInRegistries.ITEM.containsKey(id);
        }

        Identifier id = Identifier.tryParse(target);
        if (id == null) return false;

        return switch (type) {
            case MINE_BLOCK -> BuiltInRegistries.BLOCK.containsKey(id);
            case CRAFT_ITEM, OBTAIN_ITEM -> BuiltInRegistries.ITEM.containsKey(id);
            case KILL_MOB -> BuiltInRegistries.ENTITY_TYPE.containsKey(id);
            case EXPLORE_BIOME -> {
                var regOpt = Minecraft.getInstance().level.registryAccess().lookup(Registries.BIOME);
                yield regOpt.isPresent() && regOpt.get().get(ResourceKey.create(Registries.BIOME, id)).isPresent();
            }
            case EXPLORE_STRUCTURE -> true;
            case EXPLORE_DIMENSION -> {
                var handler = Minecraft.getInstance().getConnection();
                yield handler != null && handler.levels().stream().anyMatch(key -> key.identifier().equals(id));
            }
            case OPEN_LOOT_CHEST -> {
                var server = Minecraft.getInstance().getSingleplayerServer();
                if (server == null) yield true;
                var opt = server.reloadableRegistries().lookup().lookup(Registries.LOOT_TABLE);
                if (opt.isPresent()) {
                    var key = ResourceKey.create(Registries.LOOT_TABLE, id);
                    yield opt.get().get(key).isPresent();
                }
                yield false;
            }
        };
    }

    private void saveQuest() {
        this.errorMessage = null;
        this.idField.setTextColor(0xFFE0E0E0);
        this.titleField.setTextColor(0xFFE0E0E0);
        this.reqAmountField.setTextColor(0xFFE0E0E0);
        this.rewardAmountField.setTextColor(0xFFE0E0E0);

        String finalId = this.idField.getValue().trim();
        if (!PathSecurity.isValidIdentifier(finalId)) {
            this.errorMessage = "error.pacpackquests.invalid_id";
            this.idField.setTextColor(0xFFFF5555);
            return;
        }

        if (this.questId == null && ClientQuestData.CLIENT_DEFINITIONS.containsKey(finalId)) {
            this.errorMessage = "error.pacpackquests.id_already_exists";
            this.idField.setTextColor(0xFFFF5555);
            return;
        }

        if (this.titleField.getValue().trim().isEmpty()) {
            this.errorMessage = "error.pacpackquests.missing_title";
            this.titleField.setTextColor(0xFFFF5555);
            return;
        }

        String icon = this.selectedIcon.indexOf('[') >= 0 ? this.selectedIcon.substring(0, this.selectedIcon.indexOf('[')) : this.selectedIcon;
        if (this.selectedIcon.isEmpty() || Identifier.tryParse(icon) == null) {
            this.errorMessage = "error.pacpackquests.invalid_icon";
            return;
        }

        if (this.selectedTarget.isEmpty() || !isValidTarget(this.selectedTarget, currentTaskType)) {
            this.errorMessage = "error.pacpackquests.invalid_target";
            return;
        }

        int reqAmt;
        try {
            reqAmt = Integer.parseInt(this.reqAmountField.getValue().trim());
            if (reqAmt <= 0) throw new NumberFormatException();
            if (currentTaskType == TaskType.OBTAIN_ITEM && reqAmt > 2304) {
                this.errorMessage = "error.pacpackquests.invalid_object_amount";
                this.reqAmountField.setTextColor(0xFFFF5555);
                return;
            }
        } catch (NumberFormatException e) {
            this.errorMessage = "error.pacpackquests.invalid_amount";
            this.reqAmountField.setTextColor(0xFFFF5555);
            return;
        }

        if (this.currentRewardType == RewardType.ITEM && (this.selectedReward.isEmpty() || !isValidTarget(this.selectedReward, TaskType.OBTAIN_ITEM))) {
            this.errorMessage = "error.pacpackquests.invalid_reward_item";
            return;
        }

        String finalRewardStr = (currentRewardType != RewardType.XP && currentRewardType != RewardType.LEVEL) ? this.selectedReward : "minecraft:air";

        int rewAmt;
        try {
            rewAmt = Integer.parseInt(this.rewardAmountField.getValue().trim());
            if (rewAmt <= 0) throw new NumberFormatException();
            if (currentRewardType == RewardType.ITEM && rewAmt > 2304) {
                this.errorMessage = "error.pacpackquests.invalid_object_amount";
                this.rewardAmountField.setTextColor(0xFFFF5555);
                return;
            } else if (currentRewardType == RewardType.COMMAND && rewAmt > 256) {
                this.errorMessage = "error.pacpackquests.invalid_command_amount";
                this.rewardAmountField.setTextColor(0xFFFF5555);
                return;
            }
        } catch (NumberFormatException e) {
            this.errorMessage = "error.pacpackquests.invalid_amount";
            this.rewardAmountField.setTextColor(0xFFFF5555);
            return;
        }

        ArrayList<String> parentsList = new ArrayList<>();
        String parentsStr = String.join(",", this.selectedParents);
        if (!parentsStr.isEmpty()) {
            for (String p : parentsStr.split(",")) {
                String parentId = p.trim();
                if (parentId.equals(finalId)) {
                    this.errorMessage = "error.pacpackquests.self_parent";
                    return;
                }
                if (!ClientQuestData.CLIENT_DEFINITIONS.containsKey(parentId)) {
                    this.errorMessage = "error.pacpackquests.unknown_parent";
                    return;
                }
                parentsList.add(parentId);
            }
        }

        java.util.Map<String, String> properties = new java.util.HashMap<>();
        QuestDefinition existingDef = this.questId != null ? ClientQuestData.CLIENT_DEFINITIONS.get(this.questId) : this.duplicatedQuest;
        if (existingDef != null && existingDef.properties() != null) {
            properties.putAll(existingDef.properties());
        }

        SaveQuestPayload payload = new SaveQuestPayload(
                finalId, this.titleField.getValue().trim(), this.category,
                this.selectedIcon, this.gridX, this.gridY, parentsList,
                properties,
                List.of(new QuestTask(this.currentTaskType, this.selectedTarget, reqAmt)),
                List.of(QuestDefinition.createReward(this.currentRewardType, finalRewardStr, rewAmt, this.selectedCommandDescription))
        );

        Services.NETWORK.sendToServer(payload);
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if ((this.isTaskDropdownOpen || this.isRewardDropdownOpen) && dropdownScrollbar.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) return true;
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent click, double offsetX, double offsetY) {
        if ((this.isTaskDropdownOpen || this.isRewardDropdownOpen) && dropdownScrollbar.mouseDragged(click.x(), click.y(), click.button(), offsetX, offsetY)) return true;
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click) {
        if ((this.isTaskDropdownOpen || this.isRewardDropdownOpen) && dropdownScrollbar.mouseReleased(click.x(), click.y(), click.button())) return true;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubled) {
        if (this.isTaskDropdownOpen && this.taskTypeButton != null) {
            if (handleDropdownClick(click, this.taskTypeButton, TaskType.values(), this::selectTaskType, () -> {
                this.isTaskDropdownOpen = false;
                this.dropdownScrollbar.resetScroll();
            })) {
                return true;
            }
        }

        if (this.isRewardDropdownOpen && this.rewardTypeButton != null) {
            if (handleDropdownClick(click, this.rewardTypeButton, RewardType.values(), this::selectRewardType, () -> {
                this.isRewardDropdownOpen = false;
                this.dropdownScrollbar.resetScroll();
            })) {
                return true;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean keyPressed(KeyEvent input) {
        if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            this.minecraft.setScreen(parent);
            return true;
        }
        return super.keyPressed(input);
    }

    private <E extends Enum<E>> boolean handleDropdownClick(MouseButtonEvent click, Button button, E[] values, Consumer<E> onSelect, Runnable closeDropdown) {
        int dropX = button.getX();
        int dropY = button.getY() + button.getHeight();
        int dropWidth = button.getWidth();

        int numOptions = values.length;
        int visibleOptions = Math.min(numOptions, MAX_DROPDOWN_VISIBLE);
        int dropHeight = 20 * visibleOptions;

        if (this.dropdownScrollbar.mouseClicked(click.x(), click.y(), click.button())) {
            return true;
        }

        if (click.x() >= dropX && click.x() < dropX + dropWidth && click.y() >= dropY && click.y() < dropY + dropHeight) {
            int taskDropdownScroll = this.dropdownScrollbar.getStepOffset();
            int index = (int) ((taskDropdownScroll + click.y() - dropY) / 20);
            if (index >= 0 && index < numOptions) {
                onSelect.accept(values[index]);
                closeDropdown.run();
                return true;
            }
        } else if (!button.isMouseOver(click.x(), click.y())) {
            closeDropdown.run();
        }

        return false;
    }

    private void selectTaskType(TaskType type) {
        this.currentTaskType = type;
        this.taskTypeButton.setMessage(Component.translatable("form.pacpackquests.task_type")
                .append(Component.literal(" *").withStyle(ChatFormatting.RED))
                .append(Component.literal(": "))
                .append(Component.translatable("task.pacpackquests." + currentTaskType.name().toLowerCase())));
        this.reqAmountField.setEditable(type != TaskType.EXPLORE_DIMENSION && type != TaskType.EXPLORE_BIOME);
        this.reqAmountField.active = this.currentTaskType != TaskType.EXPLORE_DIMENSION && this.currentTaskType != TaskType.EXPLORE_BIOME;

        if (type == TaskType.EXPLORE_DIMENSION || type == TaskType.EXPLORE_BIOME) {
            this.reqAmountField.setValue("1");
        }

        this.selectedTarget = switch (type) {
            case MINE_BLOCK -> "minecraft:stone";
            case KILL_MOB -> "minecraft:zombie";
            case CRAFT_ITEM, OBTAIN_ITEM -> "minecraft:iron_pickaxe";
            case EXPLORE_BIOME -> "minecraft:desert";
            case EXPLORE_STRUCTURE -> "minecraft:ruined_portal";
            case EXPLORE_DIMENSION -> "minecraft:the_nether";
            case OPEN_LOOT_CHEST -> "#pacpackquests:loot_chest";
        };

        if (!this.isIconManuallyEdited) {
            this.selectedIcon = ItemUtils.toCommandString(IconUtils.getRepresentativeItem(this.selectedTarget, TaskType.getRegistryType(currentTaskType)));
            this.iconButton.setMessage(Component.literal(formatDisplayString(getTranslatedName(this.selectedIcon, RegistryType.ITEM))));
        }

        this.targetButton.setMessage(Component.literal(formatDisplayString(getTranslatedName(this.selectedTarget, getTargetRegistryType()))));
    }

    private void selectRewardType(RewardType type) {
        this.currentRewardType = type;
        this.rewardTypeButton.setMessage(Component.translatable("form.pacpackquests.reward_type")
                .append(Component.literal(" *").withStyle(ChatFormatting.RED))
                .append(Component.literal(": "))
                .append(Component.translatable("reward.pacpackquests." + currentRewardType.name().toLowerCase())));
        this.rewardButton.active = (type != RewardType.XP && type != RewardType.LEVEL);

        this.selectedReward = switch (type) {
            case ITEM -> "minecraft:diamond";
            case COMMAND -> "";
            default -> this.selectedReward;
        };

        updateRewardButtonMessage();
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);

        context.drawString(this.font, Component.translatable("form.pacpackquests.title").append(Component.literal(" *").withStyle(ChatFormatting.RED)), this.titleField.getX(), this.titleField.getY() - 10, 0xFFFFFFFF, true);
        context.drawString(this.font, Component.translatable("form.pacpackquests.quest_id"), this.idField.getX(), this.idField.getY() - 10, 0xFFFFFFFF, true);
        context.drawString(this.font, Component.translatable("form.pacpackquests.icon"), this.iconButton.getX(), this.iconButton.getY() - 10, 0xFFFFFFFF, true);
        context.drawString(this.font, Component.translatable("form.pacpackquests.task_target").append(Component.literal(" *").withStyle(ChatFormatting.RED)), this.targetButton.getX(), this.targetButton.getY() - 10, 0xFFFFFFFF, true);

        if (this.currentRewardType == RewardType.ITEM)
            context.drawString(this.font, Component.translatable("form.pacpackquests.reward_item").append(Component.literal(" *").withStyle(ChatFormatting.RED)), this.rewardButton.getX(), this.rewardButton.getY() - 10, 0xFFFFFFFF, true);
        else if (this.currentRewardType == RewardType.COMMAND)
            context.drawString(this.font, Component.translatable("form.pacpackquests.reward_command").append(Component.literal(" *").withStyle(ChatFormatting.RED)), this.rewardButton.getX(), this.rewardButton.getY() - 10, 0xFFFFFFFF, true);

        context.drawString(this.font, Component.translatable("form.pacpackquests.required_amount").append(Component.literal(" *").withStyle(ChatFormatting.RED)), this.reqAmountField.getX(), this.reqAmountField.getY() - 10, 0xFFFFFFFF, true);
        context.drawString(this.font, Component.translatable("form.pacpackquests.reward_amount").append(Component.literal(" *").withStyle(ChatFormatting.RED)), this.rewardAmountField.getX(), this.rewardAmountField.getY() - 10, 0xFFFFFFFF, true);
        context.drawString(this.font, Component.translatable("form.pacpackquests.parents"), this.parentsButton.getX(), this.parentsButton.getY() - 10, 0xFFFFFFFF, true);

        if (this.errorMessage != null) {
            context.drawCenteredString(this.font, Component.translatable(this.errorMessage).withStyle(ChatFormatting.RED, ChatFormatting.BOLD), this.width / 2, this.height - 52, 0xFFFFFFFF);
        }

        // Draw TaskType dropdown menu on top
        if (this.isTaskDropdownOpen && this.taskTypeButton != null) {
            drawDropdownMenu(context, mouseX, mouseY, this.taskTypeButton, TaskType.values(), "task");
        }

        if (this.isRewardDropdownOpen && this.rewardTypeButton != null) {
            drawDropdownMenu(context, mouseX, mouseY, this.rewardTypeButton, RewardType.values(), "reward");
        }
    }

    private <E extends Enum<E>> void drawDropdownMenu(GuiGraphics context, int mouseX, int mouseY, Button button, E[] values, String type) {
        int dropX = button.getX();
        int dropY = button.getY() + button.getHeight();
        int dropWidth = button.getWidth();
        int numOptions = values.length;

        int visibleOptions = Math.min(numOptions, MAX_DROPDOWN_VISIBLE);
        int dropHeight = 20 * visibleOptions;
        int maxScroll = Math.max(0, (numOptions * 20) - dropHeight);

        // overall background
        context.fill(dropX, dropY, dropX + dropWidth, dropY + dropHeight, 0xFF000000);

        // outline
        int totalWidth = maxScroll > 0 ? dropWidth + 6 : dropWidth;
        context.fill(dropX - 1, dropY - 1, dropX + totalWidth + 1, dropY, 0xFFAAAAAA);
        context.fill(dropX - 1, dropY + dropHeight, dropX + totalWidth + 1, dropY + dropHeight + 1, 0xFFAAAAAA);
        context.fill(dropX - 1, dropY, dropX, dropY + dropHeight, 0xFFAAAAAA);
        context.fill(dropX + totalWidth, dropY, dropX + totalWidth + 1, dropY + dropHeight, 0xFFAAAAAA);

        boolean mouseInDropdown = mouseX >= dropX && mouseX <= dropX + totalWidth && mouseY >= dropY && mouseY <= dropY + dropHeight;
        if (mouseInDropdown) {
            context.requestCursor(CursorTypes.ARROW);
        }

        context.enableScissor(dropX, dropY, dropX + dropWidth, dropY + dropHeight);
        context.pose().translate(0.0f, (float) -this.dropdownScrollbar.getScrollAmount());

        for (int i = 0; i < numOptions; i++) {
            int optY = dropY + (i * 20);
            boolean hovered = mouseX >= dropX && mouseX < dropX + dropWidth && mouseY >= dropY && mouseY < dropY + dropHeight &&
                    (mouseY + this.dropdownScrollbar.getScrollAmount()) >= optY && (mouseY + this.dropdownScrollbar.getScrollAmount()) < optY + 20;

            if (hovered) {
                context.fill(dropX, optY, dropX + dropWidth, optY + 20, 0xFF666666);
                context.requestCursor(CursorTypes.POINTING_HAND);
            }
            context.drawCenteredString(this.font, Component.translatable(type + ".pacpackquests." + values[i].name().toLowerCase()), dropX + dropWidth / 2, optY + 6, 0xFFFFFFFF);
        }

        context.pose().translate(0.0f, (float) this.dropdownScrollbar.getScrollAmount());
        context.disableScissor();

        this.dropdownScrollbar.setBounds(dropX + dropWidth, dropY, 6, dropHeight);
        this.dropdownScrollbar.setMaxScroll(maxScroll, dropHeight);
        // Draw scrollbar if needed
        if (maxScroll > 0) {
            this.dropdownScrollbar.render(context, mouseX, mouseY);
        }
    }

    public void updateParents(List<String> parents) {
        this.selectedParents = new ArrayList<>(parents);
    }
}
