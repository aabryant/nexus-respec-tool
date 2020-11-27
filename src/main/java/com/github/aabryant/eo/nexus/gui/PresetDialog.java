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

import java.util.Optional;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import com.github.aabryant.eo.nexus.manager.Data;
import com.github.aabryant.eo.nexus.model.Job;
import com.github.aabryant.eo.nexus.model.Job.Preset;
import com.github.aabryant.eo.nexus.model.NexusCharacter;
import com.github.aabryant.eo.nexus.model.Skill;

public class PresetDialog extends Dialog<ButtonType> {
  protected static Preset EMPTY = new Preset();
  protected static class PresetDialogPane extends DialogPane {
    protected Button okButton;
    protected TableView<Preset> presetTable;
    protected Job job;
    protected boolean saveMode;
    protected String saveName;
    
    protected PresetDialogPane() {
      super();
      saveMode = false;
      getStylesheets().add(getClass().getResource("/css/theme.css")
                                     .toExternalForm());
      VBox content = new VBox();
      content.setSpacing(12);
      presetTable = new TableView<>();
      presetTable.setMinWidth(600);
      TableColumn<Preset, String> nameColumn = new TableColumn<>("Name");
      nameColumn.setCellValueFactory(new PropertyValueFactory("name"));
      nameColumn.setResizable(false);
      nameColumn.prefWidthProperty()
                .bind(presetTable.widthProperty().multiply(0.4));
      TableColumn<Preset, String> spColumn = new TableColumn<>("SP Cost");
      spColumn.setCellValueFactory((p) -> {
        if (p.getValue().getName().isEmpty()) {
          return p.getValue().nameProperty();
        }
        return new SimpleStringProperty(Integer.toString(p.getValue()
                                                          .getRequiredSp()));
      });
      spColumn.setResizable(false);
      spColumn.prefWidthProperty()
              .bind(presetTable.widthProperty().multiply(0.2));
      TableColumn<Preset, String> subColumn = new TableColumn<>("Subclass");
      subColumn.setCellValueFactory((p) -> {
        if (p.getValue().getName().isEmpty()) {
          return p.getValue().nameProperty();
        }
        return Data.JOBS.get(p.getValue().sub.get()).nameProperty();
      });
      subColumn.setResizable(false);
      subColumn.prefWidthProperty()
               .bind(presetTable.widthProperty().multiply(0.4));
      presetTable.getColumns().setAll(nameColumn, spColumn, subColumn);
      presetTable.setRowFactory((t) -> {
        TableRow<Preset> row = new TableRow<Preset>() {
          @Override
          public void updateItem(Preset item, boolean empty) {
            super.updateItem(item, empty);
            if (item == null || item.getName().isEmpty()) {
              setTooltip(null);
            } else {
              Tooltip tooltip = new Tooltip();
              tooltip.setStyle("-fx-font-size: 10pt;");
              if (item.description == null) {
                ObservableList<Skill> sortedSkills = job.getSkills()
                                                        .sorted((s1, s2) -> {
                  return Integer.compare(s1.getIndex(), s2.getIndex());
                });
                StringBuilder sb = new StringBuilder();
                sb.append(job.getName()).append("\n");
                for (Skill s : sortedSkills) {
                  if (s.getIndex() >= item.skills.length) continue;
                  if (item.skills[s.getIndex()] > 0) {
                    sb.append("  ").append(s.getName()).append(": ");
                    sb.append(item.skills[s.getIndex()]).append("\n");
                  }
                }
                sb.append(Data.JOBS.get(item.sub.get()).getName()).append("\n");
                sortedSkills = Data.JOBS.get(item.sub.get()).getSkills()
                                                           .sorted((s1, s2) -> {
                  return Integer.compare(s1.getIndex(), s2.getIndex());
                });
                for (Skill s : sortedSkills) {
                  if (s.getIndex() >= item.subSkills.length) continue;
                  if (item.subSkills[s.getIndex()] > 0) {
                    sb.append("  ").append(s.getName()).append(": ");
                    sb.append(item.subSkills[s.getIndex()]).append("\n");
                  }
                }
                item.description = sb.toString();
              }
              tooltip.setText(item.description);
              setTooltip(tooltip);
            }
          }
        };
        row.setOnMouseClicked((e) -> {
          if (!row.isEmpty() && e.getButton() == MouseButton.PRIMARY &&
              e.getClickCount() > 1) {
            if (saveMode) {
              if (getSelectedPreset().getName().isEmpty()) {
                TextInputDialog input = new TextInputDialog();
                input.getDialogPane().getStylesheets().add(getClass().
                                getResource("/css/theme.css").toExternalForm());
                input.setHeaderText("What would you like to call this preset?");
                input.showAndWait().ifPresent((r) -> {
                  if (!r.isEmpty()) {
                    saveName = r;
                    okButton.fire();
                  }
                });
              } else {
                Alert alert = new Alert(AlertType.CONFIRMATION,
                             "Are you sure you want to overwrite the preset '" +
                                          getSelectedPreset().getName() + "'?");
                alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
                alert.getDialogPane().getStylesheets().add(getClass().
                                getResource("/css/theme.css").toExternalForm());
                alert.showAndWait().ifPresent((r) -> {
                  if (r == ButtonType.OK) {
                    saveName = getSelectedPreset().getName();
                    job.getPresets().remove(getSelectedPreset());
                    okButton.fire();
                  }
                });
              }
            } else {
              if (!getSelectedPreset().getName().isEmpty()) okButton.fire();
            }
          }
        });
        return row ;
      });
      presetTable.setOnKeyPressed((e) -> {
        if (getSelectedPreset().getName().isEmpty()) return;
        if (e.getCode() == KeyCode.DELETE) {
          Alert alert = new Alert(AlertType.CONFIRMATION,
                                "Are you sure you want to delete the preset '" +
                                          getSelectedPreset().getName() + "'?");
          alert.getDialogPane().getStylesheets().add(getClass().
                                getResource("/css/theme.css").toExternalForm());
          alert.getDialogPane().setMinHeight(Region.USE_PREF_SIZE);
          alert.showAndWait().ifPresent((r) -> {
            if (r == ButtonType.OK) {
              job.getPresets().remove(getSelectedPreset());
              Data.savePresets();
            }
          });
        }
      });
      
      ButtonBar buttonBar = (ButtonBar) createButtonBar();
      okButton = (Button) createButton(ButtonType.OK);
      buttonBar.getButtons().add(okButton);
      Button cancelButton = (Button) createButton(ButtonType.CANCEL);
      buttonBar.getButtons().add(cancelButton);
      content.getChildren().addAll(presetTable, buttonBar);
      setContent(content);
    }
    
    protected void setJob(Job j) {
      job = j;
      presetTable.setItems(job.getPresets());
    }
    
    protected Preset getSelectedPreset() {
      return presetTable.getSelectionModel().getSelectedItem();
    }
  }
  
  public PresetDialog() {
    super();
    setTitle("Presets");
    setDialogPane(new PresetDialogPane());
    ((PresetDialogPane) getDialogPane()).okButton.setOnAction((e) -> {});
    initModality(Modality.APPLICATION_MODAL);
  }
  
  public Preset showLoad(Job job) {
    job.getPresets().add(EMPTY);
    ((PresetDialogPane) getDialogPane()).saveMode = false;
    ((PresetDialogPane) getDialogPane()).setJob(job);
    Optional<ButtonType> result = showAndWait();
    job.getPresets().remove(EMPTY);
    if (result.isPresent() && result.get() == ButtonType.OK) {
      return ((PresetDialogPane) getDialogPane()).getSelectedPreset();
    }
    return null;
  }
  
  public boolean showSave(Job job, NexusCharacter c) {
    job.getPresets().add(EMPTY);
    ((PresetDialogPane) getDialogPane()).saveMode = true;
    ((PresetDialogPane) getDialogPane()).setJob(job);
    Optional<ButtonType> result = showAndWait();
    job.getPresets().remove(EMPTY);
    if (result.isPresent() && result.get() == ButtonType.OK) {
      job.createPreset(((PresetDialogPane) getDialogPane()).saveName, c);
      return true;
    }
    return false;
  }
}
