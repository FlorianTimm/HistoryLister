package de.hamburg.gv.s2.historyLister;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.util.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import de.hamburg.gv.s2.Abschnitt;
import de.hamburg.gv.s2.ChangeSet;
import de.hamburg.gv.s2.ChangeSetDB;
import de.hamburg.gv.s2.Netzknoten;
import de.hamburg.gv.s2.Station;

public class HistoryListerK implements Runnable {
	private Connection con;
	private ChangeSetDB changes;
	private java.sql.Date bis;
	private java.sql.Date von;
	private HistoryStringListener schnittstelle;

	public HistoryListerK(HistoryStringListener schnittstelle) {
		this.schnittstelle = schnittstelle;
		changes = new ChangeSetDB();
	}

	public boolean connect(String database, String user, String passwort) {
		try {
			Class.forName("oracle.jdbc.driver.OracleDriver");

			con = DriverManager.getConnection("jdbc:oracle:thin:@//" + database, user, passwort);
			return true;
		} catch (ClassNotFoundException e) {
			log("ClassNotFoundException: " + e.getMessage());
		} catch (SQLException e) {
			// jt1.append("SQLException: "+e.getMessage());
		} catch (Exception e) {
			// jt1.append("Exception: "+e+e.getMessage());
		}
		return false;
	}

	public void setInterval(Date von, Date bis) {
		this.von = java.sql.Date.valueOf(von.toString());
		this.bis = java.sql.Date.valueOf(bis.toString());
	}

	private void getData() {
		try {
			log("Bereite Abfrage vor...");
			PreparedStatement ppst = con.prepareStatement(
					"SELECT OPNAME, OPARGS FROM SYSADM5.NET_PROT WHERE STAND BETWEEN ? AND ? "
							+ "AND OPNAME IN ('teilAbschnittEinfuegen', 'teilAbschnittLoeschen', 'abschnitteVerbinden', "
							+ "'abschnittTrennen', 'abschnittDrehen', 'abschnittAnlegen', 'abschnittUmbenennen', "
							+ "'abschnittZurueckbauen') AND PROJEKTNR IS NOT NULL ORDER BY LFD ASC",
					ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
			ppst.setDate(1, von);
			ppst.setDate(2, bis);
			log("Abfrage läuft...");
			ResultSet rs = ppst.executeQuery();

			rs.last();
			schnittstelle.setRowCount(rs.getRow());
			rs.beforeFirst();

			log("Bearbeitung läuft...");

			while (rs.next()) {
				schnittstelle.setRow(rs.getRow());
				String opargs = rs.getString(2);
				// log(opargs);
				Document xml = parseXML(opargs);
				switch (rs.getString(1)) {
				case "teilAbschnittEinfuegen":
					parseTeilAbschnittEinfuegen(xml);
					break;
				case "teilAbschnittLoeschen":
					parseTeilAbschnittLoeschen(xml);
					break;
				case "abschnitteVerbinden":
					parseAbschnitteVerbinden(xml);
					break;
				case "abschnittTrennen":
					parseAbschnitteTrennen(xml);
					break;
				case "abschnittDrehen":
					parseAbschnittDrehen(xml);
					break;
				case "abschnittUmbennen":
					parseAbschnittUmbenennen(xml);
					break;
				case "abschnittAnlegen":
					parseAbschnittAnlegen(xml);
					break;
				case "abschnittZurueckbauen":
					parseAbschnittZurueckbauen(xml);
				}
			}

			log("Daten werden sortiert");
			changes.sort();
			
			schnittstelle.clean();

			for (ChangeSet cs : changes.getAll()) {
				log(cs.toString("\t"));
			}

		} catch (SQLException e1) {
			log(e1.getMessage());
			e1.printStackTrace();
		}
	}

	private String getAttribut(Document xml, String attribute) {
		return getAttribut(xml, attribute, 0);
	}

	private String getAttribut(Document xml, String attribute, int i) {
		Node eintrag = xml.getElementsByTagName(attribute).item(i);
		String inhalt = eintrag.getTextContent();
		// log(inhalt);
		return inhalt;
	}

	public static int toInt(String zahlAlsText) {
		try {
			int zahl = Integer.parseInt(zahlAlsText);
			// log(zahl);
			return zahl;
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private Abschnitt parseXML2Abschnitt(Document xml) {
		return parseXML2Abschnitt(xml, 0);
	}

	private Abschnitt parseXML2Abschnitt(Document xml, int i) {
		Abschnitt abs = new Abschnitt();

		String kartenblatt = getAttribut(xml, "vonKartenblatt", i);
		String lfdNr = getAttribut(xml, "vonNkLfd", i);
		String zusatz = getAttribut(xml, "vonZusatz", i);
		abs.setVNK(new Netzknoten(kartenblatt, lfdNr, zusatz));

		kartenblatt = getAttribut(xml, "nachKartenblatt", i);
		lfdNr = getAttribut(xml, "nachNkLfd", i);
		zusatz = getAttribut(xml, "nachZusatz", i);
		abs.setNNK(new Netzknoten(kartenblatt, lfdNr, zusatz));

		abs.setLEN(toInt(getAttribut(xml, "len", i)));
		return abs;
	}

	private void parseAbschnittUmbenennen(Document xml) {
		log("parseAbschnittUmbennen");
		// <object-array>
		// <de.novasib.ttsib5.interfaceTypes.NatIdProjekt/>
		// <de.novasib.ttsib5.interfaceTypes.NatIdAbschnittExt2>
		// <vonKartenblatt>2525</vonKartenblatt>
		// <vonNkLfd>12383</vonNkLfd>
		// <vonZusatz>O</vonZusatz>
		// <nachKartenblatt>2525</nachKartenblatt>
		// <nachNkLfd>12384</nachNkLfd>
		// <nachZusatz>O</nachZusatz>
		// <len>85</len>
		// <strasse>G 3581</strasse>
		// </de.novasib.ttsib5.interfaceTypes.NatIdAbschnittExt2>
		// <de.novasib.ttsib5.interfaceTypes.NatIdNullpunkt>
		// <kartenblatt>2525</kartenblatt>
		// <nkLfd>12383</nkLfd>
		// <zusatz>O</zusatz>
		// </de.novasib.ttsib5.interfaceTypes.NatIdNullpunkt>
		// <de.novasib.ttsib5.interfaceTypes.NatIdNullpunkt>
		// <kartenblatt>2525</kartenblatt>
		// <nkLfd>10871</nkLfd>
		// <zusatz>O</zusatz>
		// </de.novasib.ttsib5.interfaceTypes.NatIdNullpunkt>
		// <de.novasib.ttsib5.interfaceTypes.NatIdNetProtHolder/>
		// </object-array>
		Abschnitt abs = parseXML2Abschnitt(xml);
		ChangeSet[] cs = new ChangeSet[1];
		cs[0] = new ChangeSet();
		cs[0].setAlt(new Station(abs));

		String kartenblatt0 = getAttribut(xml, "kartenblatt", 0);
		String lfdNr0 = getAttribut(xml, "nkLfd", 0);
		String zusatz0 = getAttribut(xml, "zusatz", 0);
		Netzknoten vnk = new Netzknoten(kartenblatt0, lfdNr0, zusatz0);

		String kartenblatt1 = getAttribut(xml, "kartenblatt", 1);
		String lfdNr1 = getAttribut(xml, "nkLfd", 1);
		String zusatz1 = getAttribut(xml, "zusatz", 1);
		Netzknoten nnk = new Netzknoten(kartenblatt1, lfdNr1, zusatz1);

		Abschnitt neu = new Abschnitt(vnk, nnk, abs.getLEN());
		cs[0].setNeu(new Station(neu));
		// cs.setGedreht(true);

		log(cs);
		changes.add(cs);
	}

	private void parseAbschnittZurueckbauen(Document xml) {
		log("parseAbschnittZurueckbauen");
		// <object-array>
		// <de.novasib.ttsib5.interfaceTypes.NatIdProjekt/>
		// <de.novasib.ttsib5.interfaceTypes.NatIdAbschnittExt2>
		// <vonKartenblatt>2326</vonKartenblatt>
		// <vonNkLfd>19175</vonNkLfd>
		// <vonZusatz>O</vonZusatz>
		// <nachKartenblatt>2326</nachKartenblatt>
		// <nachNkLfd>19176</nachNkLfd>
		// <nachZusatz>O</nachZusatz>
		// <len>17</len>
		// <strasse>G 2092</strasse>
		// </de.novasib.ttsib5.interfaceTypes.NatIdAbschnittExt2>
		// <de.novasib.ttsib5.interfaceTypes.DumstufungType>
		// <rbGrund>R</rbGrund>
		// <rechtguelt>2018-06-04 22:00:00.0 UTC</rechtguelt>
		// <aktenzeichen></aktenzeichen>
		// <bemerkung>(G 2092): Rückbau e. Teilabs.</bemerkung>
		// </de.novasib.ttsib5.interfaceTypes.DumstufungType>
		// <de.novasib.ttsib5.interfaceTypes.NatIdNetProtHolder/>
		// </object-array>
		ChangeSet[] cs = new ChangeSet[1];

		Abschnitt abs = parseXML2Abschnitt(xml);
		cs[0] = new ChangeSet();
		cs[0].setAlt(new Station(abs));

		log(cs);
		changes.add(cs);
	}

	private void parseAbschnittAnlegen(Document xml) {
		log("parseAbschnittAnlegen");
		// <object-array>
		// <de.novasib.ttsib5.interfaceTypes.NatIdProjekt/>
		// <de.novasib.ttsib5.interfaceTypes.NatIdNullpunkt>
		// <kartenblatt>2525</kartenblatt>
		// <nkLfd>11571</nkLfd>
		// <zusatz>O</zusatz>
		// </de.novasib.ttsib5.interfaceTypes.NatIdNullpunkt>
		// <de.novasib.ttsib5.interfaceTypes.NatIdNullpunkt>
		// <kartenblatt>2525</kartenblatt>
		// <nkLfd>12377</nkLfd>
		// <zusatz>O</zusatz>
		// </de.novasib.ttsib5.interfaceTypes.NatIdNullpunkt>
		// <int>113</int>
		// <string>020000</string>
		// <string>0</string>
		// <string></string>
		// <de.novasib.ttsib5.interfaceTypes.DumstufungType>
		// <rbGrund></rbGrund>
		// <rechtguelt>2018-04-23 22:00:00.0 UTC</rechtguelt>
		// <aktenzeichen></aktenzeichen>
		// <bemerkung></bemerkung>
		// </de.novasib.ttsib5.interfaceTypes.DumstufungType>
		// <string>c4fc78052a364c648c0001632b2d7f99</string>
		// <de.novasib.ttsib5.interfaceTypes.NatIdNetProtHolder/>
		// </object-array>

		String kartenblatt0 = getAttribut(xml, "kartenblatt", 0);
		String lfdNr0 = getAttribut(xml, "nkLfd", 0);
		String zusatz0 = getAttribut(xml, "zusatz", 0);
		Netzknoten nk0 = new Netzknoten(kartenblatt0, lfdNr0, zusatz0);

		String kartenblatt1 = getAttribut(xml, "kartenblatt", 1);
		String lfdNr1 = getAttribut(xml, "nkLfd", 1);
		String zusatz1 = getAttribut(xml, "zusatz", 1);
		Netzknoten nk1 = new Netzknoten(kartenblatt1, lfdNr1, zusatz1);

		int len = toInt(getAttribut(xml, "int"));

		Abschnitt abs = new Abschnitt(nk0, nk1, len);

		ChangeSet[] cs = new ChangeSet[1];
		cs[0] = new ChangeSet();
		cs[0].setNeu(new Station(abs));

		log(cs);
		changes.add(cs);
		System.out.println(cs.toString());
	}

	private void parseAbschnittDrehen(Document xml) {
		log("parseAbschnittDrehen()");
		// <object-array>
		// <de.novasib.ttsib5.interfaceTypes.NatIdProjekt/>
		// <de.novasib.ttsib5.interfaceTypes.NatIdAbschnittExt2>
		// <vonKartenblatt>2425</vonKartenblatt>
		// <vonNkLfd>17361</vonNkLfd>
		// <vonZusatz>O</vonZusatz>
		// <nachKartenblatt>2425</nachKartenblatt>
		// <nachNkLfd>17362</nachNkLfd>
		// <nachZusatz>O</nachZusatz>
		// <len>102</len>
		// <strasse>G 289 A</strasse>
		// </de.novasib.ttsib5.interfaceTypes.NatIdAbschnittExt2>
		// <de.novasib.ttsib5.interfaceTypes.NatIdNetProtHolder/>
		// </object-array>
		ChangeSet[] cs = new ChangeSet[1];

		Abschnitt abs = parseXML2Abschnitt(xml);
		Abschnitt dreh = abs.clone();
		dreh.drehen();
		cs[0] = new ChangeSet(new Station(abs), new Station(dreh), true);
		// cs.setGedreht(true);

		log(cs);
		changes.add(cs);

	}

	private void parseAbschnitteTrennen(Document xml) {
		log("parseAbschnitteTrennen()");
		// <object-array>
		// <de.novasib.ttsib5.interfaceTypes.NatIdProjekt/>
		// <de.novasib.ttsib5.interfaceTypes.NatIdAbschnittExt2>
		// <vonKartenblatt>2425</vonKartenblatt>
		// <vonNkLfd>17359</vonNkLfd>
		// <vonZusatz>O</vonZusatz>
		// <nachKartenblatt>2425</nachKartenblatt>
		// <nachNkLfd>17360</nachNkLfd>
		// <nachZusatz>O</nachZusatz>
		// <len>1156</len>
		// <strasse>G 289 A</strasse>
		// </de.novasib.ttsib5.interfaceTypes.NatIdAbschnittExt2>
		// <de.novasib.ttsib5.interfaceTypes.NatIdNullpunkt>
		// <kartenblatt>2425</kartenblatt>
		// <nkLfd>17362</nkLfd>
		// <zusatz>O</zusatz>
		// </de.novasib.ttsib5.interfaceTypes.NatIdNullpunkt>
		// <int>740</int>
		// <string>778992afc1184123a746b51649294207</string>
		// <string>3fd00eb8f3d54c9da56878fd674fff7e</string>
		// <de.novasib.ttsib5.interfaceTypes.NatIdNetProtHolder/>
		// </object-array>

		Abschnitt abs = parseXML2Abschnitt(xml);
		int st = toInt(getAttribut(xml, "int"));
		String kartenblatt = getAttribut(xml, "kartenblatt");
		String lfdNr = getAttribut(xml, "nkLfd");
		String zusatz = getAttribut(xml, "zusatz");
		Netzknoten nk = new Netzknoten(kartenblatt, lfdNr, zusatz);

		ChangeSet[] cs = new ChangeSet[2];

		cs[0] = new ChangeSet();
		cs[0].setAlt(new Station(abs, 0, st));
		cs[0].setNeu(new Station(new Abschnitt(abs.getVNK(), nk), 0, st));

		cs[1] = new ChangeSet();
		cs[1].setAlt(new Station(abs, st, abs.getLEN()));
		cs[1].setNeu(new Station(new Abschnitt(nk, abs.getNNK()), 0, abs.getLEN() - st));

		log(cs);
		changes.add(cs);
	}

	private void parseAbschnitteVerbinden(Document xml) {
		log("parseAbschnitteVerbinden()");
		// <object-array>
		// <de.novasib.ttsib5.interfaceTypes.NatIdProjekt/>
		// <de.novasib.ttsib5.interfaceTypes.NatIdAbschnittExt2>
		// <vonKartenblatt>2326</vonKartenblatt>
		// <vonNkLfd>17592</vonNkLfd>
		// <vonZusatz>O</vonZusatz>
		// <nachKartenblatt>2326</nachKartenblatt>
		// <nachNkLfd>17591</nachNkLfd>
		// <nachZusatz>O</nachZusatz>
		// <len>119</len>
		// <strasse>G 210</strasse>
		// </de.novasib.ttsib5.interfaceTypes.NatIdAbschnittExt2>
		// <de.novasib.ttsib5.interfaceTypes.NatIdAbschnittExt2>
		// <vonKartenblatt>2326</vonKartenblatt>
		// <vonNkLfd>17591</vonNkLfd>
		// <vonZusatz>O</vonZusatz>
		// <nachKartenblatt>2326</nachKartenblatt>
		// <nachNkLfd>17590</nachNkLfd>
		// <nachZusatz>O</nachZusatz>
		// <len>172</len>
		// <strasse>G 210</strasse>
		// </de.novasib.ttsib5.interfaceTypes.NatIdAbschnittExt2>
		// <de.novasib.ttsib5.interfaceTypes.NatIdNetProtHolder/>
		// </object-array>
		Abschnitt abs1 = parseXML2Abschnitt(xml, 0);
		Abschnitt abs2 = parseXML2Abschnitt(xml, 1);

		Abschnitt neu = new Abschnitt(abs1.getVNK(), abs2.getNNK());
		neu.setLEN(abs1.getLEN() + abs2.getLEN());

		ChangeSet[] cs = new ChangeSet[2];

		cs[0] = new ChangeSet();
		cs[0].setAlt(new Station(abs1));
		cs[0].setNeu(new Station(neu, 0, abs1.getLEN()));

		cs[1] = new ChangeSet();
		cs[1].setAlt(new Station(abs2));
		cs[1].setNeu(new Station(neu, abs1.getLEN(), neu.getLEN()));

		log(cs);
		changes.add(cs);
	}

	private void parseTeilAbschnittEinfuegen(Document xml) {
		log("parseTeilAbschnitteEinfuegen()");
		// log(opargs);

		/*
		 * <object-array> <de.novasib.ttsib5.interfaceTypes.NatIdProjekt/>
		 * <de.novasib.ttsib5.interfaceTypes.NatIdAbschnittExt2>
		 * <vonKartenblatt>2325</vonKartenblatt> <vonNkLfd>12206</vonNkLfd>
		 * <vonZusatz>O</vonZusatz> <nachKartenblatt>2325</nachKartenblatt>
		 * <nachNkLfd>12209</nachNkLfd> <nachZusatz>O</nachZusatz> <len>60</len>
		 * <strasse>G 2414</strasse>
		 * </de.novasib.ttsib5.interfaceTypes.NatIdAbschnittExt2> <int>59</int> Station
		 * <int>17</int> hinzugekommen <de.novasib.ttsib5.interfaceTypes.DumstufungType>
		 * <rbGrund></rbGrund> <rechtguelt>2018-06-06 22:00:00.0 UTC</rechtguelt>
		 * <aktenzeichen></aktenzeichen> <bemerkung></bemerkung>
		 * </de.novasib.ttsib5.interfaceTypes.DumstufungType>
		 * <de.novasib.ttsib5.interfaceTypes.NatIdNetProtHolder/> </object-array>
		 */

		Abschnitt abs = parseXML2Abschnitt(xml);

		int st = toInt(getAttribut(xml, "int"));
		int add = toInt(getAttribut(xml, "int", 1));

		Abschnitt neu = new Abschnitt(abs);
		neu.setLEN(neu.getLEN() + add);

		ChangeSet[] cs = new ChangeSet[3];

		cs[0] = new ChangeSet();
		cs[0].setAlt(new Station(abs, 0, st));
		cs[0].setNeu(new Station(neu, 0, st));

		cs[1] = new ChangeSet();
		cs[1].setNeu(new Station(neu, st, st + add));

		cs[2] = new ChangeSet();
		cs[2].setAlt(new Station(abs, st, abs.getLEN()));
		cs[2].setNeu(new Station(neu, st + add, abs.getLEN() + add));

		log(cs);
		changes.add(cs);
	}

	private void parseTeilAbschnittLoeschen(Document xml) {
		log("parseTeilAbschnitteLoeschen()");

		/*
		 * <object-array> <de.novasib.ttsib5.interfaceTypes.NatIdProjekt>
		 * <projektNr>201802000000292</projektNr>
		 * </de.novasib.ttsib5.interfaceTypes.NatIdProjekt>
		 * <de.novasib.ttsib5.interfaceTypes.NatIdAbschnittExt2>
		 * <vonKartenblatt>2525</vonKartenblatt> <vonNkLfd>12375</vonNkLfd>
		 * <vonZusatz>O</vonZusatz> <nachKartenblatt>2525</nachKartenblatt>
		 * <nachNkLfd>12376</nachNkLfd> <nachZusatz>O</nachZusatz> <len>41</len>
		 * <strasse>G 3581</strasse>
		 * </de.novasib.ttsib5.interfaceTypes.NatIdAbschnittExt2> <int>1</int>
		 * <int>8</int> <de.novasib.ttsib5.interfaceTypes.DumstufungType>
		 * <rbGrund></rbGrund> <rechtguelt>2018-06-07 22:00:00.0 UTC</rechtguelt>
		 * <aktenzeichen></aktenzeichen> <bemerkung></bemerkung>
		 * </de.novasib.ttsib5.interfaceTypes.DumstufungType>
		 * <de.novasib.ttsib5.interfaceTypes.NatIdNetProtHolder/> </object-array>
		 */

		Abschnitt abs = parseXML2Abschnitt(xml);

		int st = toInt(getAttribut(xml, "int", 0));
		int sub = toInt(getAttribut(xml, "int", 1));

		Abschnitt neu = new Abschnitt(abs);
		neu.setLEN(abs.getLEN() - sub);

		ChangeSet[] cs = new ChangeSet[3];

		cs[0] = new ChangeSet();
		cs[0].setAlt(new Station(abs, 0, st));
		cs[0].setNeu(new Station(neu, 0, st));

		cs[1] = new ChangeSet();
		cs[1].setAlt(new Station(abs, st, st + sub));

		cs[2] = new ChangeSet();
		cs[2].setAlt(new Station(abs, st + sub, abs.getLEN()));
		cs[2].setNeu(new Station(neu, st, abs.getLEN() - sub));

		log(cs);
		changes.add(cs);
	}

	private Document parseXML(String opargs) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			StringBuilder xmlStringBuilder = new StringBuilder();
			xmlStringBuilder.append(opargs);
			ByteArrayInputStream input;

			input = new ByteArrayInputStream(xmlStringBuilder.toString().getBytes("UTF-8"));
			return builder.parse(input);
		} catch (ParserConfigurationException | SAXException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void destroy() {
		try {
			con.close();
		} catch (SQLException e) {
			System.exit(0); // Aktion(en) bei Klicken auf den "Ja-Button"
		} catch (Exception e) {
			System.exit(0);
		}
	}

	@Override
	public void run() {
		this.getData();
	}

	private void log(String text) {
		schnittstelle.showTextLine(text + "\n");
		//System.out.println(text);
	}

	private void log(ChangeSet[] cs) {
		for (ChangeSet csE : cs) {
			log(csE.toString("\t"));
		}
	}

	public void clearData() {
		changes.clearData();
	}

}
