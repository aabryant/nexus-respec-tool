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
package com.github.aabryant.eo.nexus.manager;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.github.aabryant.eo.nexus.model.Job;
import com.github.aabryant.eo.nexus.model.NexusCharacter;

public final class Data {
  
  public static String SAVE_PATH;
  public static byte[] SAVE_BYTES;
  public static ObservableList<NexusCharacter> CHARACTERS;
  public static final ObservableMap<Integer,Job> JOBS =
                                              FXCollections.observableHashMap();
  public static final ObservableList<Job> JOB_LIST =
                                            FXCollections.observableArrayList();
  
  public static boolean loadSave(String path) {
    try {
      byte[] bytes = Files.readAllBytes(Paths.get(path));
      if (!isNexusSave(bytes)) return false;
      SAVE_PATH = path;
      SAVE_BYTES = bytes;
      CHARACTERS = FXCollections.observableArrayList();
      for (int i = 0; i < 60; i++) CHARACTERS.add(new NexusCharacter(i, bytes));
      return true;
    } catch (IOException e) {
      e.printStackTrace();
      return false;
    }
  }
  
  public static boolean isNexusSave(byte[] bytes) {
    try {
      String magic = new String(bytes, 0, 8, "SHIFT_JIS");
      return magic.equals("MOS_GAME") || magic.equals("MOS_CONT"); 
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
      return false;
    }
  }
  
  public static void saveFile() {
    try {
      for (NexusCharacter c : CHARACTERS) c.write(SAVE_BYTES);
      Files.write(Paths.get(SAVE_PATH), SAVE_BYTES);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public static void saveFile(String path) {
    try {
      SAVE_PATH = path;
      for (NexusCharacter c : CHARACTERS) c.write(SAVE_BYTES);
      Files.write(Paths.get(SAVE_PATH), SAVE_BYTES);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  public static void load() {
    try (InputStream is = Data.class.getResourceAsStream("/xml/job_data.xml");
         InputStream is2 =
                  Data.class.getResourceAsStream("/xml/skill_descriptions.xml");
         InputStream is3 = Data.class.getResourceAsStream("/xml/stats.xml")) {
      new File("data").mkdirs();
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document jobData = builder.parse(is);
      Document skillDescriptions = builder.parse(is2);
      Document stats = builder.parse(is3);
      NodeList jobNodes = jobData.getElementsByTagName("job");
      XPath xpath = XPathFactory.newInstance().newXPath();
      int len = jobNodes.getLength();
      for (int i = 0; i < len; i++) {
        Job job = new Job(jobNodes.item(i).getChildNodes(), skillDescriptions,
                          stats, xpath);
        JOBS.put(job.getId(), job);
        JOB_LIST.add(job);
      }
      loadPresets();
    } catch (ParserConfigurationException | SAXException | IOException e) {
      e.printStackTrace();
    }
  }
  
  public static void loadPresets() {
    if (!new File("data/presets.xml").exists()) return;
    try (FileInputStream is = new FileInputStream("data/presets.xml")) {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document jobData = builder.parse(is);
      NodeList jobNodes = jobData.getElementsByTagName("job");
      int len = jobNodes.getLength();
      Node node;
      int jid = 0;
      for (int i = 0; i < len; i++) {
        NodeList nodes = jobNodes.item(i).getChildNodes();
        int nlen = nodes.getLength();
        for (int j = 0; j < nlen; j++) {
          node = nodes.item(j);
          switch (node.getNodeName()) {
            case "id":
              jid = Integer.parseInt(node.getTextContent());
              break;
            case "presets":
              NodeList p = node.getChildNodes();
              int plen = p.getLength();
              for (int k = 0; k < plen; k++) {
                if (p.item(k).getNodeName().equals("preset")) {
                  JOBS.get(jid).getPresets().add(new Job.Preset(
                                                    p.item(k).getChildNodes()));
                }
              }
              break;
          }
        }
      }
    } catch (ParserConfigurationException | SAXException | IOException e) {
      e.printStackTrace();
    }
  }
  
  public static void savePresets() {
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.newDocument();
      Element root = doc.createElement("jobs");
      JOBS.values().forEach((job) -> root.appendChild(job.presetsToXML(doc)));
      doc.appendChild(root);
      TransformerFactory transformerFactory = TransformerFactory.newInstance();
      Transformer transformer = transformerFactory.newTransformer();
      transformer.setOutputProperty(OutputKeys.INDENT, "yes");
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount",
                                    "2");
      DOMSource source = new DOMSource(doc);
      if (new File("data/presets.xml").exists()) {
        Files.move(Paths.get("data/presets.xml"),
                   Paths.get("data/presets.xml.bak"),
                   StandardCopyOption.REPLACE_EXISTING);
      }
      StreamResult file = new StreamResult(new File("data/presets.xml"));
      transformer.transform(source, file);
      if (new File("data/presets.xml.bak").exists()) {
        Files.delete(Paths.get("data/presets.xml.bak"));
      }
    } catch (IOException | TransformerException |
             ParserConfigurationException e) {
      e.printStackTrace();
      if (new File("data/presets.xml.bak").exists()) {
        try {
          Files.move(Paths.get("data/presets.xml.bak"),
                     Paths.get("data/presets.xml"),
                      StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
          e.printStackTrace();
        }
      }
    }
  }
}
