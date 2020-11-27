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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.github.aabryant.eo.nexus.manager.Data;
import com.mariten.kanatools.KanaConverter;

public class NexusCharacter {
  
  protected final int index;
  protected final StringProperty name;
  protected final StringProperty className;
  protected final IntegerProperty level;
  protected final IntegerProperty exp;
  protected final IntegerProperty sp;
  protected final IntegerProperty bonusSp;
  protected final IntegerProperty job;
  protected final IntegerProperty sub;
  protected final BooleanProperty hasEvilContactLens;
  protected final int[] statBonuses;
  protected final int[] baseStats;
  protected final int[] equipmentBonuses;
  protected ObservableList<Integer> skills;
  protected ObservableList<Integer> subSkills;
  
  public NexusCharacter() {
    index = -1;
    level = new SimpleIntegerProperty(1);
    exp = new SimpleIntegerProperty(0);
    sp = new SimpleIntegerProperty(3);
    bonusSp = new SimpleIntegerProperty(0);
    job = new SimpleIntegerProperty(0xFF);
    sub = new SimpleIntegerProperty(0xFF);
    skills = FXCollections.observableArrayList();
    subSkills = FXCollections.observableArrayList();
    hasEvilContactLens = new SimpleBooleanProperty(false);
    statBonuses = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
    baseStats = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
    equipmentBonuses = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
    for (int i = 0; i < 24; i++) {
      skills.add(0);
      subSkills.add(0);
    }
    name = new SimpleStringProperty("");
    className = new SimpleStringProperty("");
  }
  
  public NexusCharacter(int idx, byte[] bytes) {
    index = idx;
    int start = 0x14C + (0x168 * index);
    level = new SimpleIntegerProperty(0xFF & bytes[start + 0x05]);
    int xp = (0xFF & bytes[start + 0xB0]) |
             ((0xFF & bytes[start + 0xB1]) << 8) |
             ((0xFF & bytes[start + 0xB2]) << 16) |
             ((0xFF & bytes[start + 0xB3]) << 24);
    exp = new SimpleIntegerProperty(xp);
    sp = new SimpleIntegerProperty(0xFF & bytes[start + 0x07]);
    bonusSp = new SimpleIntegerProperty(0xFF & bytes[start + 0x08]);
    job = new SimpleIntegerProperty(bytes[start + 0x09] & 0xFF);
    sub = new SimpleIntegerProperty(bytes[start + 0x0A] & 0xFF);
    skills = FXCollections.observableArrayList();
    subSkills = FXCollections.observableArrayList();
    for (int i = 0; i < 24; i++) {
      skills.add((int) bytes[start + 0xCA + i]);
      subSkills.add((int) bytes[start + 0xE6 + i]);
    }
    name = new SimpleStringProperty();
    if (bytes[start + 0xB4] != 0) {
      int sz;
      for (sz = 0; sz < 18; sz += 2) if (bytes[start + 0xB4 + sz] == 0) break;
      try {
        name.set(KanaConverter.convertKana(new String(bytes, start + 0xB4, sz,
                                                      "SHIFT_JIS"),
                                    KanaConverter.OP_ZEN_LETTER_TO_HAN_LETTER|
                                    KanaConverter.OP_ZEN_SPACE_TO_HAN_SPACE|
                                    KanaConverter.OP_ZEN_NUMBER_TO_HAN_NUMBER));
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
    } else {
      name.set("");
    }
    className = new SimpleStringProperty();
    if (bytes[start + 0x11E] != 0) {
      int sz;
      for (sz = 0; sz < 74; sz += 2) if (bytes[start + 0x11E + sz] == 0) break;
      try {
        className.set(KanaConverter.convertKana(new String(bytes, start + 0x11E,
                                                           sz, "SHIFT_JIS"),
                                    KanaConverter.OP_ZEN_LETTER_TO_HAN_LETTER|
                                    KanaConverter.OP_ZEN_SPACE_TO_HAN_SPACE|
                                    KanaConverter.OP_ZEN_NUMBER_TO_HAN_NUMBER));
      } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
      }
    } else {
      className.set("");
    }
    statBonuses = getStats(bytes, start + 0x20);
    baseStats = getStats(bytes, start + 0x34);
    equipmentBonuses = getStats(bytes, start + 0x5C);
    hasEvilContactLens = new SimpleBooleanProperty(false);
    if (((bytes[start + 0x10] & 0xFF) |
        ((bytes[start + 0x11] & 0xFF) << 8)) == 887 ||
        ((bytes[start + 0x14] & 0xFF) |
        ((bytes[start + 0x15] & 0xFF) << 8)) == 887 ||
        ((bytes[start + 0x18] & 0xFF) |
        ((bytes[start + 0x19] & 0xFF) << 8)) == 887 ||
        ((bytes[start + 0x1C] & 0xFF) |
        ((bytes[start + 0x1D] & 0xFF) << 8)) == 887) {
      hasEvilContactLens.set(true);
      for (int i = 1; i < 8; i++) {
        equipmentBonuses[i] = (int) Math.ceil(equipmentBonuses[i] / 1.1);
      }
    }
    for (int i = 0; i < 8; i++) equipmentBonuses[i] -= baseStats[i];
    for (Skill skill : Data.JOBS.get(job.get()).getSkills()) {
      if (skill.getIndex() >= skills.size()) continue;
      int slvl = skills.get(skill.getIndex());
      if (skill.hpBuff != null) {
        equipmentBonuses[0] -= (int) (baseStats[0] * (skill.hpBuff[slvl] - 1));
      }
      if (skill.tpBuff != null) {
        equipmentBonuses[1] -= (int) (baseStats[1] * (skill.tpBuff[slvl] - 1));
      }
    }
    for (Skill skill : Data.JOBS.get(sub.get()).getSkills()) {
      if (skill.getIndex() >= subSkills.size()) continue;
      int slvl = subSkills.get(skill.getIndex());
      if (skill.hpBuff != null) {
        equipmentBonuses[0] -= (int) (baseStats[0] * (skill.hpBuff[slvl] - 1));
      }
      if (skill.tpBuff != null) {
        equipmentBonuses[1] -= (int) (baseStats[1] * (skill.tpBuff[slvl] - 1));
      }
    }
  }
  
  public int getRetirementLevel() {
    switch (statBonuses[0]) {
      case 3:  return 30;
      case 6:  return 60;
      case 7:  return 70;
      case 8:  return 80;
      case 9:  return 90;
      case 10: return 100;
      case 11: return 110;
      case 12: return 120;
      case 20: return 130;
      default: return 0;
    }
  }
  
  public void updateStats() {
    Job j = Data.JOBS.get(job.get());
    for (int i = 0; i < 8; i++) {
      baseStats[i] = statBonuses[i] + j.statAtLevel(i, level.get());
    }
  }
  
  protected int[] getStats(byte[] bytes, int start) {
    return new int[] {
      (bytes[start] & 0xFF) | ((bytes[start + 1] & 0xFF) << 8) |
      ((bytes[start + 2] & 0xFF) << 16) | ((bytes[start + 3] & 0xFF) << 24),
      
      (bytes[start + 4] & 0xFF) | ((bytes[start + 5] & 0xFF) << 8) |
      ((bytes[start + 6] & 0xFF) << 16) | ((bytes[start + 7] & 0xFF) << 24),
      
      (bytes[start + 8] & 0xFF) | ((bytes[start + 9] & 0xFF) << 8),
      
      (bytes[start + 10] & 0xFF) | ((bytes[start + 11] & 0xFF) << 8),
      
      (bytes[start + 12] & 0xFF) | ((bytes[start + 13] & 0xFF) << 8),
      
      (bytes[start + 14] & 0xFF) | ((bytes[start + 15] & 0xFF) << 8),
      
      (bytes[start + 16] & 0xFF) | ((bytes[start + 17] & 0xFF) << 8),
      
      (bytes[start + 18] & 0xFF) | ((bytes[start + 19] & 0xFF) << 8)
    };
  }
  
  public int getMaxSp() {
    return 2 + bonusSp.get() + level.get() + (sub.get() != 0xFF ? 5 : 0);
  }
  
  public void retire() {
    if (level.get() < 30) return;
    if (level.get() < 60) {
      for (int i = 0; i < 8; i++) statBonuses[i] = 3;
      if (bonusSp.get() < 3) {
        sp.set(sp.get() + 3);
        bonusSp.set(3);
      }
    } else if (level.get() < 70) {
      for (int i = 0; i < 8; i++) statBonuses[i] = 6;
      if (bonusSp.get() < 4) {
        sp.set(sp.get() + 4);
        bonusSp.set(4);
      }
    } else if (level.get() < 80) {
      for (int i = 0; i < 8; i++) statBonuses[i] = 7;
      if (bonusSp.get() < 5) {
        sp.set(sp.get() + 5);
        bonusSp.set(5);
      }
    } else if (level.get() < 90) {
      for (int i = 0; i < 8; i++) statBonuses[i] = 8;
      if (bonusSp.get() < 5) {
        sp.set(sp.get() + 5);
        bonusSp.set(5);
      }
    } else if (level.get() < 100) {
      for (int i = 0; i < 8; i++) statBonuses[i] = 9;
      if (bonusSp.get() < 6) {
        sp.set(sp.get() + 6);
        bonusSp.set(6);
      }
    } else if (level.get() < 110) {
      for (int i = 0; i < 8; i++) statBonuses[i] = 10;
      if (bonusSp.get() < 6) {
        sp.set(sp.get() + 6);
        bonusSp.set(6);
      }
    } else if (level.get() < 120) {
      for (int i = 0; i < 8; i++) statBonuses[i] = 11;
      if (bonusSp.get() < 6) {
        sp.set(sp.get() + 6);
        bonusSp.set(6);
      }
    } else if (level.get() < 130) {
      for (int i = 0; i < 8; i++) statBonuses[i] = 12;
      if (bonusSp.get() < 7) {
        sp.set(sp.get() + 7);
        bonusSp.set(7);
      }
    } else if (level.get() == 130) {
      for (int i = 0; i < 8; i++) statBonuses[i] = 20;
      if (bonusSp.get() < 10) {
        sp.set(sp.get() + 10);
        bonusSp.set(10);
      }
    }
  }
  
  public void resetSp() {
    for (int i = 0; i < 24; i++) {
      skills.set(i, 0);
      subSkills.set(i, 0);
    }
    sp.set(getMaxSp());
  }
  
  public void removeInvalidSkills() {
    Job j = Data.JOBS.get(job.get());
    for (Skill s : j.getSkills().sorted((s1, s2) -> {
          return Integer.compare(s1.getColumn(), s2.getColumn());
         })) {
      if (s.getIndex() < 24 && !s.dependenciesSatisfied(this)) {
        addSkillLevel(s.getIndex(), -skills.get(s.getIndex()));
      }
    }
    if (sub.get() != 0xFF) {
      j = Data.JOBS.get(sub.get());
      for (Skill s : j.getSkills().sorted((s1, s2) -> {
            return Integer.compare(s1.getColumn(), s2.getColumn());
           })) {
        if (s.getIndex() < 24 && !s.dependenciesSatisfied(this, true)) {
          addSubSkillLevel(s.getIndex(), -subSkills.get(s.getIndex()));
        }
      }
    }
  }
  
  public void clearSkills() {
    for (int i = 0; i < 24; i++) addSkillLevel(i, -skills.get(i));
  }
  
  public void clearSubSkills() {
    for (int i = 0; i < 24; i++) addSubSkillLevel(i, -subSkills.get(i));
  }
  
  public void changeSkillLevel(int skill, int value) {
    int old = skills.get(skill);
    if (old == value) return;
    if (value > old && sp.get() == 0) return;
    sp.set(sp.get() - (value - old));
    skills.set(skill, value);
  }
  
  public void addSkillLevel(int skill, int value) {
    if (value == 0) return;
    if (value > 0 && sp.get() == 0) return;
    if (value < 0 && skills.get(skill) == 0) return;
    sp.set(sp.get() - value);
    skills.set(skill, skills.get(skill) + value);
  }
  
  public void changeSubSkillLevel(int skill, int value) {
    int old = subSkills.get(skill);
    if (old == value) return;
    if (value > old && sp.get() == 0) return;
    sp.set(sp.get() - (value - old));
    subSkills.set(skill, value);
  }
  
  public void addSubSkillLevel(int skill, int value) {
    if (value == 0) return;
    if (value > 0 && sp.get() == 0) return;
    if (value < 0 && subSkills.get(skill) == 0) return;
    sp.set(sp.get() - value);
    subSkills.set(skill, subSkills.get(skill) + value);
  }
  
  public void write(byte[] out) {
    updateStats();
    int start = 0x14C + (0x168 * index);
    out[start + 0x05] = (byte) level.get();
    out[start + 0x07] = (byte) sp.get();
    out[start + 0x08] = (byte) bonusSp.get();
    out[start + 0x09] = (byte) job.get();
    out[start + 0x0A] = (byte) sub.get();
    out[start + 0xB0] = (byte) (exp.get() & 0xFF);
    out[start + 0xB1] = (byte) ((exp.get() >> 8) & 0xFF);
    out[start + 0xB2] = (byte) ((exp.get() >> 16) & 0xFF);
    out[start + 0xB3] = (byte) ((exp.get() >> 24) & 0xFF);
    writeStats(out, statBonuses, start + 0x20);
    writeStats(out, baseStats, start + 0x34);
    int[] buffedStats = new int[8];
    for (int i = 0; i < 8; i++) buffedStats[i] = baseStats[i];
    for (Skill skill : Data.JOBS.get(job.get()).getSkills()) {
      if (skill.getIndex() >= skills.size()) continue;
      int slvl = skills.get(skill.getIndex());
      if (skill.hpBuff != null) {
        buffedStats[0] += (int) (baseStats[0] * (skill.hpBuff[slvl] - 1));
      }
      if (skill.tpBuff != null) {
        buffedStats[1] += (int) (baseStats[1] * (skill.tpBuff[slvl] - 1));
      }
    }
    for (Skill skill : Data.JOBS.get(sub.get()).getSkills()) {
      if (skill.getIndex() >= subSkills.size()) continue;
      int slvl = subSkills.get(skill.getIndex());
      if (skill.hpBuff != null) {
        buffedStats[0] += (int) (baseStats[0] * (skill.hpBuff[slvl] - 1));
      }
      if (skill.tpBuff != null) {
        buffedStats[1] += (int) (baseStats[1] * (skill.tpBuff[slvl] - 1));
      }
    }
    writeStats(out, buffedStats, start + 0x48);
    int[] currentStats = new int[8];
    for (int i = 0; i < 8; i++) {
      currentStats[i] = buffedStats[i] + equipmentBonuses[i];
      if (i > 0 && hasEvilContactLens.get()) {
        currentStats[i] = (int) (currentStats[i] * 1.1);
      }
    }
    writeStats(out, currentStats, start + 0x5C);
    writeStats(out, currentStats, start + 0x70);
    writeStats(out, currentStats, start + 0x84);
    for (int i = 0; i < 24; i++) {
      out[start + 0xCA + i] = skills.get(i).byteValue();
      out[start + 0xE6 + i] = subSkills.get(i).byteValue();
    }
    String cName = KanaConverter.convertKana(className.get(),
                                     KanaConverter.OP_HAN_LETTER_TO_ZEN_LETTER|
                                     KanaConverter.OP_HAN_SPACE_TO_ZEN_SPACE|
                                     KanaConverter.OP_HAN_NUMBER_TO_ZEN_NUMBER);
    byte[] cNameBytes = cName.getBytes(Charset.forName("SHIFT-JIS"));
    int i;
    for (i = 0; i < cNameBytes.length; i++) {
      out[start + 0x11E + i] = cNameBytes[i];
    }
    for (; i < 74; i++) {
      out[start + 0x11E + i] = 0;
    }
  }
  
  protected void writeStats(byte[] bytes, int[] stats, int start) {
    bytes[start] = (byte) stats[0];
    bytes[start + 1] = (byte) (stats[0] >> 8);
    bytes[start + 2] = (byte) (stats[0] >> 16);
    bytes[start + 3] = (byte) (stats[0] >> 24);
    bytes[start + 4] = (byte) stats[1];
    bytes[start + 5] = (byte) (stats[1] >> 8);
    bytes[start + 6] = (byte) (stats[1] >> 16);
    bytes[start + 7] = (byte) (stats[1] >> 24);
    bytes[start + 8] = (byte) stats[2];
    bytes[start + 9] = (byte) (stats[2] >> 8);
    bytes[start + 10] = (byte) stats[3];
    bytes[start + 11] = (byte) (stats[3] >> 8);
    bytes[start + 12] = (byte) stats[4];
    bytes[start + 13] = (byte) (stats[4] >> 8);
    bytes[start + 14] = (byte) stats[5];
    bytes[start + 15] = (byte) (stats[5] >> 8);
    bytes[start + 16] = (byte) stats[6];
    bytes[start + 17] = (byte) (stats[6] >> 8);
    bytes[start + 18] = (byte) stats[7];
    bytes[start + 19] = (byte) (stats[7] >> 8);
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
  
  public StringProperty classNameProperty() {
    return className;
  }
  
  public String getClassName() {
    return className.get();
  }
  
  public void setClassName(String value) {
    className.set(value);
  }

  public IntegerProperty levelProperty() {
    return level;
  }

  public int getLevel() {
    return level.get();
  }

  public void setLevel(int value) {
    setSp(sp.get() + (value - level.get()));
    level.set(value);
    updateStats();
  }
  
  public IntegerProperty expProperty() {
    return exp;
  }

  public int getExp() {
    return exp.get();
  }

  public void setExp(int value) {
    exp.set(value);
  }

  public IntegerProperty spProperty() {
    return sp;
  }

  public int getSp() {
    return sp.get();
  }

  public void setSp(int value) {
    sp.set(value);
  }

  public IntegerProperty bonusSpProperty() {
    return bonusSp;
  }

  public int getBonusSp() {
    return bonusSp.get();
  }

  public void setBonusSp(int value) {
    bonusSp.set(value);
  }

  public IntegerProperty jobProperty() {
    return job;
  }

  public int getJob() {
    return job.get();
  }

  public void setJob(int value) {
    job.set(value);
  }

  public IntegerProperty subProperty() {
    return sub;
  }

  public int getSub() {
    return sub.get();
  }

  public void setSub(int value) {
    sub.set(value);
  }
  
  public ObservableList<Integer> getSkills() {
    return skills;
  }
  
  public ObservableList<Integer> getSubSkills() {
    return subSkills;
  }
}
