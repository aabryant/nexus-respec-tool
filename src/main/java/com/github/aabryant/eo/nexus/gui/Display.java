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
import java.util.HashMap;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Priority;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;


import com.github.aabryant.eo.nexus.model.Job;
import com.github.aabryant.eo.nexus.model.NexusCharacter;
import com.github.aabryant.eo.nexus.model.Skill;
import com.github.aabryant.eo.nexus.manager.Data;

public class Display extends VBox {
  protected MenuBar menuBar;
  protected Menu fileMenu;
  protected MenuItem openFile;
  protected MenuItem saveFile;
  protected MenuItem saveFileAs;
  protected Menu batchMenu;
  protected MenuItem syncExp;
  protected MenuItem syncLevels;
  protected MenuItem retireAll;
  protected Menu presetMenu;
  protected MenuItem savePreset;
  protected MenuItem loadPreset;
  protected HBox layout;
  protected VBox controls;
  protected VBox jobDisplay;
  protected ListView<NexusCharacter> characterList;
  protected Label classNameLabel;
  protected Label level;
  protected Label exp;
  protected Label remainingSp;
  protected Label jobLabel;
  protected Label subJobLabel;
  protected Label retirement;
  protected TextField className;
  protected ComboBox<Job> subSelect;
  protected Button resetSp;
  protected Button retire;
  protected ChangeListener<? super java.lang.Number> spListener;
  protected boolean settingData;
  
  public Display() {
    super();
    setup();
  }
  
  protected void setup() {
    addMenuBar();
    addControls();
  }
  
  protected void addMenuBar() {
    menuBar = new MenuBar();
    fileMenu = new Menu("_File");
    openFile = new MenuItem("_Open File");
    openFile.setOnAction((e) -> open());
    saveFile = new MenuItem("_Save File");
    saveFile.setOnAction((e) -> save());
    saveFileAs = new MenuItem("Save File _As");
    saveFileAs.setOnAction((e) -> saveAs());
    fileMenu.getItems().addAll(openFile, saveFile, saveFileAs);
    presetMenu = new Menu("_Preset");
    presetMenu.setVisible(false);
    savePreset = new MenuItem("_Save Preset");
    savePreset.setOnAction((e) -> savePreset());
    loadPreset = new MenuItem("_Load Preset");
    loadPreset.setOnAction((e) -> loadPreset());
    presetMenu.getItems().addAll(savePreset, loadPreset);
    batchMenu = new Menu("_Batch");
    batchMenu.setVisible(false);
    syncExp = new MenuItem("Sync _EXP");
    syncExp.setOnAction((e) -> syncExp());
    syncLevels = new MenuItem("Sync _Levels");
    syncLevels.setOnAction((e) -> syncLevels());
    retireAll = new MenuItem("_Retire All");
    retireAll.setOnAction((e) -> retireAll());
    batchMenu.getItems().addAll(syncExp, syncLevels, retireAll);
    menuBar.getMenus().addAll(fileMenu, presetMenu, batchMenu);
    getChildren().add(menuBar);
  }
  
  protected void addControls() {
    layout = new HBox();
    VBox.setVgrow(layout, Priority.ALWAYS);
    addCharacterList();
    addInfoBox();
    addJobDisplay();
    getChildren().add(layout);
    spListener = (o, ov, nv) -> {
      remainingSp.setText("SP: " + nv + " / " +
                          getSelectedCharacter().getMaxSp());
    };
  }
  
  protected void addCharacterList() {
    characterList = new ListView<>();
    characterList.getSelectionModel().selectedItemProperty()
                                     .addListener((s,o,n) -> reloadInfo(o, n));
    layout.getChildren().add(characterList);
  }
  
  protected void addInfoBox() {
    controls = new VBox();
    HBox.setHgrow(controls, Priority.ALWAYS);
    HBox infoBox = new HBox();
    infoBox.setPadding(new Insets(6, 6, 6, 6));
    infoBox.setSpacing(24);
    infoBox.setMinHeight(30);
    infoBox.getStyleClass().add("info-box");
    infoBox.setAlignment(Pos.CENTER_LEFT);
    infoBox.getChildren().add(jobLabel = new Label(""));
    infoBox.getChildren().add(subJobLabel = new Label("Subclass:"));
    infoBox.getChildren().add(subSelect = new ComboBox<>(Data.JOB_LIST));
    subSelect.getSelectionModel().select(0);
    subSelect.getSelectionModel().selectedItemProperty()
                                 .addListener((o, ov, nv) -> {
      if (settingData || ov == nv) return;
      NexusCharacter c = getSelectedCharacter();
      if (c != null) {
        if (c.getSub() == 255) c.setSp(c.getSp() + 5);
        else if (nv.getId() == 255) c.setSp(c.getSp() - 5);
        c.setSub(nv.getId());
        c.clearSubSkills();
        reloadInfo(c, c);
      }
    });
    subJobLabel.setVisible(false);
    subSelect.setVisible(false);
    infoBox.getChildren().add(resetSp = new Button("Reset SP"));
    resetSp.setOnAction((e) -> resetSp());
    resetSp.setVisible(false);
    infoBox.getChildren().add(retire = new Button("Retire"));
    retire.setOnAction((e) -> retire());
    retire.setVisible(false);
    infoBox.getChildren().add(level = new Label());
    infoBox.getChildren().add(retirement = new Label());
    infoBox.getChildren().add(exp = new Label());
    infoBox.getChildren().add(remainingSp = new Label());
    infoBox.getChildren().add(classNameLabel = new Label("Class Name:"));
    classNameLabel.setVisible(false);
    infoBox.getChildren().add(className = new TextField());
    className.setTextFormatter(new TextFormatter<TextFormatter.Change>((c) -> {
      if (c.isContentChange() && c.getControlNewText().length() > 37) {
        return null;
      }
      return c;
    }));
    className.textProperty().addListener((o, ov, nv) -> {
      if (settingData || ov == nv) return;
      NexusCharacter c = getSelectedCharacter();
      if (c != null) {
        c.setClassName(nv);
      }
    });
    HBox.setHgrow(className, Priority.ALWAYS);
    className.setVisible(false);
    controls.getChildren().add(infoBox);
  }
  
  protected void addJobDisplay() {
    jobDisplay = new VBox();
    ScrollPane scrollPane = new ScrollPane(jobDisplay);
    VBox.setVgrow(scrollPane, Priority.ALWAYS);
    scrollPane.setPadding(new Insets(10, 10, 10, 10));
    controls.getChildren().add(scrollPane);
    layout.getChildren().add(controls);
  }
  
  protected void reloadInfo(NexusCharacter o, NexusCharacter n) {
    settingData = true;
    jobDisplay.getChildren().clear();
    if (n == null) return;
    jobLabel.setText("Class: " + Data.JOBS.get(n.getJob()).getName());
    level.setText("Level: " + n.getLevel());
    exp.setText("EXP: " + n.getExp());
    remainingSp.setText("SP: " + n.getSp() + " / " + n.getMaxSp());
    className.setText(n.getClassName());
    if (o != null) o.spProperty().removeListener(spListener);
    n.spProperty().addListener(spListener);
    subSelect.getSelectionModel().select(Data.JOBS.get(n.getSub()));
    if (n.getJob() != 0xFF) {
      JobPane p;
      if (JobPane.JOBS.containsKey(n.getJob())) {
        p = JobPane.JOBS.get(n.getJob());
      } else {
        p = new JobPane(Data.JOBS.get(n.getJob()));
        JobPane.JOBS.put(n.getJob(), p);
      }
      p.setCharacter(n);
      jobDisplay.getChildren().add(p);
      if (n.getSub() != 0xFF) {
        if (JobPane.SUB_JOBS.containsKey(n.getSub())) {
          p = JobPane.SUB_JOBS.get(n.getSub());
        } else {
          p = new JobPane(Data.JOBS.get(n.getSub()), true);
          JobPane.SUB_JOBS.put(n.getSub(), p);
        }
        p.setCharacter(n);
        jobDisplay.getChildren().add(p);
      }
    }
    String retiredAt;
    if (n.getRetirementLevel() == 0) retiredAt = "N/A";
    else retiredAt = Integer.toString(n.getRetirementLevel());
    retirement.setText("Retired: " + retiredAt);
    settingData = false;
  }
  
  protected NexusCharacter getSelectedCharacter() {
    return characterList.getSelectionModel().getSelectedItem();
  }
  
  public void open() {
    FileChooser chooser = new FileChooser();
    if (Data.SAVE_PATH != null) {
      File initial = new File(Data.SAVE_PATH);
      if (initial.isDirectory()) {
        chooser.setInitialDirectory(initial);
      } else {
        chooser.setInitialDirectory(new File(initial.getParent()));
        chooser.setInitialFileName(initial.getName());
      }
    }
    File file = chooser.showOpenDialog(null);
    if (file != null) {
      if (Data.loadSave(file.getAbsolutePath())) {
        characterList.setItems(Data.CHARACTERS.filtered((c) -> {
          return !c.getName().isEmpty();
        }));
        subJobLabel.setVisible(true);
        subSelect.setVisible(true);
        presetMenu.setVisible(true);
        batchMenu.setVisible(true);
        resetSp.setVisible(true);
        retire.setVisible(true);
        classNameLabel.setVisible(true);
        className.setVisible(true);
        characterList.getSelectionModel().select(0);
      }
    }
  }
  
  public void save() {
    if (Data.SAVE_PATH != null) Data.saveFile();
  }
  
  public void saveAs() {
    if (Data.SAVE_PATH != null) {
      FileChooser chooser = new FileChooser();
      File initial = new File(Data.SAVE_PATH);
      chooser.setInitialDirectory(new File(initial.getParent()));
      chooser.setInitialFileName(initial.getName());
      File file = chooser.showSaveDialog(null);
      if (file != null) Data.saveFile(file.getAbsolutePath());
    }
  }
  
  protected void savePreset() {
    if (getSelectedCharacter() == null) return;
    new PresetDialog().showSave(Data.JOBS.get(getSelectedCharacter().getJob()),
                                getSelectedCharacter());
  }
  
  protected void loadPreset() {
    if (getSelectedCharacter() == null) return;
    Job.Preset p = new PresetDialog().showLoad(Data.JOBS.get(
                                              getSelectedCharacter().getJob()));
    if (p != null && p.canApply(getSelectedCharacter())) {
      p.apply(getSelectedCharacter());
      reloadInfo(null, getSelectedCharacter());
    }
  }
  
  protected void resetSp() {
    if (getSelectedCharacter() == null) return;
    getSelectedCharacter().resetSp();
    reloadInfo(null, getSelectedCharacter());
  }
  
  protected void retire() {
    if (getSelectedCharacter() == null) return;
    getSelectedCharacter().retire();
    reloadInfo(null, getSelectedCharacter());
  }
  
  protected void syncExp() {
    if (getSelectedCharacter() == null) return;
    HashMap<Integer,Integer> expValues = new HashMap<>();
    for (NexusCharacter c : Data.CHARACTERS) {
      if (!expValues.containsKey(c.getLevel())) {
        expValues.put(c.getLevel(), c.getExp());
      } else {
        if (c.getExp() > expValues.get(c.getLevel())) {
          expValues.put(c.getLevel(), c.getExp());
        }
      }
    }
    for (NexusCharacter c : Data.CHARACTERS) {
      if (c.getLevel() == 0) continue;
      c.setExp(expValues.get(c.getLevel()));
    }
    reloadInfo(null, getSelectedCharacter());
  }
  
  protected void syncLevels() {
    if (getSelectedCharacter() == null) return;
    int maxLevel = 0;
    int maxExp = 0;
    for (NexusCharacter c : Data.CHARACTERS) {
      if (c.getLevel() > maxLevel) maxLevel = c.getLevel();
      if (c.getExp() > maxExp) maxExp = c.getExp();
    }
    for (NexusCharacter c : Data.CHARACTERS) {
      if (c.getLevel() == 0) continue;
      c.setLevel(maxLevel);
      c.setExp(maxExp);
    }
    reloadInfo(null, getSelectedCharacter());
  }
  
  protected void retireAll() {
    if (getSelectedCharacter() == null) return;
    for (NexusCharacter c : Data.CHARACTERS) {
      if (c.getLevel() < 30) continue;
      c.retire();
    }
    reloadInfo(null, getSelectedCharacter());
  }
}
