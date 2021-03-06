package org.opendedup.sdfs.mgmt;

import java.io.IOException;
import java.util.Set;

import org.opendedup.logging.SDFSLogger;
import org.opendedup.sdfs.Main;
import org.opendedup.sdfs.cluster.cmds.ListVolsCmd;
import org.opendedup.sdfs.io.Volume;
import org.opendedup.sdfs.servers.HCServiceProxy;
import org.opendedup.util.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class GetRemoteVolumes {

	public Element getResult(String cmd, String file) throws IOException {
		try {
			if (Main.chunkStoreLocal)
				throw new IOException("Chunk Store is local");
			Document doc = XMLUtils.getXMLDoc("remote-volumes");
			Element root = doc.getDocumentElement();
			ListVolsCmd rcmd = new ListVolsCmd();
			rcmd.executeCmd(HCServiceProxy.cs);
			Set<String> volstrs = rcmd.getResults().keySet();
			for (String volstr : volstrs) {
				Volume vol = rcmd.getResults().get(volstr);
				if (vol == null) {
					Element ve = doc.createElement("volume");
					ve.setAttribute("name", volstr);
					ve.setAttribute("down", "true");
					root.appendChild(ve);
				} else {
					Element ve = vol.toXMLElement(doc);
					ve.setAttribute("host", vol.host.toString());
					ve.setAttribute("down", "false");
					root.appendChild(ve);
				}
			}
			return (Element) root.cloneNode(true);
		} catch (Exception e) {
			SDFSLogger.getLog().error(
					"unable to fulfill request on file " + file, e);
			throw new IOException("request to fetch attributes failed because "
					+ e.toString());
		}
	}

}
