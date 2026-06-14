package com.blople.view.component;

import com.blople.common.Constants;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.LinkedHashMap;
import java.util.Map;

public class ColorPalettePicker extends JPanel {

    private final Map<String, JToggleButton> buttons = new LinkedHashMap<>();
    private final ButtonGroup group = new ButtonGroup();
    private String selectedHex = Constants.DEFAULT_BLOCK_COLOR_HEX;

    public ColorPalettePicker() {
        this(Constants.DEFAULT_BLOCK_COLOR_HEX);
    }

    public ColorPalettePicker(String initialHex) {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buildButtons();
        setSelectedHex(initialHex);
    }

    private void buildButtons() {
        for (Color color : Constants.BLOCK_COLOR_PALETTE) {
            String hex = toHex(color);
            JToggleButton btn = new JToggleButton();
            btn.setPreferredSize(new Dimension(26, 26));
            btn.setFocusPainted(false);
            btn.setBorderPainted(true);
            btn.setContentAreaFilled(true);
            btn.setOpaque(true);
            btn.setBackground(color);
            btn.setToolTipText(hex);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xCCCCCC), 1),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2)));

            btn.addActionListener(e -> {
                selectedHex = hex;
                refreshBorders();
            });

            group.add(btn);
            buttons.put(hex, btn);
            add(btn);
        }
        refreshBorders();
    }

    public String getSelectedHex() {
        return selectedHex;
    }

    public void setSelectedHex(String hex) {
        if (hex == null || hex.isBlank()) {
            selectedHex = Constants.DEFAULT_BLOCK_COLOR_HEX;
        } else {
            String normalized = normalize(hex);
            if (buttons.containsKey(normalized)) {
                selectedHex = normalized;
            } else {
                selectedHex = Constants.DEFAULT_BLOCK_COLOR_HEX;
            }
        }

        JToggleButton btn = buttons.get(selectedHex);
        if (btn != null) {
            btn.setSelected(true);
        }
        refreshBorders();
    }

    private void refreshBorders() {
        for (Map.Entry<String, JToggleButton> entry : buttons.entrySet()) {
            boolean selected = entry.getKey().equalsIgnoreCase(selectedHex);
            entry.getValue().setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(selected ? new Color(0x222222) : new Color(0xCCCCCC), selected ? 2 : 1),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2)));
        }
        repaint();
    }

    private static String toHex(Color color) {
        return String.format("#%06X", color.getRGB() & 0xFFFFFF);
    }

    private static String normalize(String hex) {
        String h = hex.trim().toUpperCase();
        return h.startsWith("#") ? h : "#" + h;
    }
}
