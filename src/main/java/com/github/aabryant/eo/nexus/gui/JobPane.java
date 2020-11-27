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

import java.util.ArrayList;
import java.util.HashMap;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

import com.github.aabryant.eo.nexus.model.Job;
import com.github.aabryant.eo.nexus.model.NexusCharacter;
import com.github.aabryant.eo.nexus.model.Skill;

public class JobPane extends Group {
  protected NexusCharacter character;
  protected final Job job;
  protected final Pane backgroundPane;
  protected final boolean subJob;
  protected ObservableList<SkillNode> skillNodes;
  public final static HashMap<Integer, JobPane> JOBS = new HashMap<>();
  public final static HashMap<Integer, JobPane> SUB_JOBS = new HashMap<>();
  
  public JobPane(Job j) {
    this(j, false);
  }
  
  public JobPane(Job j, boolean s) {
    job = j;
    subJob = s;
    skillNodes = FXCollections.observableArrayList();
    backgroundPane = new Pane();
    backgroundPane.setLayoutX(0);
    backgroundPane.setLayoutY(0);
    backgroundPane.setMinWidth(1323);
    backgroundPane.setMaxWidth(1323);
    backgroundPane.setMinHeight(330);
    backgroundPane.setMaxHeight(330);
    backgroundPane.getStyleClass().add("skill-panels");
    getChildren().add(backgroundPane);
    addSkillNodes();
  }
  
  public void setSkillNodes() {
    for (SkillNode s : skillNodes) {
      if (s.getSkill().getType() == Skill.SkillType.Normal) {
        s.setLevel(charSkills().get(s.getSkill().getIndex()));
      }
    }
  }
  
  protected ObservableList<Integer> charSkills() {
    if (subJob) return character.getSubSkills();
    else return character.getSkills();
  }
  
  public void setCharacter(NexusCharacter c) {
    character = c;
    setSkillNodes();
  }
  
  protected void addSkillNodes() {
    ObservableList<Skill> sortedSkills = job.getSkills().sorted((s1, s2) -> {
      int i = Integer.compare(s1.getIndex(), s2.getIndex());
      return (s1.getColumn() == s2.getColumn() ? i : -i);
    });
    for (Skill s : sortedSkills) {
      SkillNode sn = new SkillNode(s, subJob);
      sn.setLayoutX(24 + s.getColumn() * 220);
      sn.setLayoutY(24 + s.getRow() * 42);
      getChildren().add(sn);
      int m = s.getMaxLevel();
      if (subJob) m /= 2;
      if (m > 0 || (s.getType() != Skill.SkillType.Normal && !subJob)) {
        sn.setOnClicked((n,e) -> {
          if (n.getSkill().getType() != Skill.SkillType.Normal) return;
          int idx = n.getSkill().getIndex();
          if (n.getSkill().dependenciesSatisfied(character, subJob)) {
            if (e.getButton() == MouseButton.PRIMARY) {
              int ml = n.getSkill().getMaxLevel();
              if (subJob) ml /= 2;
              if (charSkills().get(idx) < ml) {
                if (subJob) character.addSubSkillLevel(idx, 1);
                else character.addSkillLevel(idx, 1);
              }
            } else if (e.getButton() == MouseButton.SECONDARY) {
              if (charSkills().get(idx) > 0) {
                if (subJob) character.addSubSkillLevel(idx, -1);
                else character.addSkillLevel(idx, -1);
              }
            }
            character.removeInvalidSkills();
            setSkillNodes();
          }
        });
      } else {
        sn.setVisible(false);
      }
      skillNodes.add(sn);
    }
    Color lineColor = Color.web("#C6FFEC");
    for (SkillNode sn : skillNodes) {
      Skill s = sn.getSkill();
      if (s.getDependencies().size() == 1) {
        Skill s2 = job.getSkills().get(s.getDependencies().get(0).getIndex());
        SkillNode sn2 = skillNodes.get(sortedSkills.indexOf(s2));
        Line l;
        if (job.numDependents(s2.getId()) < 2) {
          double w = sn.getLayoutX() - (sn2.getLayoutX() + 175);
          l = new Line(0, 0, w, 0);
          l.setLayoutX(sn.getLayoutX() - w);
        } else {
          l = new Line(0, 0, 15, 0);
          l.setLayoutX(sn.getLayoutX() - 15);
        }
        l.setLayoutY(sn.getLayoutY() + 15);
        l.setStroke(lineColor);
        getChildren().add(l);
        if (job.numDependents(s2.getId()) < 2) {
          double tx = sn2.getLayoutX() + 175 + 3;
          Label lbl1 = new Label("Lv");
          lbl1.setStyle("-fx-text-fill: #FFFF99; -fx-font-size: 8pt;");
          Label lbl2 = new Label("" + s.getDependencies().get(0).getLevel());
          lbl2.setStyle("-fx-text-fill: #FFFF99; -fx-font-size: 10pt;");
          lbl1.setLayoutX(tx);
          lbl1.setLayoutY(sn2.getLayoutY() + 2);
          getChildren().add(lbl1);
          lbl2.setLayoutX(tx + 12);
          lbl2.setLayoutY(sn2.getLayoutY());
          getChildren().add(lbl2);
        } else if (job.numDependents(s2.getId()) > 1 &&
                   (s2.getRow() - 0.5 == s.getRow() ||
                    s2.getRow() == s.getRow())) {
          l = new Line(0, 0, sn.getLayoutX() - sn2.getLayoutX() - 191, 0);
          l.setLayoutX(sn2.getLayoutX() + 175);
          l.setLayoutY(sn2.getLayoutY() + 15);
          l.setStroke(lineColor);
          getChildren().add(l);
          l = new Line(0, 0, 1, 42 * (job.numDependents(s2.getId()) - 1));
          l.setLayoutX(sn.getLayoutX() - 16);
          l.setLayoutY(sn2.getLayoutY() - (l.getEndY() / 2.0) + 15);
          l.setStroke(lineColor);
          getChildren().add(l);
          double tx = sn2.getLayoutX() + 175 + 3;
          Label lbl1 = new Label("Lv");
          lbl1.setStyle("-fx-text-fill: #FFFF99; -fx-font-size: 8pt;");
          Label lbl2 = new Label("" + s.getDependencies().get(0).getLevel());
          lbl2.setStyle("-fx-text-fill: #FFFF99; -fx-font-size: 10pt;");
          lbl1.setLayoutX(tx);
          lbl1.setLayoutY(sn2.getLayoutY() + 2);
          getChildren().add(lbl1);
          lbl2.setLayoutX(tx + 12);
          lbl2.setLayoutY(sn2.getLayoutY());
          getChildren().add(lbl2);
        }
      } else if (s.getDependencies().size() == 2) {
        Line l = new Line(0, 0, 15, 0);
        l.setStroke(lineColor);
        l.setLayoutX(sn.getLayoutX() - 15);
        l.setLayoutY(sn.getLayoutY() + 15);
        getChildren().add(l);
        Skill s2 = job.getSkills().get(s.getDependencies().get(0).getIndex());
        SkillNode sn2 = skillNodes.get(sortedSkills.indexOf(s2));
        s2 = job.getSkills().get(s.getDependencies().get(1).getIndex());
        SkillNode sn3 = skillNodes.get(sortedSkills.indexOf(s2));
        if (sn2.getLayoutY() > sn3.getLayoutY()) {
          l = new Line(0, 0, 1, sn2.getLayoutY() - sn3.getLayoutY());
          l.setLayoutY(sn3.getLayoutY() + 15);
        } else {
          l = new Line(0, 0, 1, sn3.getLayoutY() - sn2.getLayoutY());
          l.setLayoutY(sn2.getLayoutY() + 15);
        }
        l.setLayoutX(sn.getLayoutX() - 16);
        l.setStroke(lineColor);
        getChildren().add(l);
        double x = sn.getLayoutX() - 16;
        l = new Line(0, 0, x - sn2.getLayoutX() - 175, 0);
        l.setLayoutX(sn2.getLayoutX() + 175);
        l.setLayoutY(sn2.getLayoutY() + 15);
        l.setStroke(lineColor);
        getChildren().add(l);
        double tx = sn2.getLayoutX() + 175 + 3;
        Label lbl1 = new Label("Lv");
        lbl1.setStyle("-fx-text-fill: #FFFF99; -fx-font-size: 8pt;");
        Label lbl2 = new Label("" + s.getDependencies().get(0).getLevel());
        lbl2.setStyle("-fx-text-fill: #FFFF99; -fx-font-size: 10pt;");
        lbl1.setLayoutX(tx);
        lbl1.setLayoutY(sn2.getLayoutY() + 2);
        getChildren().add(lbl1);
        lbl2.setLayoutX(tx + 12);
        lbl2.setLayoutY(sn2.getLayoutY());
        getChildren().add(lbl2);
        l = new Line(0, 0, x - sn3.getLayoutX() - 175, 0);
        l.setLayoutX(sn3.getLayoutX() + 175);
        l.setLayoutY(sn3.getLayoutY() + 15);
        l.setStroke(lineColor);
        getChildren().add(l);
        tx = sn3.getLayoutX() + 175 + 3;
        lbl1 = new Label("Lv");
        lbl1.setStyle("-fx-text-fill: #FFFF99; -fx-font-size: 8pt;");
        lbl2 = new Label("" + s.getDependencies().get(1).getLevel());
        lbl2.setStyle("-fx-text-fill: #FFFF99; -fx-font-size: 10pt;");
        lbl1.setLayoutX(tx);
        lbl1.setLayoutY(sn3.getLayoutY() + 2);
        getChildren().add(lbl1);
        lbl2.setLayoutX(tx + 12);
        lbl2.setLayoutY(sn3.getLayoutY());
        getChildren().add(lbl2);
      } else if (s.getDependencies().size() == 3) {
        Line l = new Line(0, 0, 15, 0);
        l.setStroke(lineColor);
        l.setLayoutX(sn.getLayoutX() - 15);
        l.setLayoutY(sn.getLayoutY() + 15);
        getChildren().add(l);
        Skill s2 = job.getSkills().get(s.getDependencies().get(0).getIndex());
        SkillNode sn2 = skillNodes.get(sortedSkills.indexOf(s2));
        s2 = job.getSkills().get(s.getDependencies().get(2).getIndex());
        SkillNode sn3 = skillNodes.get(sortedSkills.indexOf(s2));
        if (sn2.getLayoutY() > sn3.getLayoutY()) {
          l = new Line(0, 0, 1, sn2.getLayoutY() - sn3.getLayoutY());
          l.setLayoutY(sn3.getLayoutY() + 15);
        } else {
          l = new Line(0, 0, 1, sn3.getLayoutY() - sn2.getLayoutY());
          l.setLayoutY(sn2.getLayoutY() + 15);
        }
        l.setLayoutX(sn.getLayoutX() - 16);
        l.setStroke(lineColor);
        getChildren().add(l);
        double xroot = l.getLayoutX();
        l = new Line(0, 0, xroot - sn2.getLayoutX() - 175, 0);
        l.setStroke(lineColor);
        l.setLayoutX(sn.getLayoutX() - 16 - l.getEndX());
        l.setLayoutY(sn2.getLayoutY() + 15);
        getChildren().add(l);
        double tx = (l.getLayoutX() + l.getEndX() / 2) - 12;
        Label lbl1 = new Label("Lv");
        lbl1.setStyle("-fx-text-fill: #FFFF99; -fx-font-size: 8pt;");
        Label lbl2 = new Label("" + s.getDependencies().get(0).getLevel());
        lbl2.setStyle("-fx-text-fill: #FFFF99; -fx-font-size: 10pt;");
        lbl1.setLayoutX(tx);
        lbl1.setLayoutY(sn2.getLayoutY() + 2);
        getChildren().add(lbl1);
        lbl2.setLayoutX(tx + 12);
        lbl2.setLayoutY(sn2.getLayoutY());
        getChildren().add(lbl2);
        l = new Line(0, 0, xroot - sn3.getLayoutX() - 175, 0);
        l.setStroke(lineColor);
        l.setLayoutX(sn.getLayoutX() - 16 - l.getEndX());
        l.setLayoutY(sn3.getLayoutY() + 15);
        getChildren().add(l);
        lbl1 = new Label("Lv");
        lbl1.setStyle("-fx-text-fill: #FFFF99; -fx-font-size: 8pt;");
        lbl2 = new Label("" + s.getDependencies().get(2).getLevel());
        lbl2.setStyle("-fx-text-fill: #FFFF99; -fx-font-size: 10pt;");
        lbl1.setLayoutX(tx);
        lbl1.setLayoutY(sn3.getLayoutY() + 2);
        getChildren().add(lbl1);
        lbl2.setLayoutX(tx + 12);
        lbl2.setLayoutY(sn3.getLayoutY());
        getChildren().add(lbl2);
        s2 = job.getSkills().get(s.getDependencies().get(1).getIndex());
        sn2 = skillNodes.get(sortedSkills.indexOf(s2));
        l = new Line(0, 0, xroot - sn2.getLayoutX() - 175, 0);
        l.setStroke(lineColor);
        l.setLayoutX(sn.getLayoutX() - 16 - l.getEndX());
        l.setLayoutY(sn2.getLayoutY() + 15);
        getChildren().add(l);
        lbl1 = new Label("Lv");
        lbl1.setStyle("-fx-text-fill: #FFFF99; -fx-font-size: 8pt;");
        lbl2 = new Label("" + s.getDependencies().get(1).getLevel());
        lbl2.setStyle("-fx-text-fill: #FFFF99; -fx-font-size: 10pt;");
        lbl1.setLayoutX(tx);
        lbl1.setLayoutY(sn2.getLayoutY() + 2);
        getChildren().add(lbl1);
        lbl2.setLayoutX(tx + 12);
        lbl2.setLayoutY(sn2.getLayoutY());
        getChildren().add(lbl2);
      }
    }
  }
}
