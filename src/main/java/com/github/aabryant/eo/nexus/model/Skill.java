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
package com.github.aabryant.eo.nexus.model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

public class Skill {
  public static enum SkillType {
    Normal, Boost, Break
  }
  
  public static class Dependency {
    protected final IntegerProperty id;
    protected final IntegerProperty index;
    protected final IntegerProperty level;
    
    public Dependency(int i, int x, int l) {
      id = new SimpleIntegerProperty(i);
      index = new SimpleIntegerProperty(x);
      level = new SimpleIntegerProperty(l);
    }
    
    public boolean isSatisfied(NexusCharacter character, boolean sub) {
      if (sub) return character.getSubSkills().get(index.get()) >= level.get();
      else return character.getSkills().get(index.get()) >= level.get();
    }
    
    public IntegerProperty idProperty() {
      return id;
    }

    public int getId() {
      return id.get();
    }

    public void setId(int value) {
      id.set(value);
    }

    public IntegerProperty indexProperty() {
      return index;
    }

    public int getIndex() {
      return index.get();
    }

    public void setIndex(int value) {
      index.set(value);
    }

    public IntegerProperty levelProperty() {
      return level;
    }

    public int getLevel() {
      return level.get();
    }

    public void setLevel(int value) {
      level.set(value);
    }
  }
  
  protected final StringProperty name;
  protected final IntegerProperty id;
  protected final IntegerProperty maxLevel;
  protected final IntegerProperty index;
  protected final IntegerProperty column;
  protected final DoubleProperty row;
  protected final ObjectProperty<SkillType> type;
  protected final StringProperty bodyParts;
  protected final StringProperty requiredEquipment;
  protected final StringProperty description;
  protected final ObservableList<Dependency> dependencies;
  public double[] hpBuff;
  public double[] tpBuff;
  
  public Skill(NodeList nodes, Document descriptions, XPath xpath) {
    name = new SimpleStringProperty();
    id = new SimpleIntegerProperty();
    maxLevel = new SimpleIntegerProperty();
    index = new SimpleIntegerProperty();
    column = new SimpleIntegerProperty();
    row = new SimpleDoubleProperty(0);
    type = new SimpleObjectProperty<>();
    bodyParts = new SimpleStringProperty();
    requiredEquipment = new SimpleStringProperty();
    description = new SimpleStringProperty();
    dependencies = FXCollections.observableArrayList();
    hpBuff = null;
    tpBuff = null;
    int len = nodes.getLength();
    Node node;
    for (int i = 0; i < len; i++) {
      node = nodes.item(i);
      switch (node.getNodeName()) {
        case "name":
          name.set(node.getTextContent());
          break;
        case "desc":
          description.set(node.getTextContent());
          break;
        case "id":
          id.set(Integer.parseInt(node.getTextContent()));
          break;
        case "mlvl":
          maxLevel.set(Integer.parseInt(node.getTextContent()));
          break;
        case "uses":
          bodyParts.set(node.getTextContent());
          break;
        case "equip":
          requiredEquipment.set(node.getTextContent());
          break;
        case "idx":
          index.set(Integer.parseInt(node.getTextContent()));
          break;
        case "col":
          column.set(Integer.parseInt(node.getTextContent()));
          break;
        case "row":
          row.set(Double.parseDouble(node.getTextContent()));
          break;
        case "type":
          type.set(SkillType.valueOf(node.getTextContent()));
          break;
        case "dependencies":
          NodeList deps = node.getChildNodes();
          int dlen = deps.getLength();
          for (int j = 0; j < dlen; j++) {
            if (deps.item(j).getNodeName().equals("dependency")) {
              NodeList data = deps.item(j).getChildNodes();
              int dlen2 = data.getLength();
              int d = 0, x = 0, l = 0;
              for (int k = 0; k < dlen2; k++) {
                node = data.item(k);
                switch (node.getNodeName()) {
                  case "id":
                    d = Integer.parseInt(node.getTextContent());
                    break;
                  case "idx":
                    x = Integer.parseInt(node.getTextContent());
                    break;
                  case "level":
                    l = Integer.parseInt(node.getTextContent());
                }
              }
              dependencies.add(new Dependency(d, x, l));
            }
          }
          break;
        case "buff":
          NodeList buffs = node.getChildNodes();
          int blen = buffs.getLength();
          for (int j = 0; j < blen; j++) {
            node = buffs.item(j);
            String[] levels;
            switch (node.getNodeName()) {
              case "hp":
                levels = node.getTextContent().split(",");
                hpBuff = new double[levels.length];
                for (int k = 0; k < hpBuff.length; k++) {
                  hpBuff[k] = Double.parseDouble(levels[k]);
                }
                break;
              case "tp":
                levels = node.getTextContent().split(",");
                tpBuff = new double[levels.length];
                for (int k = 0; k < tpBuff.length; k++) {
                  tpBuff[k] = Double.parseDouble(levels[k]);
                }
                break;
            }
          }
          break;
      }
    }
    if (description.get() == null) {
      try {
        XPathExpression ex = xpath.compile("/skills/skill/id[text()=\"" +
                                           id.get() + "\"]/../desc");
        Node desc = (Node) ex.evaluate(descriptions, XPathConstants.NODE);
        if (desc != null) description.set(desc.getTextContent());
      } catch (XPathExpressionException e) {
        e.printStackTrace();
      }
    }
  }
  
  public boolean dependenciesSatisfied(NexusCharacter character) {
    return dependenciesSatisfied(character, false);
  }
  
  public boolean dependenciesSatisfied(NexusCharacter character, boolean sub) {
    if (column.get() > 3 && character.getLevel() < 40) return false;
    if (column.get() > 1 && character.getLevel() < 20) return false;
    for (Dependency d : dependencies) {
      if (!d.isSatisfied(character, sub)) return false;
    }
    return true;
  }
  
  public boolean dependsOn(int id) {
    for (Dependency d : dependencies) if (d.getId() == id) return true;
    return false;
  }
  
  public StringProperty nameProperty() {
    return name;
  }

  public String getName() {
    return name.get();
  }

  public void setName(String value) {
    name.set(value);
  }

  public IntegerProperty idProperty() {
    return id;
  }

  public int getId() {
    return id.get();
  }

  public void setId(int value) {
    id.set(value);
  }

  public IntegerProperty maxLevelProperty() {
    return maxLevel;
  }

  public int getMaxLevel() {
    return maxLevel.get();
  }

  public void setMaxLevel(int value) {
    maxLevel.set(value);
  }

  public IntegerProperty indexProperty() {
    return index;
  }

  public int getIndex() {
    return index.get();
  }

  public void setIndex(int value) {
    index.set(value);
  }

  public IntegerProperty columnProperty() {
    return column;
  }

  public int getColumn() {
    return column.get();
  }

  public void setColumn(int value) {
    column.set(value);
  }
  
  public DoubleProperty rowProperty() {
    return row;
  }

  public double getRow() {
    return row.get();
  }

  public void setRow(double value) {
    row.set(value);
  }
  
  public ObjectProperty<SkillType> typeProperty() {
    return type;
  }

  public SkillType getType() {
    return type.get();
  }

  public void setType(SkillType value) {
    type.set(value);
  }

  public StringProperty bodyPartsProperty() {
    return bodyParts;
  }

  public String getBodyParts() {
    return bodyParts.get();
  }

  public void setBodyParts(String value) {
    bodyParts.set(value);
  }

  public StringProperty requiredEquipmentProperty() {
    return requiredEquipment;
  }

  public String getRequiredEquipment() {
    return requiredEquipment.get();
  }

  public void setRequiredEquipment(String value) {
    requiredEquipment.set(value);
  }

  public StringProperty descriptionProperty() {
    return description;
  }

  public String getDescription() {
    return description.get();
  }

  public void setDescription(String value) {
    description.set(value);
  }
  
  public ObservableList<Dependency> getDependencies() {
    return dependencies;
  }
}
