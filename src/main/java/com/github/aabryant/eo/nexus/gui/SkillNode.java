/*
 * Copyright (C) 2020 Amy Bryant
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.aabryant.eo.nexus.gui;

import java.io.File;
import java.util.function.BiConsumer;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.StackPane;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.github.aabryant.eo.nexus.model.Skill;

public class SkillNode extends StackPane {
  protected final static WebView TOOLTIP_VIEW;
  protected final static WebEngine TOOLTIP_ENGINE;
  protected final static Tooltip TOOLTIP;
  protected static Label hasTooltip;
  static {
    TOOLTIP_VIEW = new WebView();
    TOOLTIP_VIEW.setMaxWidth(700);
    TOOLTIP_VIEW.setMaxHeight(400);
    TOOLTIP_ENGINE = TOOLTIP_VIEW.getEngine();
    TOOLTIP_ENGINE.setUserDataDirectory(new File("data/tmp"));
    TOOLTIP_ENGINE.setUserStyleSheetLocation(SkillNode.class
                                             .getResource("/css/html-theme.css")
                                             .toString());
    TOOLTIP = new Tooltip();
    TOOLTIP.setMaxWidth(700);
    TOOLTIP.setMaxHeight(400);
    TOOLTIP.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    TOOLTIP.setGraphic(TOOLTIP_VIEW);
  }
  
  protected BiConsumer<SkillNode, MouseEvent> onClicked;
  protected Label text;
  protected Label level;
  protected final Skill skill;
  protected final boolean subJob;
  protected String description;
  
  public SkillNode(Skill s) {
    this(s, false);
  }
  
  public SkillNode(Skill s, boolean sub) {
    super();
    skill = s;
    subJob = sub;
    text = new Label(s.getName());
    StackPane.setAlignment(text, Pos.CENTER_LEFT);
    Rectangle e = new Rectangle();
    e.setFill(Color.web("#C6FFEC"));
    text.setPadding(new Insets(-2, 0, 0, 6));
    e.setWidth(175);
    e.setHeight(30);
    e.setArcWidth(12);
    e.setArcHeight(12);
    Rectangle e2 = new Rectangle();
    e2.setFill(Color.web("#0C4A7F"));
    e2.setWidth(173);
    e2.setHeight(28);
    e2.setArcWidth(12);
    e2.setArcHeight(12);
    
    Rectangle e3 = new Rectangle();
    e3.getStyleClass().add("skill-level-node");
    e3.setWidth(30);
    e3.setHeight(26);
    e3.setArcWidth(12);
    e3.setArcHeight(12);
    StackPane.setAlignment(e3, Pos.CENTER_LEFT);
    StackPane.setMargin(e3, new Insets(0, 0, 0, 143));
    
    int ml = s.getMaxLevel();
    if (subJob) ml /= 2;
    Label maxLevel = new Label(ml == 0 ? "★" : "" + ml);
    maxLevel.setStyle("-fx-font-size: 8pt;");
    StackPane.setAlignment(maxLevel, Pos.BOTTOM_RIGHT);
    if (ml == 10) {
      StackPane.setMargin(maxLevel, new Insets(-1, 3, 0, 0));
    } else {
      StackPane.setMargin(maxLevel, new Insets(-1, 6, 0, 0));
    }
    
    level = new Label(ml == 0 ? "★" : "0");
    level.setStyle("-fx-text-fill: #003D76; -fx-font-size: 10pt;");
    StackPane.setAlignment(level, Pos.CENTER_RIGHT);
    StackPane.setMargin(level, new Insets(-2, 16, 0, 0));
    
    
    getChildren().addAll(e, e2, e3, text, maxLevel, level);
    description = "<b>" + s.getName() + "</b><br>" + s.getDescription();
    setOnMouseEntered((evt) -> updateTooltip());
    setOnMouseClicked((evt) -> {
      if (onClicked != null) onClicked.accept(this, evt);
    });
  }
  
  public Skill getSkill() {
    return skill;
  }
  
  public void setLevel(int l) {
    int ml = skill.getMaxLevel();
    if (subJob) ml /= 2;
    if (l == ml) {
      level.setText("★");
    } else {
      level.setText(Integer.toString(l));
    }
  }
  
  public void updateTooltip() {
    if (hasTooltip == text) return;
    TOOLTIP_ENGINE.loadContent(description);
    text.setTooltip(TOOLTIP);
    if (hasTooltip != null) hasTooltip.setTooltip(null);
    hasTooltip = text;
  }
  
  public void setOnClicked(BiConsumer<SkillNode, MouseEvent> c) {
    onClicked = c;
  }
}
