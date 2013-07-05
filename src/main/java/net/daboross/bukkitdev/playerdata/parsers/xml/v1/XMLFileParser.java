/*
 * Author: Dabo Ross
 * Website: www.daboross.net
 * Email: daboross@daboross.net
 */
package net.daboross.bukkitdev.playerdata.parsers.xml.v1;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import net.daboross.bukkitdev.playerdata.LoginDataImpl;
import net.daboross.bukkitdev.playerdata.PlayerDataImpl;
import net.daboross.bukkitdev.playerdata.PlayerDataStatic;
import net.daboross.bukkitdev.playerdata.libraries.dxml.DXMLException;
import net.daboross.bukkitdev.playerdata.libraries.dxml.DXMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import static net.daboross.bukkitdev.playerdata.libraries.dxml.DXMLHelper.createElement;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * @author daboross
 */
public class XMLFileParser {

    public static void writeToFile(PlayerDataImpl pd, File fileResult) throws DXMLException {
        Document document = DXMLHelper.newDocument();
        Element root = document.createElement("playerdata");
        document.appendChild(root);
        root.appendChild(createElement(document, "username", pd.getUsername()));
        root.appendChild(createElement(document, "displayname", pd.getDisplayname()));
        root.appendChild(createElement(document, "timeplayed", String.valueOf(pd.getTimePlayed())));
        {
            List<LoginDataImpl> logins = pd.getAllLogins();
            Element logInsElement = document.createElement("logins");
            for (int i = 0; i < logins.size(); i++) {
                Element e = document.createElement("login" + i);
                LoginDataParser.putDataOnXML(logins.get(i), e);
                logInsElement.appendChild(e);
            }
            root.appendChild(logInsElement);
        }
        {
            List<Long> logouts = pd.getAllLogouts();
            Element logOutsElement = document.createElement("logouts");
            for (int i = 0; i < logouts.size(); i++) {
                logOutsElement.appendChild(createElement(document, String.valueOf("logout" + i), logouts.get(i).toString()));
            }
            root.appendChild(logOutsElement);
        }
        {
            Element otherData = document.createElement("data");
            for (String dataName : pd.getExtraDataNames()) {
                Element e = document.createElement(dataName);
                ExtraDataParser.putOnXML(dataName, pd.getExtraData(dataName), e);
                otherData.appendChild(e);
            }
            root.appendChild(otherData);
        }
        DXMLHelper.writeXML(document, fileResult);
    }

    public static PlayerDataImpl readFromFile(File fl) throws DXMLException {
        Document d = DXMLHelper.readDocument(fl);
        Node root = d.getFirstChild();
        if (!root.getNodeName().equals("playerdata")) {
            throw new DXMLException("File Given Isn't PlayerData File");
        }
        if (!root.hasChildNodes()) {
            throw new DXMLException("Document Root Doesn't Have Child Nodes");
        }
        NodeList list = root.getChildNodes();
        String username = null;
        String displayname = null;
        String timePlayed = null;
        Node logOuts = null;
        Node logIns = null;
        Node data = null;
        for (int i = 0; i < list.getLength(); i++) {
            Node current = list.item(i);
            if (current.getNodeName().equals("username")) {
                username = current.getFirstChild().getNodeValue();
            } else if (current.getNodeName().equals("displayname")) {
                displayname = current.getFirstChild().getNodeValue();
            } else if (current.getNodeName().equals("timeplayed")) {
                timePlayed = current.getFirstChild().getNodeValue();
            } else if (current.getNodeName().equals("logins")) {
                logIns = current;
            } else if (current.getNodeName().equals("logouts")) {
                logOuts = current;
            } else if (current.getNodeName().equals("data")) {
                data = current;
            } else if (!current.getNodeName().equals("#text")) {
                throw new DXMLException("Field:" + current.getNodeName());
            }
        }
        if (logOuts == null || logIns == null || username == null || displayname == null || timePlayed == null || data == null) {
            throw new DXMLException("Doesn't Contain All Fields user:" + username + " display:" + displayname + " time:" + timePlayed + " data:" + data + " logins:" + logIns + " logouts:" + logOuts);
        }
        NodeList logOutList = logOuts.getChildNodes();
        ArrayList<Long> logoutsFinal = new ArrayList<Long>(logOutList.getLength());
        NodeList logInList = logIns.getChildNodes();
        ArrayList<LoginDataImpl> loginsFinal = new ArrayList<LoginDataImpl>(logInList.getLength());
        for (int i = 0; i < logOutList.getLength(); i++) {
            Node current = logOutList.item(i);
            if (current.getNodeName().equals("#text")) {
                continue;
            }
            Node child = current.getFirstChild();
            if (child == null) {
                PlayerDataStatic.getLogger().log(Level.WARNING, "Invalid Logout: User:{0}", username);
                continue;
            }
            try {
                logoutsFinal.add(Long.valueOf(child.getNodeValue()));
            } catch (NumberFormatException nfe) {
                PlayerDataStatic.getLogger().log(Level.WARNING, "Invalid Logout: User:{0}", username);
            }
        }
        for (int i = 0; i < logInList.getLength(); i++) {
            Node current = logInList.item(i);
            if (current.getNodeName().equals("#text")) {
                continue;
            }
            try {
                loginsFinal.add(LoginDataParser.fromXML(current));
            } catch (DXMLException dxmle) {
                PlayerDataStatic.getLogger().log(Level.WARNING, "Invalid Login: User:{0}", username);
            }
        }
        NodeList dataList = data.getChildNodes();
        Map<String, String[]> extraData = new HashMap<String, String[]>();
        for (int i = 0; i < dataList.getLength(); i++) {
            Node current = dataList.item(i);
            if (current.getNodeName().equals("#text")) {
                continue;
            }
            extraData.put(ExtraDataParser.getNameFromXML(current), ExtraDataParser.getDataFromXML(current));
        }
        long timePlayedLong = 0;
        try {
            timePlayedLong = Long.parseLong(timePlayed);
        } catch (NumberFormatException nfe) {
            PlayerDataStatic.getLogger().log(Level.WARNING, "Invalid TimePlayed: User:{0}", username);
        }
        return new PlayerDataImpl(username, displayname, loginsFinal, logoutsFinal, timePlayedLong, extraData);
    }
}