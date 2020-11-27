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

import java.util.StringJoiner;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Document;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import com.github.aabryant.eo.nexus.manager.Data;

public class Job {
  protected final StringProperty name;
  protected final IntegerProperty id;
  protected final ObservableList<Skill> skills;
  protected final ObservableList<Preset> presets;
  protected final int[][] stats;
  
  public static class Preset {
    public final StringProperty name;
    public final IntegerProperty sub;
    public final int[] skills;
    public final int[] subSkills;
    public final IntegerProperty requiredSp;
    public String description;
    
    public Preset(NodeList nodes) {
      name = new SimpleStringProperty();
      sub = new SimpleIntegerProperty();
      requiredSp = new SimpleIntegerProperty();
      skills = new int[24];
      subSkills = new int[24];
      int len = nodes.getLength();
      Node node;
      String[] s;
      int sp = 0;
      for (int i = 0; i < len; i++) {
        node = nodes.item(i);
        switch (node.getNodeName()) {
          case "name":
            name.set(node.getTextContent());
            break;
          case "sub_id":
            sub.set(Integer.parseInt(node.getTextContent()));
            break;
          case "skills":
            s = node.getTextContent().split(",");
            for (int j = 0; j < 24; j++) {
              skills[j] = Integer.parseInt(s[j]);
              sp += skills[j];
            }
            break;
          case "subclass_skills":
            s = node.getTextContent().split(",");
            for (int j = 0; j < 24; j++) {
              subSkills[j] = Integer.parseInt(s[j]);
              sp += subSkills[j];
            }
            break;
        }
      }
      requiredSp.set(sp);
    }
    
    public Preset() {
      name = new SimpleStringProperty("");
      sub = new SimpleIntegerProperty(0);
      skills = new int[0];
      subSkills = new int[0];
      requiredSp = new SimpleIntegerProperty(0);
    }
    
    public Preset(String n, NexusCharacter c) {
      name = new SimpleStringProperty(n);
      sub = new SimpleIntegerProperty(c.getSub());
      skills = new int[24];
      subSkills = new int[24];
      int sp = 0;
      for (int i = 0; i < 24; i++) {
        skills[i] = c.getSkills().get(i);
        sp += c.getSkills().get(i);
        subSkills[i] = c.getSubSkills().get(i);
        sp += c.getSubSkills().get(i);
      }
      requiredSp = new SimpleIntegerProperty(sp);
    }
    
    public boolean canApply(NexusCharacter c) {
      int max = c.getMaxSp();
      if (c.getSub() == 0xFF && sub.get() != 0xFF) max += 5;
      return requiredSp.get() <= max;
    }
    
    public String toString() {
      return name.get();
    }
    
    public StringProperty nameProperty() {
      return name;
    }
    
    public String getName() {
      return name.get();
    }
    
    public IntegerProperty requiredSpProperty() {
      return requiredSp;
    }
    
    public int getRequiredSp() {
      return requiredSp.get();
    }
    
    public boolean apply(NexusCharacter c) {
      if (!canApply(c)) return false;
      c.setSub(sub.get());
      for (int i = 0; i < 24; i++) {
        c.getSkills().set(i, skills[i]);
        c.getSubSkills().set(i, subSkills[i]);
      }
      c.setSp(c.getMaxSp() - requiredSp.get());
      c.removeInvalidSkills();
      return true;
    }
    
    public String getSkillsAsString() {
      StringJoiner out = new StringJoiner(",");
      for (int i = 0; i < 24; i++) out.add(Integer.toString(skills[i]));
      return out.toString();
    }
    
    public String getSubSkillsAsString() {
      StringJoiner out = new StringJoiner(",");
      for (int i = 0; i < 24; i++) out.add(Integer.toString(subSkills[i]));
      return out.toString();
    }
    
    public Node toXML(Document document) {
      Element preset = document.createElement("preset");
      Element attribute;
      
      attribute = document.createElement("name");
      attribute.appendChild(document.createTextNode(name.get()));
      preset.appendChild(attribute);
      
      attribute = document.createElement("sub_name");
      attribute.appendChild(document.createTextNode(Data.JOBS.get(sub.get())
                                                             .getName()));
      preset.appendChild(attribute);
      
      attribute = document.createElement("sub_id");
      attribute.appendChild(document.createTextNode(
                                                  Integer.toString(sub.get())));
      preset.appendChild(attribute);
      
      attribute = document.createElement("skills");
      attribute.appendChild(document.createTextNode(getSkillsAsString()));
      preset.appendChild(attribute);
      
      attribute = document.createElement("subclass_skills");
      attribute.appendChild(document.createTextNode(getSubSkillsAsString()));
      preset.appendChild(attribute);
      
      return preset;
    }
  }
  
  public void createPreset(String name, NexusCharacter character) {
    presets.add(new Preset(name, character));
    Data.savePresets();
  }
  
  public Job(String n, int i) {
    name = new SimpleStringProperty(n);
    id = new SimpleIntegerProperty(i);
    skills = FXCollections.observableArrayList();
    presets = FXCollections.observableArrayList();
    stats = new int[131][8];
  }
  
  public Job(NodeList nodes, Document desc, Document statDoc, XPath xpath) {
    name = new SimpleStringProperty();
    id = new SimpleIntegerProperty();
    skills = FXCollections.observableArrayList();
    presets = FXCollections.observableArrayList();
    stats = new int[131][8];
    int len = nodes.getLength();
    Node node;
    for (int i = 0; i < len; i++) {
      node = nodes.item(i);
      switch (node.getNodeName()) {
        case "name":
          name.set(node.getTextContent());
          break;
        case "id":
          id.set(Integer.parseInt(node.getTextContent()));
          break;
        case "skills":
          NodeList sk = node.getChildNodes();
          int slen = sk.getLength();
          for (int j = 0; j < slen; j++) {
            if (sk.item(j).getNodeName().equals("skill")) {
              skills.add(new Skill(sk.item(j).getChildNodes(), desc, xpath));
            }
          }
          break;
      }
    }
    try {
      XPathExpression ex = xpath.compile("/jobs/job/id[text()=\"" +
                                         id.get() + "\"]/../levels");
      Node levelData = (Node) ex.evaluate(statDoc, XPathConstants.NODE);
      if (levelData != null) {
        NodeList levels = levelData.getChildNodes();
        int lvl = 0;
        int llen = levels.getLength();
        for (int i = 0; i < llen; i++) {
          if (levels.item(i).getNodeName().equals("level")) {
            NodeList statNodes = levels.item(i).getChildNodes();
            int slen = statNodes.getLength();
            for (int j = 0; j < slen; j++) {
              Node statNode = statNodes.item(j);
              switch (statNode.getNodeName()) {
                case "level":
                  lvl = Integer.parseInt(statNode.getTextContent());
                  break;
                case "hp":
                  stats[lvl][0] = Integer.parseInt(statNode.getTextContent());
                  break;
                case "tp":
                  stats[lvl][1] = Integer.parseInt(statNode.getTextContent());
                  break;
                case "str":
                  stats[lvl][2] = Integer.parseInt(statNode.getTextContent());
                  break;
                case "vit":
                  stats[lvl][3] = Integer.parseInt(statNode.getTextContent());
                  break;
                case "agi":
                  stats[lvl][4] = Integer.parseInt(statNode.getTextContent());
                  break;
                case "luc":
                  stats[lvl][5] = Integer.parseInt(statNode.getTextContent());
                  break;
                case "int":
                  stats[lvl][6] = Integer.parseInt(statNode.getTextContent());
                  break;
                case "wis":
                  stats[lvl][7] = Integer.parseInt(statNode.getTextContent());
                  break;
              }
            }
          }
        }
      }
    } catch (XPathExpressionException e) {
      e.printStackTrace();
    }
  }
  
  public int statAtLevel(int stat, int level) {
    return stats[level][stat];
  }
  
  public String toString() {
    return name.get();
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
  
  public ObservableList<Skill> getSkills() {
    return skills;
  }
  
  public int numDependents(int id) {
    int n = 0;
    for (Skill skill : skills) {
      for (Skill.Dependency dep : skill.getDependencies()) {
        if (dep.getId() == id) n++;
      }
    }
    return n;
  }
  
  public ObservableList<Preset> getPresets() {
    return presets;
  }
  
  public Node presetsToXML(Document document) {
    Element job = document.createElement("job");
    Element attribute;
    
    attribute = document.createElement("name");
    attribute.appendChild(document.createTextNode(getName()));
    job.appendChild(attribute);
    
    attribute = document.createElement("id");
    attribute.appendChild(document.createTextNode(Integer.toString(getId())));
    job.appendChild(attribute);
    
    attribute = document.createElement("presets");
    for (Preset p : presets) {
      if (p.name.get().isEmpty()) continue;
      attribute.appendChild(p.toXML(document));
    }
    job.appendChild(attribute);
    
    return job;
  }
}
