package com.atomiclegacy.client;

import com.atomiclegacy.client.qmaw.QmawLoader;
import com.atomiclegacy.client.qmaw.QmawScreen;
import com.atomiclegacy.client.qmaw.QuickManualAndWiki;
import com.atomiclegacy.util.Calculator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * [parity S3: calculator] GUIScreenCalculator: calculator / QMAW search overlay opened with the calculator key.
 * Key semantics of keyTyped(char, keyCode) are rebuilt from GLFW key and char events.
 */
public final class CalculatorScreen extends Screen {
    private static final int X_SIZE = 220;
    private static final int Y_SIZE = 50;
    private static final int BORDER_WIDTH = 2;
    private static final int MAX_HISTORY = 6;
    private static final List<Map.Entry<String, Double>> CALC_HISTORY = new ArrayList<>(MAX_HISTORY);
    private static final List<String> QMAW_HISTORY = new ArrayList<>(MAX_HISTORY);
    private static final int BORDER_COLOR = 0xFF2d2d2d, FILL_COLOR = 0xFF3d3d3d, SELECT_COLOR = 0xFF111111;
    private static final int QMAW_ENTRY_Y_SPACING = 7;
    private static final int MOUSE_LEFT = 0, MOUSE_MIDDLE = 2;
    // Can in theory add more modes; the original kept the mode across openings.
    private static boolean qmawMode = false;

    private final List<String> qmawResults = new ArrayList<>();
    private EditBox inputField;
    private int selectedHist = -1;
    private String latestResult = "?", lastInput = "";

    public CalculatorScreen() {
        super(Component.empty());
    }

    @Override
    protected void init() {
        int x = startX();
        int y = startY();
        String previous = inputField == null ? "" : inputField.getValue();
        inputField = new EditBox(font, x + 5, y + 14, 210, 13, Component.empty());
        inputField.setTextColor(-1);
        inputField.setCanLoseFocus(false);
        inputField.setFocused(true);
        inputField.setMaxLength(1000);
        inputField.setValue(previous);
        addRenderableWidget(inputField);
        setInitialFocus(inputField);
    }

    private int startX() {
        return (width - X_SIZE) / 2;
    }

    private int startY() {
        return (height - (Y_SIZE + (font.lineHeight + 4) * MAX_HISTORY)) / 2;
    }

    private boolean isInputEmpty() {
        return inputField.getValue().isEmpty();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean handled = inputField.keyPressed(keyCode, scanCode, modifiers);
        if (!handled && keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        boolean enter = keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER;
        if (keyCode == GLFW.GLFW_KEY_TAB) qmawMode = !qmawMode;
        typed(enter, keyCode == GLFW.GLFW_KEY_DELETE, keyCode == GLFW.GLFW_KEY_UP, keyCode == GLFW.GLFW_KEY_DOWN);
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        inputField.charTyped(codePoint, modifiers);
        typed(false, false, false, false);
        return true;
    }

    private void typed(boolean enter, boolean delete, boolean up, boolean down) {
        if (qmawMode) {
            trySearchForQMAW(enter, delete, up, down);
        } else {
            handleCalculator(enter, delete, up, down);
        }
    }

    private void handleCalculator(boolean enter, boolean delete, boolean up, boolean down) {
        String input = inputField.getValue().replaceAll("[^\\d+\\-*/%^!.()\\sA-Za-z]+", "");
        selectedHistSanityCheck();
        if (enter) {
            if (selectedHist != -1) {
                input = CALC_HISTORY.get(selectedHist).getKey();
                inputField.setValue(input);
                selectedHist = -1;
            } else {
                try {
                    double result = Calculator.evaluateExpression(input);
                    CALC_HISTORY.add(0, new AbstractMap.SimpleEntry<>(input, result));
                    if (CALC_HISTORY.size() > MAX_HISTORY) CALC_HISTORY.remove(CALC_HISTORY.size() - 1);
                    String plainStringRepresentation = (new BigDecimal(result, MathContext.DECIMAL64)).toPlainString();
                    Minecraft.getInstance().keyboardHandler.setClipboard(plainStringRepresentation);
                    inputField.setValue(plainStringRepresentation);
                    inputField.moveCursorToEnd(false);
                    inputField.setHighlightPos(0);
                } catch (Exception ignored) {
                }
                return;
            }
        }
        if (delete && input.isEmpty()) deleteCalcHistoryEntry();
        if (up) selectedHist = (selectedHist < 0) ? (Math.min(CALC_HISTORY.size(), MAX_HISTORY) - 1) : Math.max(selectedHist - 1, -1);
        else if (down) selectedHist = (selectedHist >= (CALC_HISTORY.size() - 1)) ? -1 : Math.min(selectedHist + 1, CALC_HISTORY.size() - 1);
        else selectedHist = -1;

        if (input.isEmpty()) {
            latestResult = "?";
            return;
        }
        try {
            latestResult = Double.toString(Calculator.evaluateExpression(input));
        } catch (Exception e) {
            latestResult = e.toString();
        }
    }

    private void deleteCalcHistoryEntry() {
        selectedHistSanityCheck();
        if (selectedHist >= 0) CALC_HISTORY.remove(selectedHist);
    }

    private static String language() {
        String selected = Minecraft.getInstance().getLanguageManager().getSelected();
        int underscore = selected.indexOf('_');
        return underscore < 0 || underscore + 1 >= selected.length()
                ? selected : selected.substring(0, underscore).toLowerCase(Locale.ROOT) + "_"
                + selected.substring(underscore + 1).toUpperCase(Locale.ROOT);
    }

    private static QuickManualAndWiki page(String name) {
        return QmawLoader.page(name).orElse(null);
    }

    private static void sortQMAWResults(String search, List<String> results) {
        String lang = language();
        // Prioritize entries that start with the query
        results.sort((s1, s2) -> {
            String title1 = page(s1).title(lang), title2 = page(s2).title(lang);
            boolean starts1 = title1.toLowerCase(Locale.ROOT).startsWith(search), starts2 = title2.toLowerCase(Locale.ROOT).startsWith(search);
            return starts1 && !starts2 ? -1 : !starts1 && starts2 ? 1 : title1.compareTo(title2);
        });
    }

    private void trySearchForQMAW(boolean enter, boolean delete, boolean up, boolean down) {
        boolean history = isInputEmpty();
        String query = inputField.getValue().toLowerCase(Locale.ROOT);
        if (!inputField.getValue().equalsIgnoreCase(lastInput)) {
            qmawResults.clear();
            lastInput = inputField.getValue();
            String lang = language();
            for (QuickManualAndWiki qmaw : QmawLoader.indexedPages()) {
                if (qmaw.title(lang).toLowerCase(Locale.ROOT).contains(query)) qmawResults.add(qmaw.name());
            }
        }
        sortQMAWResults(query, qmawResults);

        if (enter) executeQMAW(history);
        if (delete && history) deleteQMAWHistoryEntry();
        if (up) selectedHist = (selectedHist < 0) ? ((history ? QMAW_HISTORY.size() : Math.min(qmawResults.size(), MAX_HISTORY)) - 1) : Math.max(selectedHist - 1, -1);
        else if (down) selectedHist = (selectedHist >= ((history ? QMAW_HISTORY.size() : qmawResults.size()) - 1)) ? -1 : ((selectedHist >= (MAX_HISTORY - 1)) ? -1 : (selectedHist + 1));
        else selectedHist = -1;
    }

    private void executeQMAW(boolean historyMode) {
        selectedHistSanityCheck();
        if (selectedHist >= 0) {
            final String key = new ArrayList<>(historyMode ? QMAW_HISTORY : qmawResults).get(selectedHist);
            final QuickManualAndWiki qmaw = page(key);
            if (qmaw == null) return;
            QMAW_HISTORY.remove(key); // Remove duplicate history
            QMAW_HISTORY.add(0, key);
            if (QMAW_HISTORY.size() > MAX_HISTORY) QMAW_HISTORY.remove(QMAW_HISTORY.size() - 1);
            inputField.setValue(qmaw.title(language()));
            selectedHist = -1;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            Minecraft.getInstance().setScreen(new QmawScreen(qmaw));
        }
    }

    private void deleteQMAWHistoryEntry() {
        selectedHistSanityCheck();
        if (selectedHist >= 0) QMAW_HISTORY.remove(selectedHist);
    }

    /** GuiScreen#drawScreen draws no default background. */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int x = startX();
        int y = startY();
        int histHeight = (font.lineHeight + 4) * MAX_HISTORY;
        int histStart = y + 30 + font.lineHeight + 8;

        graphics.fill(x, y, x + X_SIZE, y + Y_SIZE + histHeight, BORDER_COLOR);
        graphics.fill(x + BORDER_WIDTH, y + BORDER_WIDTH, x + X_SIZE - BORDER_WIDTH, y + Y_SIZE - BORDER_WIDTH + histHeight, FILL_COLOR);
        if (!qmawMode) graphics.fill(x, histStart - 5, x + X_SIZE, histStart - 3, BORDER_COLOR);

        super.render(graphics, mouseX, mouseY, partialTick); // input field
        final String headerText = I18n.get(qmawMode ? "gui.calc.qmawResult" : "gui.calc.calculatorResult");
        graphics.drawString(font, headerText, x + 4, y + 4, -1, false);

        if (qmawMode) drawQMAW(graphics, mouseX, mouseY, x, y);
        else drawCalc(graphics, x, y);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        long window = Minecraft.getInstance().getWindow().getWindow();
        if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_MIDDLE) == GLFW.GLFW_PRESS) {
            return false;
        }
        // Scrolling up behaves the same as up arrow, and vice versa
        if (scrollY > 0) {
            if (qmawMode)
                selectedHist = (selectedHist < 0) ? ((isInputEmpty() ? QMAW_HISTORY.size() : Math.min(qmawResults.size(), MAX_HISTORY)) - 1) : Math.max(selectedHist - 1, -1);
            else
                selectedHist = (selectedHist < 0) ? (Math.min(CALC_HISTORY.size(), MAX_HISTORY) - 1) : Math.max(selectedHist - 1, -1);
        } else if (scrollY < 0) {
            if (qmawMode)
                selectedHist = (selectedHist >= ((isInputEmpty() ? QMAW_HISTORY.size() : qmawResults.size()) - 1)) ? -1 : ((selectedHist >= (MAX_HISTORY - 1)) ? -1 : (selectedHist + 1));
            else
                selectedHist = (selectedHist >= (CALC_HISTORY.size() - 1)) ? -1 : Math.min(selectedHist + 1, CALC_HISTORY.size() - 1);
        }
        return true;
    }

    private void drawCalc(GuiGraphics graphics, int x, int y) {
        graphics.drawString(font, "=" + latestResult, x + 5, y + 32, -1, false);
        int i = 0;
        for (Map.Entry<String, Double> prevInput : CALC_HISTORY) {
            int hy = y + 50 + (font.lineHeight + 1) * i;
            if (i == selectedHist) graphics.fill(x + 4, hy - 1, x + 4 + X_SIZE - 9, hy + font.lineHeight, SELECT_COLOR);
            graphics.drawString(font, prevInput.getKey() + " = " + prevInput.getValue(), x + 5, hy, -1, false);
            i++;
        }
    }

    private void drawQMAW(GuiGraphics graphics, int mouseX, int mouseY, int x, int y) {
        String lang = language();
        int i = 0;
        for (String r : isInputEmpty() ? QMAW_HISTORY : qmawResults) {
            if (i > MAX_HISTORY - 1) break;
            QuickManualAndWiki qmaw = page(r);
            if (qmaw == null) continue;
            int hy = y + 35 + (font.lineHeight + QMAW_ENTRY_Y_SPACING) * i;
            if (i == selectedHist) graphics.fill(x + 4, hy - QMAW_ENTRY_Y_SPACING / 2, x + 4 + X_SIZE - 9, hy + font.lineHeight + QMAW_ENTRY_Y_SPACING / 2, SELECT_COLOR);
            int hover = mouseEntrySelected(mouseX, mouseY);
            graphics.drawString(font, qmaw.title(lang), x + 25, hy, hover == i ? 0xffff00 : -1, false);
            ItemStack icon = qmaw.icon();
            if (!icon.isEmpty()) {
                graphics.renderItem(icon, x + 5, hy - QMAW_ENTRY_Y_SPACING + 2);
                graphics.renderItemDecorations(font, icon, x + 5, hy - QMAW_ENTRY_Y_SPACING + 2);
            }
            i++;
        }
    }

    private boolean isMouseInSegment(double mouseX, double mouseY) {
        final int x = startX(), y = startY();
        return (mouseX > x && mouseX < (x + X_SIZE)) && (mouseY > y && mouseY < (y + Y_SIZE + (font.lineHeight + 4) * MAX_HISTORY));
    }

    private int mouseEntrySelected(double mouseX, double mouseY) {
        int entry = -1;
        if (isMouseInSegment(mouseX, mouseY)) {
            final int x = startX(), y = startY();
            for (int i = 0; i < MAX_HISTORY; i++) {
                final int hy = qmawMode ? y + 35 + (font.lineHeight + QMAW_ENTRY_Y_SPACING) * i : y + 50 + (font.lineHeight + 1) * i;
                if (qmawMode) {
                    if ((mouseX > (x + 4)) && (mouseY > (hy - (QMAW_ENTRY_Y_SPACING / 2))) && (mouseX < ((x + 4 + X_SIZE) - 9))
                            && (mouseY < (hy + font.lineHeight + (QMAW_ENTRY_Y_SPACING / 2)))) {
                        entry = i;
                        break;
                    }
                } else if ((mouseX > (x + 4)) && (mouseY > (hy - 1)) && (mouseX < ((x + 4 + X_SIZE) - 9)) && (mouseY < (hy + font.lineHeight))) {
                    entry = i;
                    break;
                }
            }
        }
        final boolean history = isInputEmpty();
        if (entry >= (qmawMode ? (history ? QMAW_HISTORY.size() : qmawResults.size()) : CALC_HISTORY.size())) entry = -1;
        return entry;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        inputField.setFocused(true);
        if (!isMouseInSegment(mouseX, mouseY)) return true;
        if ((selectedHist = mouseEntrySelected(mouseX, mouseY)) >= 0) {
            if (qmawMode) {
                if (button == MOUSE_LEFT) executeQMAW(isInputEmpty());
                else if (button == MOUSE_MIDDLE && isInputEmpty()) deleteQMAWHistoryEntry();
            } else {
                if (button == MOUSE_LEFT) handleCalculator(true, false, false, false); // Sometimes you gotta live by the jank
                else if (button == MOUSE_MIDDLE) deleteCalcHistoryEntry();
            }
        }
        return true;
    }

    private void selectedHistSanityCheck() {
        selectedHist = Mth.clamp(selectedHist, -1, (qmawMode ? (isInputEmpty() ? QMAW_HISTORY.size() : qmawResults.size()) : CALC_HISTORY.size()) - 1);
    }
}
